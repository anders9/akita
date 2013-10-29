package anders.akita.plan;

import java.util.*;

public class QBPlan {
	
	ArrayList<QBPlan> prevQBPlans;
	
	FetchDataOperator[] operators;
	
	String tmpTabName;
	
	String[] entries;
	
	String[] schema;
	int[] colLen;
	
}
