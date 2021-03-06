package anders.akita.plan;

import java.util.*;

import anders.akita.meta.*;
import anders.akita.parser.*;
import anders.util.*;

public class Planner {
	
	QB rootqb;
	long qid;
	
	public Planner(QB qb, long qid){
		this.rootqb = qb;
		this.qid = qid;
	}
	
	public QBPlan genQBPlan(){
		return genQBPlanIter(rootqb);
	}
	
	private QBPlan genQBPlanIter(QB qb){
		
		QBPlan plan = new QBPlan();
		
		plan.prevQBPlans = new ArrayList<QBPlan>();
		
		for(QB pqb: qb.prevQBs.values()){
			
			QBPlan qbp = genQBPlanIter(pqb);
			
			plan.prevQBPlans.add(qbp);
		}

		plan.operators = genSubQBPlan(qb, plan);

		FetchDataOperator lastOp = plan.operators[plan.operators.length - 1];
		plan.entries = lastOp.entries.clone();
		plan.schema = (Schema)qb.schema.clone();
		
		return plan;
	}
	
	static class OpBase{
		ArrayList<String> fetchCol;
		QBClause qbClause;
		int endPos;
		int rsqPos = -1; // the index in relSubQ array
		int rsqEndPos = -1; // the end position in this relSubQ's src array
		boolean genID = false;//for next step
		boolean containID = false;//whether this step's output tab contain ID
	}
	//src, srcPhy, entry, 
	static class OpFetchData extends OpBase{
		String[] src;
		String[] srcPhy;
		JoinType joinType = JoinType.INNER;
		ArrayList<RootExp> joinCond;		
		//String[] entry;
		String distrSrcPhy;
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
		//String[] rhsEntry;
		//String[] reducerEntry;
		//String collectEntry;
		ArrayList<RootExp> where;
	}
	/*
	static class OpRelAggr extends OpBase{
		int aggrReducerN;
		//String[] reducerEntry;
		//RootExp[] aggrExprs;
		//String[] groupby;//if NULL, is for relative-sub-query
		ArrayList<RootExp> havingPreds;
	}*/
	/*
	static class OpAggr{
		String[] groupby;
		RootExp[] aggrExprs;
		int aggrReducerN;
		ArrayList<RootExp> havingPreds;		
	}
	*/
	
	 static ArrayList<String> getPredCoverSrc(RootExp pred)
	 {
		final ArrayList<String> cov = new ArrayList<String>();
		try{
			pred.traverse(new NodeVisitor(){
				public void visit(ZExp node, RootExp root)
						throws ExecException{
					if(node instanceof ZColRef){
						ZColRef cr = (ZColRef)node;
						if(!cov.contains(cr.table)){
							cov.add(cr.table);
						}
					}
				}
			});
		}catch(ExecException e){}
		return cov;
	}
	static boolean checkCanPushDown(String[] srcs, ArrayList<String> covers){
		for(String c: covers){
			int l = Util.findStr(QBParser.getColSrc(c), srcs);
			if(l == -1)
				return false;
		}
		return true;
	}
	 
	 
	static int getPushdownLevel(String[] srcs, ArrayList<String> covers){
		int level = 0;
		for(String c: covers){
			int l = Util.findStr(c, srcs);
			if(l != -1 && l > level)
				level = l;
		}
		return level;
	}
	static ArrayList<RootExp>[] genPushDownPredsArray(String[] src, ArrayList<RootExp> preds){
		ArrayList<RootExp>[] pdList = new ArrayList[src.length];
		if(preds == null)
			preds = new ArrayList<RootExp>();
		for(int i = 0; i < src.length; ++i){
			pdList[i] = new ArrayList<RootExp>();
		}
		for(RootExp pred: preds){
			int lv = getPushdownLevel(src, getPredCoverSrc(pred));
			pdList[lv].add(pred);
		}
		return pdList;
	}
	static ArrayList<RootExp> mergePredArray(ArrayList<RootExp>[] preds, int beg, int end){
		ArrayList<RootExp> list = new ArrayList<RootExp>();
		for(int i = beg; i < end; ++i){
			list.addAll(preds[i]);
		}
		return list;
	}
	

	static ArrayList<String> getCoverCol(RootExp exp){
		final ArrayList<String> list = new ArrayList<String>();
		try{
			exp.traverse(new NodeVisitor(){
				public void visit(ZExp node, RootExp root) throws ExecException {
					if(node instanceof ZColRef){
						ZColRef cr = (ZColRef)node;
						String col = cr.toString();
						if(!list.contains(col))
							list.add(col);
					}
				}
			});
		}catch(Exception e){}
		return list;
	}
	static ArrayList<String> getCoverCol(ArrayList<RootExp> expList){
		ArrayList<String> list = new ArrayList<String>();
		for(RootExp e: expList){
			list.addAll(getCoverCol(e));
		}
		return list;
	}
	static ArrayList<String> getCoverCol(RootExp[] expList){
		ArrayList<String> list = new ArrayList<String>();
		for(RootExp e: expList){
			list.addAll(getCoverCol(e));
		}
		return list;
	}
	static ArrayList<String> filteCoverCol(String[] cutSrc, ArrayList<String> list){
		ArrayList<String> rlist = new ArrayList<String>();
		for(String col: list){
			String s = QBParser.getColSrc(col);
			if(Util.findStr(s, cutSrc) == -1)
				rlist.add(col);
		}
		return rlist;
	}
	static ArrayList<String> filteCoverColKeep(String[] src, ArrayList<String> list){
		ArrayList<String> rlist = new ArrayList<String>();
		for(String col: list){
			String s = QBParser.getColSrc(col);
			if(Util.findStr(s, src) != -1)
				rlist.add(col);
		}
		return rlist;
	}
	
	
	String genTmpTableName(QB qb, int step){
		String prefix = qb.genNamePrefix(qid);
		return prefix + "_step_" + step; 
	}
	
