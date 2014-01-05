package anders.akita.plan;

import java.util.*;

public class QBPlan {
	
	public ArrayList<QBPlan> prevQBPlans;
	
	public FetchDataOperator[] operators;
	
	//String tmpTabName;
	
	public String[] entries;
	
	//String[] schema;
	//int[] colLen;
	public Schema schema;
	
	public QBPlan getSubQBPlan(String tabName){
		for(QBPlan qbp: prevQBPlans){
			if(tabName.equals(qbp.schema.name))
				return qbp;
		}
		return null;
	}
}
