package anders.akita.exec;

import java.util.*;

import anders.akita.parser.*;

public class AggrDesc {
	
	public static String REL_SUB_QB_ID = "$ID";
	
	boolean isRelSubQuery;
	
	ZColRef refCols;//used for relative sub-query
	
	RootExp[] groupBy; // reference with $gb1, $gb2, ..., length be zero if aggr for whole table
	
	//RootExp[] aggrExprs; // reference with $ag1, $ag2, ...
	
	RootExp[] preAggrExprs;// reference with $pa1, $pa2, ...
	
	//ZExpression[] aggrNode;
	
	String[] aggrFunc;
	boolean[] distinctAggr;
	boolean[] canMergeAggr;
	int[] aggrColLen;
	
	
	//RootExp[] exExprs;
	//String[] exExpr;
	//SubQueryBlock outerTab;//used for inner-query
	
	//boolean containMerger;
	//boolean containDistinctAggr;
	
	ArrayList<RootExp> havingPreds;
	
	int aggrReducerN;
	
	

	final static public int INVALID_VALUE = -1;
	
	static private String genMidAlias(int idx, String prefix){
		return prefix + idx;
	}
		
	static private int checkMidAlias(String alias, String prefix){
		if(alias.startsWith(prefix)){
			return Integer.valueOf(alias.substring(prefix.length()));
		}
		return INVALID_VALUE;
	}	
	
	static public String genGroupbyKeyAlias(int idx){
		return genMidAlias(idx, "$gb");
	}
		
	static public int checkGroupbyKey(String alias){
		return checkMidAlias(alias, "$gb");
	}

	static public String genAggrPh1Alias(int idx){
		return genMidAlias(idx, "$pa");
	}
		
	static public int checkAggrPh1Alias(String alias){
		return checkMidAlias(alias, "$pa");
	}


	static public String genAggrAlias(int idx){
		return genMidAlias(idx, "$ag");
	}
		
	static public int checkAggrAlias(String alias){
		return checkMidAlias(alias, "$ag");
	}

	public RootExp expandAggrPhase1(int index){
		
	}
	public void expandAggrPhase2(RootExp exp){
		
	}
	public void expandPreAggrCol(RootExp exp){
		
	}
	
}
