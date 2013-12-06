package anders.akita.plan;

import java.util.*;

import anders.akita.parser.*;

public class QB {
	
	//ArrayList<QB> prevQBs;
	HashMap<String, QB> prevQBs;//contain nested from clause & non-relative inner-query
	
	//SubQB[] subQBs;
	
	String[] src;
	String[] srcPhy;//if is nested from clause, srcPhy = src
	JoinType joinType;
	ArrayList<RootExp> joinCond;
	JoinPolicy[] joinPolicy;
	int[] joinReducerN;		
	RelSubQuery[] relSubQ;
	
	ArrayList<RootExp> where;
	
	ZColRef[] groupby;//may be length = 0 
	int aggrReducerN;
	ArrayList<RootExp> havingPreds;
	
	RootExp[] selList;
	//String[] selAlias;
	Schema schema;
	
	//String name;
}
