package anders.akita.plan;

import java.util.*;

import anders.akita.parser.*;

public class QB {
	
	//ArrayList<QB> prevQBs;
	HashMap<String, QB> prevQBs;//contain nested from clause & non-relative inner-query
	HashMap<String, ArrayList<String>> nonRelSubQVarMap;
	//SubQB[] subQBs;
	
	String[] src;
	String[] srcPhy;//if is nested from clause, srcPhy = src
	JoinType joinType;
	ArrayList<RootExp> joinCond;
	JoinPolicy[] joinPolicy;
	int[] joinReducerN;		
	RelSubQuery[] relSubQ;
	
	ArrayList<RootExp> where;
	
	ZColRef[] groupby;//may be length = 0, when aggr the whole table. = null when not contain aggr operation
	int aggrReducerN;
	ArrayList<RootExp> havingPreds;
	
	RootExp[] selList;
	//String[] selAlias;
	Schema schema;
	
	//!!!!
	boolean distinct;
	String[] orderby;//if null, no order-by operation
	boolean[] orderbyAsc;
	int shuffleCnt; // if == 0, no shuffle
}
