package anders.akita.exec;

import java.util.ArrayList;

import anders.akita.parser.RootExp;

public class JoinDesc {
	
	int joinType;
	
	JoinItem[] joinItems;
	
	ArrayList<RootExp> joinConds;
	
	ArrayList<RootExp> wherePreds;
	
}
