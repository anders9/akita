package anders.akita.plan;


import anders.akita.parser.*;


public class SubQB {

	String name;
	
	String[] srcPys;
	String[] src;
	
	JoinType[] joinType;
	JoinPolicy[] joinPolicy;
	int[] joinReducerN;	
	
	RootExp[] wherePreds;
	
	String[] groupby;	
	int aggrReducerN;
	RootExp[] havingPreds;
	
	RootExp[] selList;
	//String[] alias;
	
}
