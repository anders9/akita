package anders.akita.plan;

import java.util.*;

import anders.akita.meta.Meta;

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
		
		boolean first = true;
		
		ArrayList<String[]> srcL = new ArrayList<String[]>();
		ArrayList<String[]> srcPhyL = new ArrayList<String[]>();
		ArrayList<String[]> entryL = new ArrayList<String[]>();;
		
		for(SubQB sqb: qb.subQBs){
			
			//fetchDataOp, JoinOp, ..., aggrOp,
			int pos = 0;
			String[] src;
			String[] srcPhy;
			
			if(first){
				int distr = 0;
				int distrIdx = -1;
				while(pos < sqb.srcPys.length){
					if(Meta.getTab(sqb.srcPys[pos]).isDistributed())
						++distr;
					if(distr == 1)
						distrIdx = pos;
					if(distr == 2)
						break;
					++pos;
				}
				src = new String[pos];
				srcPhy = new String[pos];
				for(int i = 0; i < pos; ++i){
					src[i] = sqb.src[i];
					srcPhy[i] = sqb.srcPys[i];
				}
				srcL.add(src);
				srcPhyL.add(srcPhy);
				String[] entry;
				if(distr == 0)
					entry = Meta.randomEntries(1);
				else
					entry = Meta.getTab(sqb.srcPys[distrIdx]).getEntries();
				entryL.add(entry);
			}
			else
				pos = 1;
			
			
			
			
			
			if(first)
				first = false;
		}
		//shuffle op,
	}
}
