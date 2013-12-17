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
		
		// !!! check & gen join-cond
		
		
		//2. check aggr
		
		//3. check & generate select list, generate type !!
		
		//4. where list generate, including inner-q, & type in aggr-call !!
		
		//change middle QB's name, avoid name conflict.
		
		//5. generate aggr, order by, distinct, and so on.
		
	}
	
}
