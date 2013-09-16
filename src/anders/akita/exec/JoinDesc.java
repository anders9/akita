package anders.akita.exec;

import java.util.ArrayList;

import anders.akita.parser.RootExp;

public class JoinDesc {
	
	public static final int NO_JOIN = -1;
	
	int joinType = NO_JOIN;
	
	JoinItem[] joinItems;
	
	JoinChain joinChain;

	ArrayList<RootExp> joinConds;
	
	ArrayList<RootExp> wherePreds;
	
}
