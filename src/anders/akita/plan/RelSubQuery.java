package anders.akita.plan;

import java.util.ArrayList;

import anders.akita.parser.RootExp;

public class RelSubQuery {
	
	int pos;
	
	String colType;
	String[] src;
	String[] srcPhy;

	JoinPolicy[] joinPolicy;
	int[] joinReducerN;		

	int aggrReducerN;
	ArrayList<RootExp> wherePreds;
	
	RootExp havingPreds;
	
}
