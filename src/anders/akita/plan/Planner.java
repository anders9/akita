package anders.akita.plan;

import java.util.*;

public class Planner {
	
	QBPlan genQBPlan(QB qb){
		
		QBPlan plan = new QBPlan();
		
		plan.prevQBPlans = new ArrayList<QBPlan>();
		
		for(QB pqb: qb.prevQBs){
			
			QBPlan qbp = genQBPlan(pqb);
			
			plan.prevQBPlans.add(qbp);
		}

		plan.operators = genSubQBPlan(qb);

		return plan;
	}
	
	FetchDataOperator[] genSubQBPlan(QB qb){
		
		for(SubQB sqb: qb.subQBs){
			
			//fetchDataOp, JoinOp, ..., aggrOp,
			
		}
		//shuffle op,
	}
}
