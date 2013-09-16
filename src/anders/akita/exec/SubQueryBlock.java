package anders.akita.exec;

import java.util.*;
import anders.akita.parser.*;

public class SubQueryBlock implements ITable{

	QueryBlock QB;
	
	SubQueryBlock preSubQB;
	boolean afterAggr;
	
	String alias;//allocate alias by PARSER
	
	boolean distinct;
	
	RootExp[] selectList;
	String[] selectAlias;
	int[] len; //used for varchar
	
	HashMap<String, RootExp> fieldList;
	
	JoinDesc join;// if preSubQB != NULL, then The first element of JOIN-LIST must be preSubQB.alias

	ArrayList<ZColRef> colRefsForRelSubQ;
	
	AggrDesc aggrDesc;	
	
	//middle QB table, contain QBs in from-clause
	
	HashMap<String, QueryBlock> derivedQB; //alias as KEY
	
	//non-relative sub-query
	ArrayList<QueryBlock> nonRelSubQB;
	
	String[] orderBy;
	
	long topK;
	public static final int FETCH_ALL = -1;
	int balanceReducerN;
	String[] hashBy;
	
	boolean generateID;//used for inner-query
	boolean isRoot;
	ArrayList<String> midQBTabList;
	
	public String getAlias(){
		return alias;
	}
	
	public void cleanup(){}
}
