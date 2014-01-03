package anders.akita.plan;

import anders.akita.meta.Meta;
import anders.akita.parser.*;
import anders.util.Util;

import java.util.*;

public class QBParser {

	long qid;
	
	ZQuery rootQuery;
	
	public QBParser(long qid, ZQuery q){
		
		this.qid = qid;
		this.rootQuery = q;
	}
	
	public QB parse()
		throws ExecException
	{
		return parseIter(rootQuery, "$", null);
	}
	
	private QB parseIter(final ZQuery q, String name, QB parent)
		throws ExecException
	{
		final QB qb = new QB();
		
		qb.name = name;
		qb.parent = parent;
		
		//1. generate from list, including sub-q in from list
		ZFromClause fc = q.getFrom();
		qb.joinType = fc.getJoinType();
		
		qb.src = new String[fc.items.size()];
		qb.srcPhy = new String[fc.items.size()];
		qb.prevQBs = new HashMap<String, QB>();
		
		HashMap<String, String> subQBNameMap = new HashMap<String, String>();
		
		for(int i = 0; i < fc.getItemN(); ++i){
			ZFromItemEx item = fc.getItem(i);
			if(item.isSubQuery()){
				qb.src[i] = qb.srcPhy[i] = item.alias;
				QB subQB = parseIter(item.getSubQuery(), "$" + item.alias, qb);
				
				//!!! generate unique name for derived table !!!
				String uniqName = qb.genNamePrefix(qid) + "_dr_" + item.alias;
				subQB.schema.name = uniqName;
				subQBNameMap.put(item.alias, uniqName);
				
				qb.prevQBs.put(uniqName, subQB);
			}
			else{
				qb.src[i] = item.alias;
				qb.srcPhy[i] = item.getFromItem().table;
			}
		}
		
		Vector<ZSelectItem> newlist = new Vector<ZSelectItem>();
		for(ZSelectItem item: q.getSelect()){
			if(item.type == ZSelectItem.STAR){
				for(String src: qb.src){
					String[] fields = QBParser.getColumns(qb, src, null, null);
					for(String field: fields){
						ZColRef cr = new ZColRef(src, field);
						newlist.add(new ZSelectItem(cr, null));
					}
				}
			}
			else if(item.type == ZSelectItem.TAB_DOT_STAR){
				if(Util.findStr(item.table, qb.src) == -1)
					throw new ExecException("Illegal select item: " + item.table + ".*");
				String src = item.table;
				String[] fields = QBParser.getColumns(qb, src, null, null);
				for(String field: fields){
					ZColRef cr = new ZColRef(src, field);
					newlist.add(new ZSelectItem(cr, null));
				}
			}
			else newlist.add(item);
		}
		q.setSelect(newlist);
		
		qb.selList = new RootExp[q.getSelect().size()];
		for(int i = 0; i < q.getSelect().size(); ++i){
			qb.selList[i] = new RootExp(q.getSelect().get(i).expr);
		}
		
		qb.needAggr = q.getGroupBy().getGroupBy() != null;
		for(RootExp re: qb.selList){
			re.traverse(new NodeVisitor(){
				public void visit(ZExp node, RootExp root) throws ExecException {
					if(node instanceof ZColRef){
						QBParser.checkValidCol(qb, (ZColRef)node, null, null);
					}
					else if(node instanceof ZExpression){
						ZExpression exp = (ZExpression)node;
						if(checkValidAggr(qb, (ZExpression)exp, null, null))
							qb.needAggr = true;
					}
					else if(node instanceof ZQuery){
						throw new ExecException("Not allow sub-query in select list: " + node.toString());
					}
				}				
			});
		}
		
		
		qb.schema = new Schema();
		qb.schema.name = null; //allocate by upper level
		
		qb.schema.col = new String[q.getSelect().size()];
		qb.schema.type = new String[q.getSelect().size()];
		
		for(int i = 0; i < q.getSelect().size(); ++i){
			ZSelectItem si = q.getSelect().get(i);
			if(si.alias != null)
				qb.schema.col[i] = si.alias;
			else{
				if(si.expr instanceof ZColRef){
					qb.schema.col[i] = ((ZColRef) si.expr).col;
				}
				else throw new ExecException("No alias on col: " + si.expr.toString());
			}
			if(genTypeInfo(qb, si.expr, null, null)){
				qb.schema.type[i] = si.expr.valType;
			}
			else throw new ExecException("No type information on field: " + si.expr.toString());
		}
		
		HashSet<String> testFields = new HashSet<String>();
		for(String alias: qb.schema.col){
			if(testFields.contains(alias))
				throw new ExecException("select fields name conflict: " + alias);
			testFields.add(alias);
		}
		
		// !!! check & gen join-cond		
		if(q.getFrom().join_cond != null){
			qb.joinCond = genRootExpList(q.getFrom().join_cond);
			for(RootExp re: qb.joinCond){
				re.traverse(new NodeVisitor(){
					public void visit(ZExp node, RootExp root) throws ExecException {
						if(node instanceof ZColRef){
							QBParser.checkValidCol(qb, (ZColRef)node, null, null);
						}
						else if(node instanceof ZExpression){
							if(((ZExpression)node).isAggr())
								throw new ExecException("Not allow aggregation in join-condition: " + node.toString());
						}
						else if(node instanceof ZQuery){
							throw new ExecException("Not allow sub-query in join-condition: " + node.toString());
						}
					}
				});
			}
		}
		
		qb.joinType = q.getFrom().getJoinType();
		qb.joinPolicy = new JoinPolicy[q.getFrom().getItemN()];
		qb.joinReducerN = new int[q.getFrom().getItemN()];
		for(int i = 0; i < qb.joinPolicy.length; ++i){
			qb.joinPolicy[i] = q.getFrom().getItem(i).joinPolicy;
			qb.joinReducerN[i] = q.getFrom().getItem(i).joinReducerN;
		}
		
		if(q.getWhere() != null){
			qb.where = genRootExpList(q.getWhere());
			for(RootExp re: qb.where){
				re.traverse(new NodeVisitor(){
					public void visit(ZExp node, RootExp root) throws ExecException {
						if(node instanceof ZColRef){
							QBParser.checkValidCol(qb, (ZColRef)node, null, null);
						}
						else if(node instanceof ZExpression){
							if(((ZExpression)node).isAggr())
								throw new ExecException("Not allow aggregation in where-clause: " + node.toString());
						}
						else if(node instanceof ZQuery){
							//currently.
							throw new ExecException("Not allow sub-query in where-clause: " + node.toString());
						}
					}
				});
			}
		}
		

		if(q.getGroupBy().getHaving() != null){
			qb.havingPreds = genRootExpList(q.getGroupBy().getHaving());
			for(RootExp re: qb.havingPreds){
				re.traverse(new NodeVisitor(){
					public void visit(ZExp node, RootExp root) throws ExecException {
						if(node instanceof ZColRef){
							QBParser.checkValidCol(qb, (ZColRef)node, null, null);
						}
						else if(node instanceof ZExpression){
							if(((ZExpression)node).isAggr())
								throw new ExecException("Not allow aggregation in having-clause: " + node.toString());
						}
						else if(node instanceof ZQuery){
							//currently.
							throw new ExecException("Not allow sub-query in where-clause: " + node.toString());
						}
					}
				});
			}
		}
		
		if(qb.needAggr){
			ZGroupBy gb = q.getGroupBy();
			if(gb.getGroupBy() == null)
				qb.groupby = new ZColRef[0];
			else{
				qb.groupby = new ZColRef[gb.getGroupBy().size()];
				for(int i = 0; i < qb.groupby.length; ++i){
					ZColRef cr = (ZColRef)gb.getGroupBy().get(i);
					checkValidCol(qb, cr, null, null);
					qb.groupby[i] = cr;
				}
			}
			qb.aggrReducerN = q.getGroupBy().aggrReducerN;
		}
		
		qb.distinct = q.isDistinct();
		if(q.getOrderBy() != null){
			qb.orderby = new String[q.getOrderBy().size()];
			qb.orderbyAsc = new boolean[q.getOrderBy().size()];
			
			for(int i = 0; i < qb.orderby.length; ++i){
				qb.orderby[i] = q.getOrderBy().get(i).getCol();
				qb.orderbyAsc[i] = q.getOrderBy().get(i).getAscOrder();
			}
			qb.topK = q.topK;
		}
		
		qb.shuffleCnt = q.shuffleN;
		if(qb.orderby != null){
			qb.shuffleCnt = 1;
		}
		//!!!!!! add mapjoin, reducejoin, aggrReducer, midtabstoragetype...
		
		//2. check aggr
		
		//3. check & generate select list, generate type !!
		
		//4. where list generate, including inner-q, & type in aggr-call !!
		
		//change middle QB's name, avoid name conflict.
		
		//5. generate aggr, order by, distinct, and so on.
		
		return qb;
	}
	
