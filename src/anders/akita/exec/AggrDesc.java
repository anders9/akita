package anders.akita.exec;

import java.util.*;

import anders.akita.parser.*;

public class AggrDesc {
	
	RootExp[] groupBy; // reference with $gb1, $gb2, ...
	
	RootExp[] aggrExprs; // reference with $ag1, $ag2, ...
	
	//RootExp[] exExprs;
	//String[] exExpr;
	String outerTab;
	
	//boolean containMerger;
	//boolean containDistinctAggr;
	
	ArrayList<RootExp> havingPreds;
	
	int aggrReducerN;
	
}
