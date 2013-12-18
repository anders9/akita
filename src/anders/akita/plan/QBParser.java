package anders.akita.plan;

import anders.akita.parser.*;

import java.util.*;

public class QBParser {

	long qid;
	
	ZQuery rootQuery;
	
	public QBParser(long qid, ZQuery q){
		
		this.qid = qid;
		this.rootQuery = q;
	}
	
	public QB parse(){
		return parseIter(rootQuery, "$", null);
	}
	
	private QB parseIter(ZQuery q, String name, QB parent){
		QB qb = new QB();
		
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
				for(String key: q.tabList.keySet()){
					ZFromItemEx tab = q.tabList.get(key);
					for(String field: tab.getFieldList()){
						ZColRef cr = new ZColRef(key, field);
						newlist.add(new ZSelectItem(cr, null));
					}
				}
			}
			else if(item.type == ZSelectItem.TAB_DOT_STAR){
				if(q.tabList.get(item.table) == null)
					throw new ExecException("Illegal select item: " + item.table + ".*");
				for(String field: q.tabList.get(item.table).getFieldList()){
					ZColRef cr = new ZColRef(item.table, field);
					newlist.add(new ZSelectItem(cr, null));					
				}
			}
			else newlist.add(item);
		}
		q.setSelect(newlist);

		
		
		// !!! check & gen join-cond		
		
		//2. check aggr
		
		//3. check & generate select list, generate type !!
		
		//4. where list generate, including inner-q, & type in aggr-call !!
		
		//change middle QB's name, avoid name conflict.
		
		//5. generate aggr, order by, distinct, and so on.
		
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