	static boolean genTypeInfo(final QB qb, ZExp node, String[] extSrc, String[] extSrcPhy){
		if(node.valType != null)
			return true;
		else if(node instanceof ZColRef){
			ZColRef cr = (ZColRef)node;
			node.valType = QBParser.getColumnType(qb, cr.table, cr.col, extSrc, extSrcPhy);
			return true;
		}
		return false;
	}
	
	static boolean checkValidAggr(final QB qb, ZExpression exp, String[] extSrc, String[] extSrcPhy)
		throws ExecException
	{
		if(exp.isAggr()){
			ZExp e = exp.parentExp;
			while( ! (e instanceof RootExp) ){
				if(e instanceof ZExpression && ((ZExpression)e).isAggr())
					throw new ExecException("Nested Aggregation: " + e.toString());
				e = e.parentExp;
			}
			if(exp.funcOrAggrName.equalsIgnoreCase("COUNT") && exp.getOperand(0) == null){
				exp.setOperand(new ZConstant("1", ZConstant.NUMBER), 0);
			}
			if(exp.getOperands().size() != 1)
				throw new ExecException("Not support multi-parm aggregation: " + exp.toString());
			//check type...
			/*
			for(ZExp param: exp.getOperands()){
				if(!genTypeInfo(qb, param, extSrc, extSrcPhy)){
					String intype = FunctionMgr.aggrParamType(exp.funcOrAggrName);
					if(intype == null)
						throw new ExecException("Can't get aggregation param type info: " + param.toString() + " in " + exp.toString());
				}
				if(!genTypeInfo(qb, exp, extSrc, extSrcPhy)){
					if(FunctionMgr.aggrTypeUseParam(exp.funcOrAggrName))
						exp.valType = exp.getOperand(0).valType;
					else{
						String rettype = FunctionMgr.aggrRetValType(exp.funcOrAggrName);
						if(rettype != null)exp.valType = rettype;
						else throw new ExecException("Can't get aggregation return type info: " + exp.toString());
					}
				}
			}
			*/
			
			return true;
		}
		else{
			return false;
		}
	}
	
