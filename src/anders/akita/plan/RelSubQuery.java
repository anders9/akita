package anders.akita.plan;

import java.util.ArrayList;

import anders.akita.parser.RootExp;

public class RelSubQuery {
	
	int pos;
	
	String[] src;
	String[] srcPhy;

	JoinPolicy[] joinPolicy;
	int[] joinReducerN;		

	int aggrReducerN;
	ArrayList<RootExp> wherePreds;
	ArrayList<RootExp> havingPreds;
}
