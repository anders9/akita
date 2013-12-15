package anders.akita.plan;

import anders.akita.parser.*;
import java.util.*;

public class QBParser {

	String qid;
	
	ZQuery rootQuery;
	
	public QBParser(String qid, ZQuery q){
		
		this.qid = qid;
		this.rootQuery = q;
	}
	
	public QB parse(){
		return parseIter(rootQuery);
	}
	
	private QB parseIter(ZQuery q){
		QB qb = new QB();
		
		//1. generate from list, including sub-q in from list
		ZFromClause fc = q.getFrom();
		qb.joinType = fc.getJoinType();
		
		qb.src = new String[fc.items.size()];
		qb.srcPhy = new String[fc.items.size()];
		qb.prevQBs = new HashMap<String, QB>();
		
		for(int i = 0; i < fc.getItemN(); ++i){
			ZFromItemEx item = fc.getItem(i);
			if(item.isSubQuery()){
				qb.src[i] = qb.srcPhy[i] = item.alias;
				QB subQB = parseIter(item.getSubQuery());
				qb.prevQBs.put(item.alias, subQB);
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
		
		//5. generate aggr, order by, distinct, and so on.
		
	}
}
