package anders.akita.plan;

import anders.akita.parser.*;

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
		
		//2. check aggr
		
		//3. check & generate select list
		
		//4. where list generate, including inner-q
		
		//5. generate aggr, order by, distinct, and so on.
		
	}
}
