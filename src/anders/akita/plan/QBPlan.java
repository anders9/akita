package anders.akita.plan;

import java.util.*;

public class QBPlan {
	
	ArrayList<QBPlan> prevQBPlans;
	
	FetchDataOperator[] operators;
	
	//String tmpTabName;
	
	String[] entries;
	
	//String[] schema;
	//int[] colLen;
	Schema schema;
	
	public QBPlan getSubQBPlan(String tabName){
		for(QBPlan qbp: prevQBPlans){
			if(tabName.equals(qbp.schema.name))
				return qbp;
		}
		return null;
	}
}
