package anders.akita.plan;

import anders.akita.parser.*;
import java.util.*;

public class JoinChain {
	
	JoinChain prev;

	ArrayList<RootExp> joinConds;
	int join_type;
	
	ArrayList<ITable> fromTabs;//used when JoinChain.prev==NULL
	ITable distrTab;//May be NULL,//used when JoinChain.prev==NULL
	
	//The following properties is used when JoinChain.prev != NULL
	String rhsTab;
	
	ArrayList<String> rhsLocalTabs;

	
	int optType;
	int reducerN;
	
}
