package anders.akita.exec;

import java.util.*;
import anders.akita.parser.*;

public class QueryBlock {

	boolean distinct;
	
	ArrayList<ZExp> selectList;
	
	HashMap<String, ZExp> fieldList;
	
	int joinType;
	
	JoinItem[] joinItems;
	
	ArrayList<RootExp> joinConds;
	
	ArrayList<RootExp> wherePreds;
	
	RootExp[] groupBy;
	ArrayList<RootExp> havingPreds;
	int aggrReducerN;
	
	String[] orderBy;
	
	long topK;
	public static final int FETCH_ALL = -1;
	int balanceReducerN;
	String[] hashBy;
	
	boolean addID;//used for inner-query
}