	static void checkValidCol(final QB qb, ZColRef cr, String[] extSrc, String[] extSrcPhy)
		throws ExecException
	{
		if(cr.table != null){
			String[] cols = getColumns(qb, cr.table, extSrc, extSrcPhy);
			if(Util.findStr(cr.col, cols) != -1)
				return;
			else throw new ExecException("Not exist column: " + cr.toString());
		}
		else{
			ArrayList<String> tmpTabs = new ArrayList<String>();
			for(String s: qb.srcPhy)tmpTabs.add(s);
			for(String s: extSrcPhy)tmpTabs.add(s);
			
			String srcPhy = null;
			for(String tab: tmpTabs){
				String[] cols = getColumnsBySrcPhy(qb, tab);
				
				if(Util.findStr(cr.col, cols) != -1){
					if(srcPhy == null)
						srcPhy = tab;
					else
						throw new ExecException("select column ambiguous: "
								+ cr.col + " in tab " + srcPhy + " and " + tab);
				}
			}
			if(srcPhy == null)
				throw new ExecException("Not exist column: " + cr.toString());
			else{
				int idx = Util.findStr(srcPhy, qb.srcPhy);
				if(idx != -1)cr.table = qb.src[idx];
				else{
					idx = Util.findStr(srcPhy, extSrcPhy);
					cr.table = extSrc[idx];
				}
			}
		}
		cr.valType = QBParser.getColumnType(qb, cr.table, cr.col, extSrc, extSrcPhy);
	}
	
	static String genCol(String src, String col){
		return src + '.' + col;
	}
	static String getColSrc(String col){
		return col.substring(0, col.indexOf('.'));
	}
	static String getColName(String col){
		return col.substring(col.indexOf('.') + 1);
	}
	
	static String src2PhySrc(final QB qb, String src, String[] extSrc, String[] extSrcPhy){
		int idx = Util.findStr(src, qb.src);
		if(idx != -1)
			return qb.srcPhy[idx];
		if(extSrc != null){
			idx = Util.findStr(src, extSrc);
			if(idx != -1)
				return extSrcPhy[idx];
		}
		return null;
	}
	
	static String[] getColumnsBySrcPhy(final QB qb, String srcPhy){
		QB pqb = qb.prevQBs.get(srcPhy);
		if(pqb != null){
			return pqb.schema.col.clone();
		}
		return Meta.getTab(srcPhy).getAllColName();
	}
	
	static String[] getColumns(final QB qb, String src, String[] extSrc, String[] extSrcPhy){
		String tab = Planner.src2PhySrc(qb, src, extSrc, extSrcPhy);
		return getColumnsBySrcPhy(qb, tab);
	}
	
	static String getColumnType(final QB qb, String col, String[] extSrc, String[] extSrcPhy){
		return QBParser.getColumnType(qb, getColSrc(col), getColName(col), extSrc, extSrcPhy);
	}
	
	static String getColumnType(final QB qb, String src, String colName, String[] extSrc, String[] extSrcPhy){
		
		String tab = Planner.src2PhySrc(qb, src, extSrc, extSrcPhy);
		//!!!
		//process temp table#
		QB pqb = qb.prevQBs.get(tab);
		if(pqb != null){
			int idx = Util.findStr(colName, pqb.schema.col);
			return pqb.schema.type[idx];
		}
		return Meta.getTab(tab).getCol(colName).getType();
	}
	
	void splitExprByAndIter(ZExp exp, ArrayList<ZExp> list){
		if(
			exp instanceof ZExpression
			&& ((ZExpression)exp).type == ZExpression.OPERATOR
			&& ((ZExpression)exp).getOperator() == Operator.AND
		
		){
			for(ZExp sube: ((ZExpression)exp).getOperands()){
				splitExprByAndIter(sube, list);
			}
		}
		else{
			list.add(exp);
		}
	}
	
	ArrayList<RootExp> genRootExpList(ZExp exp){
		ArrayList<ZExp> list = new ArrayList<ZExp>();
		splitExprByAndIter(exp, list);
		ArrayList<RootExp> relist = new ArrayList<RootExp>();
		for(ZExp e: list){
			relist.add(new RootExp(e));
		}
		return relist;
	}
	
	
}
