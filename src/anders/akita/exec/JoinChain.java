package anders.akita.exec;

import anders.akita.parser.*;
import java.util.*;

public class JoinChain {
	
	JoinChain prev;
	
	ArrayList<String> fromTabs;//used when JoinChain.prev==NULL
	String distrTab;//May be NULL,//used when JoinChain.prev==NULL
	
	//The following properties is used when JoinChain.prev != NULL
	String rhsTab;
	
	ArrayList<String> rhsLocalTabs;
	ArrayList<RootExp> joinConds;
	int join_type;
	
	int optType;
	int reducerN;
	
}
