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
		
		
		
	}
}
