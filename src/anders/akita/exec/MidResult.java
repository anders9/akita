package anders.akita.exec;

import java.util.*;

import anders.akita.parser.*;

public class MidResult implements ITable{
	
	String alias;//For last step of each subQB, alias is the subQB's alias, else $subQB_ALIAS+ID
	
	//MidField[] mf;
	
	ArrayList<ZColRef> fetchList;
	
	int[] len;
	JoinDesc jd;
	ArrayList<String> midQBTabList;
	ArrayList<RootExp> wherePreds;
	NodeEntry[] entries;
	
	RootExp[] selectList;
	String[] selectAlias;
	
	boolean isRelSubQ;
	RootExp[] groupby;
	ArrayList<RootExp> havingPreds;
	
	public void cleanup(){}
}
