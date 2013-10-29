package anders.akita.plan;


import anders.akita.parser.*;


public class SubQB {

	String name;
	
	String[] srcPys;
	String[] src;
	
	JoinType joinType;
	
	RootExp[] wherePreds;
	
	String[] groupby;	
	RootExp[] havingPreds;
	
	RootExp[] selList;
	//String[] alias;
	
}