	FetchDataOperator[] genSubQBPlan(QB qb, QBPlan qbPlan){
		
		ArrayList<OpBase> opList = new ArrayList<OpBase>();
		
		//push predicates down
		ArrayList<RootExp>[] pdList = genPushDownPredsArray(qb.src, qb.where);
		//special process for RIGHT-JOIN
		if(qb.src.length == 2 && qb.joinType == JoinType.RIGHT){
			//when right-join, can't push down where-preds to left-src, so restore them.
			pdList[1].addAll(pdList[0]);
			pdList[0].clear();
		}
		//process Join condition
		if(qb.src.length == 2 && qb.joinType != JoinType.INNER && qb.joinCond != null){
			if(qb.joinType == JoinType.RIGHT){
				//when right-join, can push join-condition into left-src.
				Iterator<RootExp> iter = qb.joinCond.iterator();
				while(iter.hasNext()){
					RootExp jc = iter.next();
					int lv = getPushdownLevel(qb.src, getPredCoverSrc(jc));
					if(lv == 0){
						pdList[0].add(jc);
						iter.remove();
					}
				}
			}
		}
		/*
		ArrayList<RootExp>[][] spdList = new ArrayList[qb.relSubQ.length][];
		for(int i = 0; i < spdList.length; ++i){
			RelSubQuery rsq = qb.relSubQ[i];
			spdList[i] = genPushDownPredsArray(rsq.src, rsq.wherePreds);
		}*/
		//process LEFT/RIGHT JOIN condition 
		
		
		int ri = 0;
		int end = qb.src.length;
		//int relPos = 0;
		//if(qb.relSubQ.length > 0)
		//	end = qb.relSubQ[0].pos;
		
		int pos = 0;
		int distr = 0;
		int distrIdx = -1;
		while(pos < end){
			if(/*Meta.getTab(qb.srcPhy[pos]).isDistributed()*/
					Planner.checkSrcPhyDistribute(qbPlan, qb.srcPhy[pos]))
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
		//ofp.entry = distrIdx == -1 ?
				//Meta.randomEntries(1) : 
				/*Meta.getTab(ofp.srcPhy[distrIdx]).getEntries()*/
				//Planner.getSrcPhyEntries(qb, qb.src[distrIdx]);
		
		ofp.endPos = pos;
		ofp.joinType = qb.joinType;
		ofp.joinCond = qb.joinCond;
		ofp.where = mergePredArray(pdList, 0, pos);
		opList.add(ofp);
		
		while(true){
			
			while(pos < end){
					
				int beg = pos;
				++pos;
				while (pos < end) {
					if (/*Meta.getTab(qb.srcPhy[pos]).isDistributed()*/
							Planner.checkSrcPhyDistribute(qbPlan, qb.srcPhy[pos]))
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
				if(oj.joinPolicy == null)
					oj.joinPolicy = JoinPolicy.Mapside;
				oj.joinReducerN = qb.joinReducerN[beg];
				//oj.rhsEntry = /*Meta.getTab(oj.srcPhy[0]).getEntries()*/Planner.getSrcEntries(qb, qb.src[0]);
				//if(oj.joinPolicy == JoinPolicy.Reduceside)
				//	oj.reducerEntry = Meta.randomEntries(oj.joinReducerN);
				
				oj.endPos = pos;
				oj.where = mergePredArray(pdList, beg, pos);
				opList.add(oj);
			}
			/*
			boolean first = true;
			while(relPos < qb.relSubQ.length){
				
				if(!first && qb.relSubQ[relPos].pos != qb.relSubQ[relPos - 1].pos)
					break;
				
				RelSubQuery rsq = qb.relSubQ[relPos];
				opList.get(opList.size() - 1).genID = true;
				int k = 0;
				while(k < rsq.srcPhy.length && 
						!Planner.checkSrcPhyDistribute(qbPlan, qb.srcPhy[k])
						)
					++k;
				
				if(k > 0){
					//add a local-join operator
					OpJoin oj = new OpJoin();
					for(int i = 0; i < k; ++i){
						oj.src[i] = rsq.src[i];
						oj.srcPhy[i] = rsq.srcPhy[i];
						oj.joinType = JoinType.INNER;
						oj.joinPolicy = JoinPolicy.Local;
					}
					oj.endPos = pos;
					oj.rsqPos = relPos;
					oj.rsqEndPos = k;
					oj.where = mergePredArray(spdList[relPos], 0, k);
					oj.containID = true;
					opList.add(oj);
				}
				while(k < rsq.srcPhy.length){
					int beg = k;
					++k;
					while (k < rsq.srcPhy.length) {
						if (
								Planner.checkSrcPhyDistribute(qbPlan, qb.srcPhy[k]))
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
					//oj.rhsEntry = Planner.getSrcEntries(qb, oj.src[0]);
					//if(oj.joinPolicy == JoinPolicy.Reduceside)
					//	oj.reducerEntry = Meta.randomEntries(oj.joinReducerN);
					
					oj.endPos = pos;
					oj.rsqPos = relPos;
					oj.rsqEndPos = k;
					oj.where = mergePredArray(spdList[relPos], beg, k);
					oj.containID = true;
					opList.add(oj);
				}
				//add aggregation node for Relative-Sub-query
				OpRelAggr oa = new OpRelAggr();
				oa.aggrReducerN = rsq.aggrReducerN;
				//oa.reducerEntry = Meta.randomEntries(rsq.aggrReducerN);
				oa.havingPreds = new ArrayList<RootExp>();
				oa.havingPreds.add(rsq.havingPreds);
				oa.containID = false;//By default, AGGR-node output table not contain ID, but if next step need ID (chain Inner-query),
									// then keep ID, this place will be treated specially.
				
				oa.endPos = pos;
				oa.rsqPos = relPos;
				oa.rsqEndPos = k;
				
				opList.add(oa);
				
				if(first)
					first = false;
				++relPos;
			}
			*/
			if(end == qb.src.length)
				break;
			/*
			if(relPos < qb.relSubQ.length)
				end = qb.relSubQ[relPos].pos;
			else
				end = qb.src.length;
			*/
		}
		
		/*
		 * FetchData, Join, ... Join(genID),| Join(containID), ... Aggr( [genID]),| ...
		 */
		
		ArrayList<String> fcols;
		//get col set...
		fcols = getCoverCol(qb.selList);
		if(qb.groupby != null){
			ArrayList<String> t = new ArrayList<String>();
			for(ZColRef gb : qb.groupby)
				t.add(gb.toString());
			fcols = Util.<String>mergeArrayList(fcols, t);
			if(qb.havingPreds != null){
				t = getCoverCol(qb.havingPreds);
				fcols = Util.<String>mergeArrayList(fcols, t);
			}
		}
		
		//pruning column...
		for(int k = opList.size() - 1; k >= 0; --k){
			OpBase ob = opList.get(k);
			ob.fetchCol = (ArrayList<String>)fcols.clone();
			/*if(ob instanceof OpRelAggr){
				OpRelAggr oa = (OpRelAggr)ob;
				fcols = Util.<String>mergeArrayList(fcols, getCoverCol(oa.havingPreds));
			}
			else */if(ob instanceof OpJoin){
				OpJoin oj = (OpJoin)ob;
				fcols = Util.<String>mergeArrayList(fcols, getCoverCol(oj.where));
				if(oj.joinCond != null)
					fcols = Util.<String>mergeArrayList(fcols, getCoverCol(oj.joinCond));
				fcols = filteCoverCol(oj.src, fcols);
			}
			else if(ob instanceof OpFetchData){
				//Do nothing
			}
		}
		
		ArrayList<FetchDataOperator> ops = new ArrayList<FetchDataOperator>();		
		
		for(int i = 0; i < opList.size(); ++i){
			OpBase ob = opList.get(i), prevOb = null;
			
			FetchDataOperator fdo = null, prevOp = null;
			if(i > 0){
				prevOp = ops.get(ops.size() - 1);
				prevOb = opList.get(i - 1);
			}
			
			if(ob instanceof OpFetchData){
				
				assert i == 0;
				
				OpFetchData ofd = (OpFetchData)ob;
				fdo = new FetchDataOperator();
				//fdo.schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i), ob.fetchCol, false);
				
				ob.qbClause = genSelectClause(qb, i, 
						ofd.src, ofd.srcPhy,
						null,
						ofd.joinType,
						ofd.joinCond,
						ofd.where, ofd.fetchCol, null, false);
				fdo.schema = ob.qbClause.schema;
				
				fdo.fetchSQL = ob.qbClause.toString();
				
				fdo.nonRelSubQVar = ob.qbClause.nonRelSubQVar;
				fdo.entries = ofd.distrIdx == -1 ?
						Meta.randomEntries(1) : 
						Planner.getSrcPhyEntries(qbPlan, ofd.srcPhy[ofd.distrIdx]);
				
				fdo.tmpTabList = new ArrayList<String>();//No temp tab generated this step
				
				ops.add(fdo);
			}
			else if(ob instanceof OpJoin){
				OpJoin oj = (OpJoin)ob;
				/*if(oj.joinPolicy == JoinPolicy.Local){
					fdo = new LocalJoinOperator();
					LocalJoinOperator ljo = (LocalJoinOperator)fdo;
					//fdo.schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i), ob.fetchCol, oj.containID);
					
					ljo.leftSrc = prevOp;
					assert oj.containID == true;
					
					ljo.genPrevID = !ljo.leftSrc.schema.containID;
					fdo.entries = ljo.leftSrc.entries;
					ob.qbClause = genSelectClause(qb, i, oj.src, oj.srcPhy,
							qb.relSubQ[ob.rsqPos],
							oj.joinType, oj.joinCond, oj.where, oj.fetchCol, new String[]{ljo.leftSrc.schema.name}, true);
					fdo.schema = ob.qbClause.schema;
					fdo.fetchSQL = ob.qbClause.toString();
					fdo.nonRelSubQVar = ob.qbClause.nonRelSubQVar;
					
					fdo.tmpTabList = new ArrayList<String>();
					fdo.tmpTabList.add(ljo.leftSrc.schema.name);
				}
				else */if(oj.joinPolicy == JoinPolicy.Mapside){
					fdo = new MapJoinOperator();
					//fdo.schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i), ob.fetchCol, oj.containID);
					MapJoinOperator mjo = (MapJoinOperator)fdo;
					
					mjo.leftSrc = prevOp;
					fdo.entries = Planner.getSrcPhyEntries(qbPlan, oj.srcPhy[0]);
					
					mjo.collectNode = Meta.randomEntries(1)[0];
					mjo.genPrevID = oj.containID && !mjo.leftSrc.schema.containID;
					
					ob.qbClause = genSelectClause(qb, i, oj.src, oj.srcPhy, 
							null, //ob.rsqPos == -1 ? null : qb.relSubQ[ob.rsqPos],
							oj.joinType,
							oj.joinCond,
							oj.where, oj.fetchCol, new String[]{mjo.leftSrc.schema.name},
							oj.containID
							);
					fdo.fetchSQL = ob.qbClause.toString();
					fdo.schema = ob.qbClause.schema;
					
					fdo.nonRelSubQVar = ob.qbClause.nonRelSubQVar;
					
					fdo.tmpTabList = new ArrayList<String>();
					fdo.tmpTabList.add(mjo.leftSrc.schema.name);
				}
				else if(oj.joinPolicy == JoinPolicy.Reduceside){
					fdo = new ReduceJoinOperator();
					//fdo.schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i), ob.fetchCol, oj.containID);
					ReduceJoinOperator rjo = (ReduceJoinOperator)fdo;
					
					rjo.srcs = new FetchDataOperator[2];
					rjo.srcs[0] = prevOp;
					
					ArrayList<RootExp> rhsWhere = genJoinRhsPred(oj.joinType, oj.joinCond, oj.src, oj.where);
					
					rjo.srcs[1] = new FetchDataOperator();
					//generate rhs-table column
					ArrayList<String> rhsCols = (ArrayList<String>)oj.fetchCol.clone();
					rhsCols = Util.<String>mergeArrayList(rhsCols, getCoverCol(oj.where));
					if(oj.joinCond != null)
						rhsCols = Util.<String>mergeArrayList(rhsCols, getCoverCol(oj.joinCond));
					rhsCols = filteCoverColKeep(oj.src, rhsCols);
					//rjo.srcs[1].schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i) + "_rhs", rhsCols, false);
					rjo.srcs[1].entries = Planner.getSrcPhyEntries(qbPlan, oj.srcPhy[0]);;
					
					QBClause rhsQBclause = genSelectClause(qb, i, oj.src, oj.srcPhy,
							null, //ob.rsqPos == -1 ? null : qb.relSubQ[ob.rsqPos],
							JoinType.INNER, null, rhsWhere, rhsCols, null, false);
							
					rjo.srcs[1].fetchSQL = rhsQBclause.toString();
					rjo.srcs[1].schema = rhsQBclause.schema;
					rjo.srcs[1].schema.name += "_rhs"; //avoid name conflict with result fetch table
					rjo.srcs[1].nonRelSubQVar = rhsQBclause.nonRelSubQVar;
					
					fdo.tmpTabList = new ArrayList<String>();
					fdo.tmpTabList.add(rjo.srcs[0].schema.name);
					fdo.tmpTabList.add(rjo.srcs[1].schema.name);
					
					//rjo.joinKeyIdx
					String[][] jk = Planner.genJoinKeyIdx(oj.src, oj.joinType, oj.joinCond, oj.where);
					
					int[][] jkIdx = new int[jk.length][2];
					
					for(int k = 0; k < jk.length; ++k){
						jkIdx[k][0] = Util.findStr(jk[k][0], rjo.srcs[0].schema.col);
						jkIdx[k][1] = Util.findStr(jk[k][1], rjo.srcs[1].schema.col);
					}
					rjo.joinKeyIdx = jkIdx;
					
					fdo.entries = Meta.randomEntries(oj.joinReducerN);
					String[] jsrc = new String[]{rjo.srcs[0].schema.name, rjo.srcs[1].schema.name};
					ob.qbClause = genSelectClause(qb, i, new String[0], new String[0],
							null, //ob.rsqPos == -1 ? null : qb.relSubQ[ob.rsqPos],
							oj.joinType, oj.joinCond, oj.where, oj.fetchCol, jsrc,
							oj.containID
							);
					fdo.schema = ob.qbClause.schema;
					fdo.fetchSQL = ob.qbClause.toString();
					fdo.nonRelSubQVar = ob.qbClause.nonRelSubQVar;
					
					rjo.genPrevID = oj.containID && !rjo.srcs[0].schema.containID;
				}
				else
					assert false;
				ops.add(fdo);
			}
			/*
			else if(ob instanceof OpRelAggr){
				boolean containID = ob.genID;//if $genID for next step, no need to re-gen, only keep ID column from src-tab
				OpRelAggr ora = new OpRelAggr();
				
				if(prevOp.entries.length == 1){
					//push current node into prev-node (fetch data)
					ob.qbClause = genAggrClause(qb, i, prevOb.qbClause, 
							qb.relSubQ[ob.rsqPos],
							ora.fetchCol,
							new String[]{"id"}, ora.havingPreds, containID);
					prevOb.qbClause = ob.qbClause;
					
					//replace previous operator's fetch data SQL, not gen new operator
					prevOp.fetchSQL = ob.qbClause.toString();
					prevOp.schema = ob.qbClause.schema;
					prevOp.nonRelSubQVar = ob.qbClause.nonRelSubQVar;
					//prevOp.schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i), ob.fetchCol, containID);
				}
				else{
					QBClause[] qbClauses = new QBClause[2];
					gen2PhaseAggrClause(qb, i, prevOb.qbClause,
							qb.relSubQ[ob.rsqPos],
							ora.fetchCol,  
							new String[]{"id"}, ora.havingPreds, containID,
							qbClauses);
					//modify previous node
					prevOb.qbClause = qbClauses[0];
					prevOp.fetchSQL = qbClauses[0].toString();
					prevOp.nonRelSubQVar = qbClauses[0].nonRelSubQVar;
					prevOp.schema = qbClauses[0].schema;
					
					fdo = new AggrOperator();
					AggrOperator ao = (AggrOperator)fdo;
					//fdo.schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i), ob.fetchCol, containID);
					ao.src = prevOp;
					fdo.entries = Meta.randomEntries(ora.aggrReducerN);
					
					ob.qbClause = qbClauses[1];
					fdo.fetchSQL = qbClauses[1].toString();
					//fdo.schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i), ob.fetchCol, containID);
					fdo.schema = qbClauses[1].schema;
					fdo.nonRelSubQVar = qbClauses[1].nonRelSubQVar;
					fdo.tmpTabList = new ArrayList<String>();
					fdo.tmpTabList.add(ao.src.schema.name);
					
					ops.add(fdo);
				}
			}*/
		}
		
		FetchDataOperator prevOp = ops.get(ops.size() - 1);
		QBClause prevClause = opList.get(opList.size() - 1).qbClause;
		int obIdx = opList.size();
		
		
		if(qb.groupby != null){
			//append Aggr clause if exist, if last operator is AggrOp, then generate two-level aggr clause
			
			if(prevOp.entries.length == 1){
				//push current node into prev-node (fetch data)
				prevClause = genAggrClause(qb, obIdx, prevClause, null, null, 
						null, null, false);
				
				//replace previous operator's fetch data SQL, not gen new operator
				prevOp.fetchSQL = prevClause.toString();
				prevOp.nonRelSubQVar = prevClause.nonRelSubQVar;
				prevOp.schema = prevClause.schema;
			}
			else{
				QBClause[] qbClauses = new QBClause[2];
				gen2PhaseAggrClause(qb, obIdx, prevClause, null, null,  
						null, null, false,
						qbClauses);
				//modify previous node
				//prevClause = qbClauses[0];
				prevOp.fetchSQL = qbClauses[0].toString();
				prevOp.nonRelSubQVar = qbClauses[0].nonRelSubQVar;
				prevOp.schema = qbClauses[0].schema;
				
				AggrOperator ao = new AggrOperator();
				
				ao.src = prevOp;
				ao.entries = Meta.randomEntries(qb.aggrReducerN);
				
				ao.shuffleKeyIdx = new int[qb.groupby.length];
				for(int i = 0; i < ao.shuffleKeyIdx.length; ++i){
					ao.shuffleKeyIdx[i] = 
						Util.findStr(qb.groupby[i].toString().replace('.', '$'), prevOp.schema.col);
				}
				prevClause = qbClauses[1];
				ao.fetchSQL = qbClauses[1].toString();
				ao.schema = qbClauses[1].schema;
				ao.nonRelSubQVar = qbClauses[1].nonRelSubQVar;
				
				ao.tmpTabList = new ArrayList<String>();
				ao.tmpTabList.add(ao.src.schema.name);
				
				ops.add(ao);
				prevOp = ao;
			}			
		}
		else{
			//!!!
			//replace select expr list into prevClause
			
			prevClause = genSelExpClause(qb, obIdx, prevClause);
			prevOp.fetchSQL = prevClause.toString();
			prevOp.schema = prevClause.schema;
			prevOp.nonRelSubQVar = prevClause.nonRelSubQVar;
		}
		++obIdx;
		
		//last step,
		//1. distinct flag
		//2. order by + top K
		//3. balance table data (shuffle)
		
		if(prevOp.entries.length == 1){
			//no need shuffle, add possible distinct flag & order-by top k into prevClause
			prevClause.distinct = qb.distinct;
			prevClause.orderbyClause = Planner.genOrderbyList(qb);
			prevOp.fetchSQL = prevClause.toString();
		}
		else{
			if(qb.shuffleCnt != -1 || qb.distinct || qb.orderby != null){
				
				if(qb.orderby != null)
					qb.shuffleCnt = 1;
				if(qb.shuffleCnt == -1)
					qb.shuffleCnt = 1;
				
				ShuffleOperator so = new ShuffleOperator();
				so.src = prevOp;
				so.tmpTabList = new ArrayList<String>();
				so.tmpTabList.add(so.src.schema.name);
				so.entries = Meta.randomEntries(qb.shuffleCnt);
				so.schema = (Schema)so.src.schema.clone();
				so.schema.name = this.genTmpTableName(qb, obIdx);
	
				// shuffle to N node
				// phase 1. add [distinct] & order-by top k into prevClause
				prevClause.distinct = qb.distinct;
				prevClause.orderbyClause = Planner.genOrderbyList(qb);
				prevOp.fetchSQL = prevClause.toString();
				String[] col = prevOp.schema.col;
				
				String cl2 = "select ";
				if(qb.distinct)
					cl2 += "distinct ";
				for(int i = 0; i < col.length; ++i){
					if(i > 0)
						cl2 += ", ";
					cl2 += col[i];
				}
				cl2 += (" from " + prevOp.schema.name);
	
				// phase 2. collect data into one node and add [distinct] & order-by
				// top k again
				if (prevClause.orderbyClause != null) {
					cl2 += (" order by " + prevClause.orderbyClause);
				}
				so.fetchSQL = cl2;
				if( qb.distinct && qb.shuffleCnt != 1 ){
					so.distinctShuffle = true;
				}
				if( !qb.distinct && qb.orderby == null ){
					so.schema.name = so.src.schema.name = qb.schema.name;
				}
				prevOp = so;
				ops.add(so);
			}
		}
		//ops.remove(0);//remove first fetch-data operator
		return ops.toArray(new FetchDataOperator[0]);
	}
	/*
	static String genSh2SelClause(String tab, String[] col, boolean distinct){
		String s = "select ";
		if(distinct)
			s += "distinct ";
		for(int i = 0; i < col.length; ++i){
			if(i > 0)
				s += ", ";
			s += col[i];
		}
		s += (" from " + tab);
		return s;
	}*/
	/*
	static Schema genTabSchema(QB qb, String name, ArrayList<String> cols, boolean withID){
		Schema s = new Schema();
		s.name = name;
		s.col = ((ArrayList<String>)cols.clone()).toArray(new String[0]);

		s.type = new String[s.col.length];
		for(int k = 0; k < s.col.length; ++k){
			s.type[k] = getColumnType(qb, s.col[k], null, null);
		}
		for(String c : s.col)
			c.replace('.', '$');
		s.containID = withID;
		return s;
	}
	*/
	
	static void pushDownPredsByCover(ArrayList<RootExp> from, ArrayList<RootExp> to, String[] srcs){
		Iterator<RootExp> iter = from.iterator();
		while(iter.hasNext()){
			RootExp e = iter.next();
			ArrayList<String> cvs = Planner.getCoverCol(e);
			if(Planner.checkCanPushDown(srcs, cvs)){
				to.add(e);
				iter.remove();
			}
		}
	}
	
	static ArrayList<RootExp> genJoinRhsPred(JoinType joinType, 
			ArrayList<RootExp> joinCond, String[] rhsSrcs, ArrayList<RootExp> where){
		ArrayList<RootExp> to = new ArrayList<RootExp>();
		if(joinType == JoinType.INNER || joinType == JoinType.RIGHT){
			Planner.pushDownPredsByCover(where, to, rhsSrcs);
		}
		if(joinCond != null && joinType == JoinType.LEFT){
			Planner.pushDownPredsByCover(joinCond, to, rhsSrcs);
		}
		return to;
	}
	
	static String genPredsStr(ArrayList<RootExp> preds, String[] rawStr){
		ZColRef.rawSrc = rawStr;
		try{
			if(preds != null && preds.size() > 0){
				String s = "";
				for(int i = 0; i < preds.size(); ++i){
					if(i > 0)
						s += " and ";
					s += preds.get(i).toString();
				}
				return s;
			}
		return null;
		}finally{
			ZColRef.rawSrc = null;
		}
	}
	
	static class QBClause implements Cloneable{
		
		public Object clone()
		{
			try{return super.clone();}catch(CloneNotSupportedException e){return null;}
		}
		
		public String toString(){
			String dist = distinct ? " distinct" : "";
			String t = String.format("select%s %s from %s", dist, fields, fromClause);
			if(whereClause != null)
				t += (" where " + whereClause);
			if(groupbyClause != null)
				t += (" group by " + groupbyClause);
			if(havingClause != null)
				t += (" having " + havingClause);
			if(orderbyClause != null)
				t += (" order by " + orderbyClause);
			//!!
			//add second aggr code.
			
			return t;
		}
		
		String fields;
		String fromClause;
		String[] rawSrcs;//when Column ref tab not in this array, then translate into tab$column 
		String whereClause;
		ArrayList<String> nonRelSubQVar;
		Schema schema;
		
		int aggrLevel;// = 0, 1, //2
		
		String groupbyClause;
		String havingClause;
		
		//String groupbyClause2;
		//String havingClause2;
		
		boolean distinct = false;
		String orderbyClause;
		
	}
	
	QBClause genSelectClause(QB qb, int stepIdx,
			String[] src, String[] srcPhy, 
			RelSubQuery rsq, //if Inner-Q, this RSQ used for generate middle expr's type(need check ref's srcPhy)
			JoinType joinType, 
			ArrayList<RootExp> joinCond,
			ArrayList<RootExp> where, 
			ArrayList<String> cols,
			String[] midSrc,
			boolean withID
			){
		
		QBClause qbc = new QBClause();
		
		if(src == null)
			src = srcPhy = new String[0];
		qbc.rawSrcs = src.clone();
		
		Schema s = new Schema();
		s.name = this.genTmpTableName(qb, stepIdx);
		s.col = new String[cols.size()];

		s.type = new String[cols.size()];
		
		s.containID = withID;
		
		qbc.fields = "";
		
		for(int i = 0; i < s.col.length; ++i){
			if(i > 0) qbc.fields += ", ";
			
			String col = cols.get(i);
			String fc;
			String alias = col.replace('.', '$');
			if(Util.findStr(QBParser.getColSrc(col), src) != -1){
				fc = col + " as " + alias;
			}
			else fc = alias;
			
			qbc.fields += fc;
			s.col[i] = alias;
			s.type[i] = getColumnType(qb, col, null, null);
		}
		
		qbc.schema = s;
		if(withID)
			qbc.fields += ", id";
		
		//generate from clause
		//midSrc 1/2 srcs. outer-join
		if(midSrc == null)
			midSrc = new String[0];

		
		String[] jsrc = new String[midSrc.length + src.length];
		String[] jsrcPhy = new String[jsrc.length];
		
		for(int i = 0; i < jsrc.length; ++i){
			if(i < midSrc.length){
				jsrc[i] = jsrcPhy[i] = midSrc[i];
			}
			else{
				jsrc[i] = src[i - midSrc.length];
				jsrcPhy[i] = srcPhy[i - midSrc.length];
			}
		}
		
		if(joinType == JoinType.INNER || jsrc.length == 1){
			qbc.fromClause = "";
			for(int i = 0; i < jsrc.length; ++i){
				if(i > 0) qbc.fromClause += ", ";
				qbc.fromClause += genFromItem(jsrc[i], jsrcPhy[i]);
			}
		}
		else{
			qbc.fromClause = 
				genFromItem(jsrc[0], jsrcPhy[0])
			+	(joinType == JoinType.LEFT? " left join " :  " right join ")
			+	genFromItem(jsrc[1], jsrcPhy[1]);
			if(joinCond != null && joinCond.size() > 0){
				qbc.fromClause += " on ";
				qbc.fromClause += Planner.genPredsStr(joinCond, qbc.rawSrcs);
			}
		}
		qbc.whereClause = Planner.genPredsStr(where, qbc.rawSrcs);
		qbc.aggrLevel = 0;
		
		return qbc;
	}
	
	static String genFromItem(String src, String srcPhy){
		if(src.equals(srcPhy))
			return src;
		return srcPhy + " as " + src;
	}
	
	
	void fillClauseSelExp(QB qb, int stepIdx, QBClause qbc, String[] rawSrcs){
		String fields = "";
		ZColRef.rawSrc = rawSrcs;
		for(int i = 0; i < qb.selList.length; ++i){
			if(i > 0)
				fields += ", ";
			fields += ( qb.selList[i].toString() + " as " + qb.schema.col[i] );
		}
		ZColRef.rawSrc = null;
		qbc.fields = fields;
		
		Schema s = (Schema)qb.schema.clone();
		s.name = this.genTmpTableName(qb, stepIdx);
		s.containID = false;
		
		qbc.schema = s;		
	}
	
	QBClause genSelExpClause(QB qb, int stepIdx, QBClause prevClause){
		
		QBClause qbc = (QBClause)prevClause.clone();
		fillClauseSelExp(qb, stepIdx, qbc, prevClause.rawSrcs);
		
		return qbc;
	}
	
	static String genGroupbyList(QB qb, String[] rawSrcs){
		String s = null;
		if (qb.groupby.length != 0) {
			s = "";
			ZColRef.rawSrc = rawSrcs;
			for (int i = 0; i < qb.groupby.length; ++i) {
				if (i > 0)
					s += ", ";
				s += qb.groupby[i].toString();
			}
			ZColRef.rawSrc = null;
		}
		return s;
	}
	
	static String genOrderbyList(QB qb){
		String s = null;
		if (qb.orderby != null) {
			s = "";
			
			for (int i = 0; i < qb.orderby.length; ++i) {
				if (i > 0)
					s += ", ";
				s += qb.orderby[i];
				if(qb.orderbyAsc[i])
					s += " asc";
				else
					s += " desc";
			}
			s += ( " limit " + qb.topK );
		}
		
		return s;
	}
	
	QBClause genAggrClause(QB qb, int stepIdx, QBClause prevClause, 
			RelSubQuery rsq, //if Inner-Q, this RSQ used for generate middle expr's type(need check ref's srcPhy)
			ArrayList<String> selList, String[] groupby, ArrayList<RootExp> having,
			boolean withID){
		
		QBClause qbc = genSelExpClause(qb, stepIdx, prevClause);
		
		qbc.aggrLevel = 1;
		qbc.groupbyClause = genGroupbyList(qb, prevClause.rawSrcs);

		if (qb.havingPreds != null && qb.havingPreds.size() > 0) {
			qbc.havingClause = Planner.genPredsStr(qb.havingPreds, qbc.rawSrcs);
		}
		
		return qbc;
	}
	
	static boolean canPreAggr = true;
	void gen2PhaseAggrClause(QB qb, int stepIdx, QBClause prevClause, 
			RelSubQuery rsq, //if Inner-Q, this RSQ used for generate middle expr's type(need check ref's srcPhy)
			ArrayList<String> selList, String[] groupby, ArrayList<RootExp> having,
			boolean withID,
			QBClause[] qbClauses
			){
		//!!!
		//if selList == NULL, then generate QB self's aggr operator
		
		QBClause qbc1 = (QBClause)prevClause.clone();
		
		qbc1.schema.name = this.genTmpTableName(qb, stepIdx) + "_agglv1";
		
		QBClause qbc2 = new QBClause();
		qbc2.fromClause = qbc1.schema.name;
		
		qbClauses[0] = qbc1;
		qbClauses[1] = qbc2;
		/*
		canPreAggr = true;
		for(int i = 0; i < qb.selList.length; ++i){
			RootExp re = qb.selList[i];
			try{
				re.traverse(new NodeVisitor(){
					public void visit(ZExp node, RootExp root)
							throws ExecException 
					{
						if(node instanceof ZExpression && ((ZExpression)node).isAggr()){
							ZExpression e = (ZExpression)node;
							if(e.type == ZExpression.AGGR_DISTINCT
								|| FunctionMgr.getAggrMerger(e.funcOrAggrName) == null)
								canPreAggr = false;
						}
					}
				});
			}catch(ExecException e){}			
		}
		if(!canPreAggr){
		*/
			this.fillClauseSelExp(qb, stepIdx, qbc2, new String[0]);
			
			qbc2.aggrLevel = 1;
			qbc2.groupbyClause = genGroupbyList(qb, new String[0]);

			if (qb.havingPreds != null && qb.havingPreds.size() > 0) {
				qbc2.havingClause = Planner.genPredsStr(qb.havingPreds, new String[0]);
			}
		/*	
		}
		else{
			for(int i = 0; i < qb.selList.length; ++i){
				final ArrayList<RootExp> lv1 = new ArrayList<RootExp>();//col name: $aggr0,$aggr1,...
				final ArrayList<String> lv1type = new ArrayList<String>();
				RootExp re = qb.selList[i];
				try{
					re.traverse(new NodeVisitor(){
						public void visit(ZExp node, RootExp root)throws ExecException{
							if(node instanceof ZExpression && ((ZExpression)node).isAggr()){
								ZExpression e = (ZExpression)node;
								int id = lv1.size();
								String colName = "$aggrd" + id;
								ZExp parExp = e.parentExp;
								
								String mergefun = FunctionMgr.getAggrMerger(e.funcOrAggrName);
								ZExpression merger = new ZExpression(mergefun, new ZColRef(null, colName), false);
								
								parExp.replaceSubExp(e, merger);
								
								lv1.add(new RootExp(e));
								lv1type.add(e.valType);
							}
						}
					});
				}catch(ExecException e){}
			}		
			//qbc1: add group by list & lv1 list into select items. + group-by-clause
		}
		*/
	}
	static String[] checkEqPred(RootExp pred, String[] rhsSrc){
		if(! (pred.exp instanceof ZExpression))
			return null;
		ZExpression e = (ZExpression)pred.exp;
		if(e.getOperator() != Operator.EQ && e.getOperator() != Operator.SAFE_EQ)
			return null;
		
		if( ! (e.getOperand(0) instanceof ZColRef) || ! (e.getOperand(1) instanceof ZColRef))
			return null;
		
		ZColRef[] cr = new ZColRef[]{ (ZColRef)e.getOperand(0), (ZColRef)e.getOperand(1) };
		String[] t = new String[2];
		int cnt = 0;
		for(int i = 0; i < 2; ++i){
			if(Util.findStr(cr[i].table, rhsSrc) != -1)
				++cnt;
			t[i] = cr[i].toString().replace('.', '$');
		}
		if(cnt != 1)
			return null;
		return t;
	}
	
	//dim1: eq-cols, dim2 eq-left,right, use tab$colName format
	static String[][] genJoinKeyIdx(String[] rhsSrc, JoinType joinType, 
			ArrayList<RootExp> joinCond,
			ArrayList<RootExp> where){
		
		String[] t;
		ArrayList<String[]> list = new ArrayList<String[]>();
		if(joinType != JoinType.INNER){
			if(joinCond != null){
				for(RootExp re : joinCond){
					t =  checkEqPred(re, rhsSrc);
					if(t != null)
						list.add(t);
				}
			}
		}
		else{
			if(where != null){
				for(RootExp re : where){
					t =  checkEqPred(re, rhsSrc);
					if(t != null)
						list.add(t);
				}
			}
		}
		return list.toArray(new String[0][]);
	}
	
	static String[] getSrcPhyEntries(QBPlan qbPlan, String srcPhy){
		QBPlan sqbp = qbPlan.getSubQBPlan(srcPhy);
		if(sqbp != null)
			return sqbp.entries;
		return Meta.getTab(srcPhy).getEntries();
	}
	
	static boolean checkSrcPhyDistribute(QBPlan qbPlan, String srcPhy){
		if(qbPlan.getSubQBPlan(srcPhy) != null)
			return true;
		return Meta.getTab(srcPhy).isDistributed();
	}
	

	static String src2PhySrc(QB qb, String src, String[] extSrc, String[] extSrcPhy){
		int idx = Util.findStr(src, qb.src);
		if(idx != -1)
			return qb.srcPhy[idx];
		if(extSrc != null){
			idx = Util.findStr(src, extSrc);
			if(idx != -1)
				return extSrcPhy[idx];
		}
		return null;
	}

	
	static String getColumnType(QB qb, String col, String[] extSrc, String[] extSrcPhy){
		String src = QBParser.getColSrc(col);
		String colName = QBParser.getColName(col);
		
		String tab = Planner.src2PhySrc(qb, src, extSrc, extSrcPhy);
		//!!!
		//process temp table#
		QB pqb = qb.prevQBs.get(tab);
		if(pqb != null){
			int idx = Util.findStr(colName, pqb.schema.col);
			return pqb.schema.type[idx];
		}
		return Meta.getTab(tab).getCol(colName).getType();
	}
}
