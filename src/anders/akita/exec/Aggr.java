package anders.akita.exec;

import java.util.*;

import anders.akita.parser.*;

public class Aggr {
	
	RootExp[] groupBy;
	
	RootExp[] aggrExprs;
	
	//boolean containMerger;
	//boolean containDistinctAggr;
	
	ArrayList<RootExp> havingPreds;
	
	int aggrReducerN;
	
}
