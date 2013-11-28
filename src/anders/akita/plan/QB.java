package anders.akita.plan;

import java.util.*;

import anders.akita.parser.*;

public class QB {
	
	ArrayList<QB> prevQBs;
	
	//SubQB[] subQBs;
	
	String[] src;
	String[] srcPhy;
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
	String[] selAlias;
	
	String name;
	
}
