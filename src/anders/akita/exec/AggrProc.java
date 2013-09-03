package anders.akita.exec;

import java.util.*;

import anders.akita.parser.*;

public class AggrProc {
	
	RootExp[] groupBy; // reference with $gb1, $gb2, ...
	
	RootExp[] aggrExprs; // reference with $ag1, $ag2, ...
	
	//boolean containMerger;
	//boolean containDistinctAggr;
	
	ArrayList<RootExp> havingPreds;
	
	int aggrReducerN;
	
}
