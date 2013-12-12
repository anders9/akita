package anders.akita.plan;

import java.lang.reflect.Array;
import java.util.*;

import anders.akita.meta.*;
import anders.akita.parser.*;
import anders.util.*;

public class Planner {
	
	QB rootqb;
	int qid;
	
	public Planner(QB qb, int qid){
		this.rootqb = qb;
		this.qid = qid;
	}
	
	public QBPlan genQBPlan(){
		return genQBPlanIter(rootqb);
	}
	
	private QBPlan genQBPlanIter(QB qb){
		
		QBPlan plan = new QBPlan();
		
		plan.prevQBPlans = new ArrayList<QBPlan>();
		
		for(QB pqb: qb.prevQBs.values()){
			
			QBPlan qbp = genQBPlanIter(pqb);
			
			plan.prevQBPlans.add(qbp);
		}

		plan.operators = genSubQBPlan(qb);

		return plan;
	}
	
	static class OpBase{
		ArrayList<String> fetchCol;
		QBClause qbClause;
		int endPos;
		int rsqPos = -1;
		int rsqEndPos = -1;
		boolean genID = false;//for next step
		boolean containID = false;//whether this step's output tab contain ID
	}
	//src, srcPhy, entry, 
	static class OpFetchData extends OpBase{
		String[] src;
		String[] srcPhy;
		JoinType joinType = JoinType.INNER;
		ArrayList<RootExp> joinCond;		
		String[] entry;
		int distrIdx;
		ArrayList<RootExp> where;
	}
	static class OpJoin extends OpBase{
		String[] src;
		String[] srcPhy;
		JoinType joinType;
		ArrayList<RootExp> joinCond;
		JoinPolicy joinPolicy;
		int joinReducerN;
		String[] rhsEntry;
		String[] reducerEntry;
		//String collectEntry;
		ArrayList<RootExp> where;
	}
	static class OpRelAggr extends OpBase{
		int aggrReducerN;
		String[] reducerEntry;
		//RootExp[] aggrExprs;
		//String[] groupby;//if NULL, is for relative-sub-query
		ArrayList<RootExp> havingPreds;
	}
	/*
	static class OpAggr{
		String[] groupby;
		RootExp[] aggrExprs;
		int aggrReducerN;
		ArrayList<RootExp> havingPreds;		
	}
	*/
	
