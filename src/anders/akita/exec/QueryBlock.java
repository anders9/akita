package anders.akita.exec;

import java.util.*;
import anders.akita.parser.*;

public class QueryBlock {

	boolean distinct;
	
	ZExp[] selectList;
	String[] selectAlias;
	
	HashMap<String, ZExp> fieldList;
	
	int joinType;
	
	JoinItem[] joinItems;
	
	ArrayList<RootExp> joinConds;
	
	ArrayList<RootExp> wherePreds;
	
	ZColRef[] colRefs;
	
	AggrProc aggrProc;	
	
	QueryBlock[] nestedQB;//reference with $$nest1, $$nest2, ...
	
	String[] orderBy;
	
	long topK;
	public static final int FETCH_ALL = -1;
	int balanceReducerN;
	String[] hashBy;
	
	boolean generateID;//used for inner-query
	boolean isRoot;
}
