package anders.akita.plan;

import java.util.*;

import anders.akita.meta.Meta;
import anders.akita.parser.RootExp;

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
	
	static class OpBase{
		String[] fetchCol;
		int endPos;
		int rsqPos = -1;
		int rsqEndPos = -1;
		boolean genID = false;
		boolean removeID = false;
	}
	//src, srcPhy, entry, 
	static class OpFetchData extends OpBase{
		String[] src;
		String[] srcPhy;
		JoinType joinType = JoinType.INNER;
		ArrayList<RootExp> joinCond;		
		String[] entry;
		int distrIdx;
		ArrayList<RootExp> where;
	}
	static class OpJoin extends OpBase{
		String[] src;
		String[] srcPhy;
		JoinType joinType;
		ArrayList<RootExp> joinCond;
		JoinPolicy joinPolicy;
		int joinReducerN;
		String[] rhsEntry;
		String[] reducerEntry;
		ArrayList<RootExp> where;
	}
	static class OpAggr extends OpBase{
		int aggrReducerN;
		String[] groupby;//if NULL, is for relative-sub-query
		ArrayList<RootExp> havingPreds;
	}
	/*
	static class OpAggr{
		String[] groupby;
		RootExp[] aggrExprs;
		int aggrReducerN;
		ArrayList<RootExp> havingPreds;		
	}
	*/
	FetchDataOperator[] genSubQBPlan(QB qb){
		
		ArrayList<OpBase> opList = new ArrayList<OpBase>();
		
		int ri = 0;
		int end = qb.src.length;
		int relPos = 0;
		if(qb.relSubQ.length > 0)
			end = qb.relSubQ[0].pos;
		
		int pos = 0;
		int distr = 0;
		int distrIdx = -1;
		while(pos < end){
			if(Meta.getTab(qb.srcPhy[pos]).isDistributed())
				++distr;
			if(distr == 1)
				distrIdx = pos;
			if(distr == 2)
				break;
			++pos;
		}
		OpFetchData ofp = new OpFetchData();
		ofp.src = new String[pos];
		ofp.srcPhy = new String[pos];
		for(int i = 0; i < pos; ++i){
			ofp.src[i] = qb.src[i];
			ofp.srcPhy[i] = qb.srcPhy[i];
		}
		ofp.distrIdx = distrIdx;
		ofp.entry = distrIdx == -1 ?
				Meta.randomEntries(1) : 
				Meta.getTab(ofp.srcPhy[distrIdx]).getEntries();
		ofp.endPos = pos;
		ofp.joinType = qb.joinType;
		ofp.joinCond = qb.joinCond;
		
		opList.add(ofp);
		
		while(true){
			
			while(pos < end){
					
				int beg = pos;
				++pos;
				while (pos < end) {
					if (Meta.getTab(qb.srcPhy[pos]).isDistributed())
						break;
				}

				OpJoin oj = new OpJoin();
				oj.src = new String[pos - beg];
				oj.srcPhy = new String[pos - beg];
				
				for(int i = beg; i < pos; ++i){
					oj.src[i - beg] = qb.src[i];
					oj.srcPhy[i - beg] = qb.srcPhy[i];
				}
				oj.joinType = qb.joinType;
				oj.joinCond = qb.joinCond;
				oj.joinPolicy = qb.joinPolicy[beg];
				oj.joinReducerN = qb.joinReducerN[beg];
				oj.rhsEntry = Meta.getTab(oj.srcPhy[0]).getEntries();
				if(oj.joinPolicy == JoinPolicy.Reduceside)
					oj.reducerEntry = Meta.randomEntries(oj.joinReducerN);
				oj.endPos = pos;
				
				opList.add(oj);
			}
			
			boolean first = true;
			while(relPos < qb.relSubQ.length){
				
				if(!first && qb.relSubQ[relPos].pos != qb.relSubQ[relPos - 1].pos)
					break;
				
				RelSubQuery rsq = qb.relSubQ[relPos];
				opList.get(opList.size() - 1).genID = true;
				int k = 0;
				while(!Meta.getTab(rsq.srcPhy[k]).isDistributed())
					++k;
				
				if(k > 0){
					//add a single-node map-side join operator
					OpJoin oj = new OpJoin();
					for(int i = 0; i < k; ++i){
						oj.src[i] = rsq.src[i];
						oj.srcPhy[i] = rsq.srcPhy[i];
						oj.joinType = JoinType.INNER;
						oj.joinPolicy = JoinPolicy.Mapside;
					}
					oj.endPos = pos;
					oj.rsqPos = relPos;
					oj.rsqEndPos = k;
					opList.add(oj);
				}
				while(k < rsq.srcPhy.length){
					int beg = k;
					++k;
					while (k < rsq.srcPhy.length) {
						if (Meta.getTab(rsq.srcPhy[k]).isDistributed())
							break;
						++k;
					}
					OpJoin oj = new OpJoin();
					oj.src = new String[k - beg];
					oj.srcPhy = new String[k - beg];
					
					for(int i = beg; i < k; ++i){
						oj.src[i - beg] = rsq.src[i];
						oj.srcPhy[i - beg] = rsq.srcPhy[i];
					}
					oj.joinType = JoinType.INNER;
					oj.joinPolicy = rsq.joinPolicy[beg];
					oj.joinReducerN = rsq.joinReducerN[beg];
					oj.rhsEntry = Meta.getTab(oj.srcPhy[0]).getEntries();
					if(oj.joinPolicy == JoinPolicy.Reduceside)
						oj.reducerEntry = Meta.randomEntries(oj.joinReducerN);
					
					oj.endPos = pos;
					oj.rsqPos = relPos;
					oj.rsqEndPos = k;
					opList.add(oj);
				}
				//add aggregation node for Relative-Sub-query
				OpAggr oa = new OpAggr();
				oa.aggrReducerN = rsq.aggrReducerN;
				oa.havingPreds = rsq.havingPreds;
				oa.removeID = true;
				
				oa.endPos = pos;
				oa.rsqPos = relPos;
				oa.rsqEndPos = k;
				
				opList.add(oa);
				
				if(first)
					first = false;
				++relPos;
			}
			
			if(end == qb.src.length)
				break;
			
			if(relPos < qb.relSubQ.length)
				end = qb.relSubQ[relPos].pos;
			else
				end = qb.src.length;
		}
		OpAggr oa = new OpAggr();
		oa.aggrReducerN = qb.aggrReducerN;
		oa.groupby = qb.groupby;
		oa.havingPreds = qb.havingPreds;
		oa.endPos = qb.src.length;
		
		opList.add(oa);
	}

}