	 ArrayList<String> getPredCoverSrc(RootExp pred)
	 {
		final ArrayList<String> cov = new ArrayList<String>();
		try{
			pred.traverse(new NodeVisitor(){
				public void visit(ZExp node, RootExp root)
						throws ExecException{
					if(node instanceof ZColRef){
						ZColRef cr = (ZColRef)node;
						if(!cov.contains(cr.table)){
							cov.add(cr.table);
						}
					}
				}
			});
		}catch(ExecException e){}
		return cov;
	}
	boolean checkCanPushDown(String[] srcs, ArrayList<String> covers){
		for(String c: covers){
			int l = Util.findStr(c, srcs);
			if(l == -1)
				return false;
		}
		return true;
	}
	 
	 
	int getPushdownLevel(String[] srcs, ArrayList<String> covers){
		int level = 0;
		for(String c: covers){
			int l = Util.findStr(c, srcs);
			if(l != -1 && l > level)
				level = l;
		}
		return level;
	}
	ArrayList<RootExp>[] genPushDownPredsArray(String[] src, ArrayList<RootExp> preds){
		ArrayList<RootExp>[] pdList = new ArrayList[src.length];
		
		for(int i = 0; i < src.length; ++i){
			pdList[i] = new ArrayList<RootExp>();
		}
		for(RootExp pred: preds){
			int lv = getPushdownLevel(src, getPredCoverSrc(pred));
			pdList[lv].add(pred);
		}
		return pdList;
	}
	ArrayList<RootExp> mergePredArray(ArrayList<RootExp>[] preds, int beg, int end){
		ArrayList<RootExp> list = new ArrayList<RootExp>();
		for(int i = beg; i < end; ++i){
			list.addAll(preds[i]);
		}
		return list;
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
	ArrayList<String> getCoverCol(RootExp exp){
		final ArrayList<String> list = new ArrayList<String>();
		try{
			exp.traverse(new NodeVisitor(){
				public void visit(ZExp node, RootExp root) throws ExecException {
					if(node instanceof ZColRef){
						ZColRef cr = (ZColRef)node;
						String col = cr.toString();
						if(!list.contains(col))
							list.add(col);
					}
				}
			});
		}catch(Exception e){}
		return list;
	}
	ArrayList<String> getCoverCol(ArrayList<RootExp> expList){
		ArrayList<String> list = new ArrayList<String>();
		for(RootExp e: expList){
			list.addAll(getCoverCol(e));
		}
		return list;
	}
	ArrayList<String> getCoverCol(RootExp[] expList){
		ArrayList<String> list = new ArrayList<String>();
		for(RootExp e: expList){
			list.addAll(getCoverCol(e));
		}
		return list;
	}
	ArrayList<String> filteCoverCol(String[] cutSrc, ArrayList<String> list){
		ArrayList<String> rlist = new ArrayList<String>();
		for(String col: list){
			String s = getColSrc(col);
			if(Util.findStr(s, cutSrc) == -1)
				rlist.add(col);
		}
		return rlist;
	}
	
	String genTmpTableName(String qbName, int step){
		return String.format("$$ts%d_%s_%d", qid, qbName, step); 
	}
	
	FetchDataOperator[] genSubQBPlan(QB qb){
		
		ArrayList<OpBase> opList = new ArrayList<OpBase>();
		
		//push predicates down
		ArrayList<RootExp>[] pdList = genPushDownPredsArray(qb.src, qb.where);
		//special process for RIGHT-JOIN
		if(qb.src.length == 2 && qb.joinType == JoinType.RIGHT){
			pdList[1].addAll(pdList[0]);
			pdList[0].clear();
		}
		//process Join condition
		if(qb.src.length == 2 && qb.joinType != JoinType.INNER && qb.joinCond != null){
			if(qb.joinType == JoinType.RIGHT){
				Iterator<RootExp> iter = qb.joinCond.iterator();
				while(iter.hasNext()){
					RootExp jc = iter.next();
					int lv = getPushdownLevel(qb.src, getPredCoverSrc(jc));
					if(lv == 0){
						pdList[0].add(jc);
						iter.remove();
					}
				}
			}
		}
		ArrayList<RootExp>[][] spdList = new ArrayList[qb.relSubQ.length][];
		for(int i = 0; i < spdList.length; ++i){
			RelSubQuery rsq = qb.relSubQ[i];
			spdList[i] = genPushDownPredsArray(rsq.src, rsq.wherePreds);
		}
		//process LEFT/RIGHT JOIN condition 
		
		
		int ri = 0;
		int end = qb.src.length;
		int relPos = 0;
		if(qb.relSubQ.length > 0)
			end = qb.relSubQ[0].pos;
		
		int pos = 0;
		int distr = 0;
		int distrIdx = -1;
		while(pos < end){
			if(/*Meta.getTab(qb.srcPhy[pos]).isDistributed()*/
					Planner.checkSrcDistribute(qb, qb.src[pos]))
				++distr;
			if(distr == 1)
				distrIdx = pos;
			if(distr == 2)
				break;
			++pos;
		}
		OpFetchData ofp = new OpFetchData();
		ofp.src = new String[pos];
		ofp.srcPhy = new String[pos];
		for(int i = 0; i < pos; ++i){
			ofp.src[i] = qb.src[i];
			ofp.srcPhy[i] = qb.srcPhy[i];
		}
		ofp.distrIdx = distrIdx;
		ofp.entry = distrIdx == -1 ?
				Meta.randomEntries(1) : 
				/*Meta.getTab(ofp.srcPhy[distrIdx]).getEntries()*/
				Planner.getSrcEntries(qb, qb.src[distrIdx]);
		ofp.endPos = pos;
		ofp.joinType = qb.joinType;
		ofp.joinCond = qb.joinCond;
		ofp.where = mergePredArray(pdList, 0, pos);
		opList.add(ofp);
		
		while(true){
			
			while(pos < end){
					
				int beg = pos;
				++pos;
				while (pos < end) {
					if (/*Meta.getTab(qb.srcPhy[pos]).isDistributed()*/
							Planner.checkSrcDistribute(qb, qb.src[pos]))
						break;
				}

				OpJoin oj = new OpJoin();
				oj.src = new String[pos - beg];
				oj.srcPhy = new String[pos - beg];
				
				for(int i = beg; i < pos; ++i){
					oj.src[i - beg] = qb.src[i];
					oj.srcPhy[i - beg] = qb.srcPhy[i];
				}
				oj.joinType = qb.joinType;
				oj.joinCond = qb.joinCond;
				oj.joinPolicy = qb.joinPolicy[beg];
				oj.joinReducerN = qb.joinReducerN[beg];
				oj.rhsEntry = /*Meta.getTab(oj.srcPhy[0]).getEntries()*/Planner.getSrcEntries(qb, qb.src[0]);
				if(oj.joinPolicy == JoinPolicy.Reduceside)
					oj.reducerEntry = Meta.randomEntries(oj.joinReducerN);
				
				oj.endPos = pos;
				oj.where = mergePredArray(pdList, beg, pos);
				opList.add(oj);
			}
			
			boolean first = true;
			while(relPos < qb.relSubQ.length){
				
				if(!first && qb.relSubQ[relPos].pos != qb.relSubQ[relPos - 1].pos)
					break;
				
				RelSubQuery rsq = qb.relSubQ[relPos];
				opList.get(opList.size() - 1).genID = true;
				int k = 0;
				while(k < rsq.srcPhy.length && /* !Meta.getTab(rsq.srcPhy[k]).isDistributed()*/
						!Planner.checkSrcDistribute(qb, qb.src[k])
						)
					++k;
				
				if(k > 0){
					//add a local-join operator
					OpJoin oj = new OpJoin();
					for(int i = 0; i < k; ++i){
						oj.src[i] = rsq.src[i];
						oj.srcPhy[i] = rsq.srcPhy[i];
						oj.joinType = JoinType.INNER;
						oj.joinPolicy = JoinPolicy.Local;
					}
					oj.endPos = pos;
					oj.rsqPos = relPos;
					oj.rsqEndPos = k;
					oj.where = mergePredArray(spdList[relPos], 0, k);
					oj.containID = true;
					opList.add(oj);
				}
				while(k < rsq.srcPhy.length){
					int beg = k;
					++k;
					while (k < rsq.srcPhy.length) {
						if (/*Meta.getTab(rsq.srcPhy[k]).isDistributed()*/
								Planner.checkSrcDistribute(qb, qb.src[k]))
							break;
						++k;
					}
					OpJoin oj = new OpJoin();
					oj.src = new String[k - beg];
					oj.srcPhy = new String[k - beg];
					
					for(int i = beg; i < k; ++i){
						oj.src[i - beg] = rsq.src[i];
						oj.srcPhy[i - beg] = rsq.srcPhy[i];
					}
					oj.joinType = JoinType.INNER;
					oj.joinPolicy = rsq.joinPolicy[beg];
					oj.joinReducerN = rsq.joinReducerN[beg];
					oj.rhsEntry = /*Meta.getTab(oj.srcPhy[0]).getEntries()*/Planner.getSrcEntries(qb, oj.src[0]);
					if(oj.joinPolicy == JoinPolicy.Reduceside)
						oj.reducerEntry = Meta.randomEntries(oj.joinReducerN);
					
					oj.endPos = pos;
					oj.rsqPos = relPos;
					oj.rsqEndPos = k;
					oj.where = mergePredArray(spdList[relPos], beg, k);
					oj.containID = true;
					opList.add(oj);
				}
				//add aggregation node for Relative-Sub-query
				OpRelAggr oa = new OpRelAggr();
				oa.aggrReducerN = rsq.aggrReducerN;
				oa.reducerEntry = Meta.randomEntries(rsq.aggrReducerN);
				oa.havingPreds = new ArrayList<RootExp>();
				oa.havingPreds.add(rsq.havingPreds);
				oa.containID = false;//By default, AGGR-node output table not contain ID, but if next step need ID (chain Inner-query),
									// then keep ID, this place will be treated specially.
				
				oa.endPos = pos;
				oa.rsqPos = relPos;
				oa.rsqEndPos = k;
				
				opList.add(oa);
				
				if(first)
					first = false;
				++relPos;
			}
			
			if(end == qb.src.length)
				break;
			
			if(relPos < qb.relSubQ.length)
				end = qb.relSubQ[relPos].pos;
			else
				end = qb.src.length;
		}
		
		/*
		 * FetchData, Join, ... Join(genID),| Join(containID), ... Aggr( [genID]),| ...
		 */
		
		ArrayList<String> fcols;
		//get col set...
		fcols = getCoverCol(qb.selList);
		if(qb.groupby != null){
			ArrayList<String> t = new ArrayList<String>();
			for(ZColRef gb : qb.groupby)
				t.add(t.toString());
			fcols = Util.<String>mergeArrayList(fcols, t);
			if(qb.havingPreds != null){
				t = getCoverCol(qb.havingPreds);
				fcols = Util.<String>mergeArrayList(fcols, t);
			}
		}
		
		//pruning column...
		for(int k = opList.size() - 1; k >= 0; --k){
			OpBase ob = opList.get(k);
			ob.fetchCol = (ArrayList<String>)fcols.clone();
			if(ob instanceof OpRelAggr){
				OpRelAggr oa = (OpRelAggr)ob;
				fcols = Util.<String>mergeArrayList(fcols, getCoverCol(oa.havingPreds));
			}
			else if(ob instanceof OpJoin){
				OpJoin oj = (OpJoin)ob;
				fcols = Util.<String>mergeArrayList(fcols, getCoverCol(oj.where));
				if(oj.joinCond != null)
					fcols = Util.<String>mergeArrayList(fcols, getCoverCol(oj.joinCond));
				fcols = filteCoverCol(oj.src, fcols);
			}
			else if(ob instanceof OpFetchData){
				//Do nothing
			}
		}
		
		ArrayList<FetchDataOperator> ops = new ArrayList<FetchDataOperator>();		
		
		for(int i = 0; i < opList.size(); ++i){
			OpBase ob = opList.get(i);
			
			FetchDataOperator fdo = null;
			
			if(ob instanceof OpFetchData){
				OpFetchData ofd = (OpFetchData)ob;
				fdo = new FetchDataOperator();
				fdo.schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i), ob.fetchCol, false);
				
				ob.qbClause = Planner.genSelectClause(ofd.src, ofd.srcPhy,
						ofd.joinType,
						ofd.joinCond,
						ofd.where, ofd.fetchCol, null, false);
				fdo.fetchSQL = ob.qbClause.toString();
				fdo.entries = ofd.entry;
				
				fdo.tmpTabList = new ArrayList<String>();//No temp tab generated this step
				
				ops.add(fdo);
			}
			else if(ob instanceof OpJoin){
				OpJoin oj = (OpJoin)ob;
				if(oj.joinPolicy == JoinPolicy.Local){
					fdo = new LocalJoinOperator();
					LocalJoinOperator ljo = (LocalJoinOperator)fdo;
					fdo.schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i), ob.fetchCol, oj.containID);
					
					ljo.leftSrc = ops.get(i - 1);
					assert oj.containID == true;
					
					ljo.genPrevID = !ljo.leftSrc.schema.containID;
					fdo.entries = ljo.leftSrc.entries;
					ob.qbClause = Planner.genSelectClause(oj.src, oj.srcPhy, 
							oj.joinType, oj.joinCond, oj.where, oj.fetchCol, new String[]{ljo.leftSrc.schema.name}, true);
					fdo.fetchSQL = ob.qbClause.toString();
					fdo.tmpTabList = new ArrayList<String>();
					fdo.tmpTabList.add(ljo.leftSrc.schema.name);
				}
				else if(oj.joinPolicy == JoinPolicy.Mapside){
					fdo = new MapJoinOperator();
					fdo.schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i), ob.fetchCol, oj.containID);
					MapJoinOperator mjo = (MapJoinOperator)fdo;
					
					mjo.leftSrc = ops.get(i - 1);
					fdo.entries = oj.rhsEntry;
					
					mjo.collectNode = Meta.randomEntries(1)[0];
					mjo.genPrevID = oj.containID && !mjo.leftSrc.schema.containID;
					
					ob.qbClause = Planner.genSelectClause(oj.src, oj.srcPhy, 
							oj.joinType,
							oj.joinCond,
							oj.where, oj.fetchCol, new String[]{mjo.leftSrc.schema.name},
							oj.containID
							);
					fdo.fetchSQL = ob.qbClause.toString();
					fdo.tmpTabList = new ArrayList<String>();
					fdo.tmpTabList.add(mjo.leftSrc.schema.name);
				}
				else if(oj.joinPolicy == JoinPolicy.Reduceside){
					fdo = new ReduceJoinOperator();
					fdo.schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i), ob.fetchCol, oj.containID);
					ReduceJoinOperator rjo = (ReduceJoinOperator)fdo;
					
					rjo.srcs = new FetchDataOperator[2];
					rjo.srcs[0] = ops.get(i - 1);
					
					ArrayList<RootExp> rhsWhere = genJoinRhsPred(oj.joinType, oj.joinCond, oj.src, oj.where);
					
					rjo.srcs[1] = new FetchDataOperator();
					//generate rhs-table column
					ArrayList<String> rhsCols = (ArrayList<String>)oj.fetchCol.clone();
					rhsCols = Util.<String>mergeArrayList(rhsCols, getCoverCol(oj.where));
					if(oj.joinCond != null)
						rhsCols = Util.<String>mergeArrayList(rhsCols, getCoverCol(oj.joinCond));
					rhsCols = filteCoverCol(oj.src, rhsCols);
					rjo.srcs[1].schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i) + "_rhs", rhsCols, false);
					rjo.srcs[1].entries = oj.rhsEntry;
					rjo.srcs[1].fetchSQL = Planner.genSelectClause(oj.src, oj.srcPhy,
							JoinType.INNER, null, rhsWhere, rhsCols, null, false)
							.toString();
					
					fdo.tmpTabList = new ArrayList<String>();
					fdo.tmpTabList.add(rjo.srcs[0].schema.name);
					fdo.tmpTabList.add(rjo.srcs[1].schema.name);
					
					//rjo.joinKeyIdx
					String[][] jk = Planner.genJoinKeyIdx(oj.joinType, oj.joinCond, oj.where);
					
					int[][] jkIdx = new int[jk.length][2];
					
					for(int k = 0; k < jk.length; ++k){
						jkIdx[k][0] = Util.findStr(jk[k][0], rjo.srcs[0].schema.col);
						jkIdx[k][1] = Util.findStr(jk[k][1], rjo.srcs[1].schema.col);
					}
					rjo.joinKeyIdx = jkIdx;
					
					fdo.entries = oj.reducerEntry;
					String[] jsrc = new String[]{rjo.srcs[0].schema.name, rjo.srcs[1].schema.name};
					ob.qbClause = Planner.genSelectClause(new String[0], new String[0], oj.joinType, oj.joinCond, oj.where, oj.fetchCol, jsrc,
							oj.containID
							);
					fdo.fetchSQL = ob.qbClause.toString();
					rjo.genPrevID = oj.containID && !rjo.srcs[0].schema.containID;
				}
				else
					assert false;
				ops.add(fdo);
			}
			else if(ob instanceof OpRelAggr){
				boolean containID = ob.genID;//if $genID for next step, no need to re-gen, only keep ID column from src-tab
				OpRelAggr ora = new OpRelAggr();
				
				FetchDataOperator prevOp = ops.get(i - 1);
				OpBase prevOb = opList.get(i - 1);
				
				if(prevOp.entries.length == 1){
					//push current node into prev-node (fetch data)
					ob.qbClause = Planner.genAggrClause(prevOb.qbClause, ora.fetchCol, null, 
							new String[]{"id"}, ora.havingPreds, containID);
					prevOb.qbClause = ob.qbClause;
					
					//replace previous operator's fetch data SQL, not gen new operator
					prevOp.fetchSQL = ob.qbClause.toString();
					
					prevOp.schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i), ob.fetchCol, containID);
				}
				else{
					QBClause[] qbClauses = new QBClause[2];
					Schema[] schemas = new Schema[2];
					Planner.gen2PhaseAggrClause(prevOb.qbClause, ora.fetchCol, null, 
							new String[]{"id"}, ora.havingPreds, containID,
							qbClauses, schemas);
					//modify previous node
					prevOb.qbClause = qbClauses[0];
					prevOp.fetchSQL = qbClauses[0].toString();
					prevOp.schema = schemas[0];
					
					fdo = new AggrOperator();
					AggrOperator ao = (AggrOperator)fdo;
					//fdo.schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i), ob.fetchCol, containID);
					ao.src = prevOp;
					fdo.entries = ora.reducerEntry;
					
					ora.qbClause = qbClauses[1];
					fdo.fetchSQL = qbClauses[1].toString();
					fdo.schema = schemas[1];
					fdo.tmpTabList = new ArrayList<String>();
					fdo.tmpTabList.add(ao.src.schema.name);
					
					ops.add(fdo);
				}
			}
		}
		
		//append Aggr clause if exist, if last operator is AggrOp, then generate two-level aggr clause

	}
	Schema genTabSchema(QB qb, String name, ArrayList<String> cols, boolean withID){
		Schema s = new Schema();
		s.name = name;
		s.col = ((ArrayList<String>)cols.clone()).toArray(new String[0]);
		for(String c : s.col)
			c.replace('.', '$');
		s.type = new String[s.col.length];
		for(int k = 0; k < s.col.length; ++k){
			s.type[k] = getColumnType(qb, s.col[k]);
		}
		s.containID = withID;
		return s;
	}
	ArrayList<RootExp> genJoinRhsPred(JoinType joinType, ArrayList<RootExp> joinCond, String[] rhsSrcs, ArrayList<RootExp> where){
		
	}
	
	static class QBClause{
		String fields;
		String fromClause;
		String[] rawSrcs;//when Column ref tab not in this array, then translate into tab$column 
		String whereClause;
		
		int aggrLevel;// = 0, 1, 2
		
		String groupbyClause;
		String havingClause;
		
		String groupbyClause2;
		String havingClause2;
		
		boolean distinct = false;
		String orderbyClause;
		
		public String toString(){
			String dist = distinct ? " distinct" : "";
			String t = String.format("select%s %s from %s where %s", dist, fields, fromClause, whereClause);
			if(groupbyClause != null)
				t += (" group by " + groupbyClause);
			if(havingClause != null)
				t += (" having " + havingClause);
			if(orderbyClause != null)
				t += (" order by " + orderbyClause);
			//!!
			//add second aggr code.
			
			//return t;
		}
	}
	
	static QBClause genSelectClause(String[] src, String[] srcPhy, 
			JoinType joinType, 
			ArrayList<RootExp> joinCond,
			ArrayList<RootExp> where, 
			ArrayList<String> cols,
			String[] midSrc,
			boolean withID
			){
		
	}
	
	static QBClause genAggrClause(QBClause prevClause, 
			ArrayList<String> selList, ArrayList<RootExp> selListExp,  String[] groupby, ArrayList<RootExp> having,
			boolean withID){
		
	}
	
	
	static boolean gen2PhaseAggrClause(QBClause prevClause, 
			ArrayList<String> selList, ArrayList<RootExp> selListExp,  String[] groupby, ArrayList<RootExp> having,
			boolean withID,
			QBClause[] qbClauses,
			Schema[] schemas
			){
		
	}
	
	static String[][] genJoinKeyIdx(JoinType joinType, 
			ArrayList<RootExp> joinCond,
			ArrayList<RootExp> where){
		
	}
	
	static String[] getSrcEntries(QB qb, String src){
		
	}
	
	static boolean checkSrcDistribute(QB qb, String src){
		
	}
	
	static String src2PhySrc(QB qb, String src){
		int idx = Util.findStr(src, qb.src);
		if(idx != -1)
			return qb.srcPhy[idx];
		return null;
	}
	
	static String getColumnType(QB qb, String col){
		String src = Planner.getColSrc(col);
		String colName = Planner.getColName(col);
		
		String tab = Planner.src2PhySrc(qb, src);
		
		
		//!!!
		//process temp table#
		QB pqb = qb.prevQBs.get(tab);
		if(pqb != null){
			int idx = Util.findStr(colName, pqb.schema.col);
			return pqb.schema.type[idx];
		}
		return Meta.getTab(tab).getCol(colName).getType();
	}
}
