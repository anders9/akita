package anders.akita.plan;

import java.util.*;

import anders.akita.meta.*;
import anders.akita.parser.*;
import anders.util.*;

public class Planner {
	
	QB rootqb;
	int qid;
	
	public Planner(QB qb, int qid){
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

		plan.operators = genSubQBPlan(qb);

		return plan;
	}
	
	static class OpBase{
		ArrayList<String> fetchCol;
		int endPos;
		int rsqPos = -1;
		int rsqEndPos = -1;
		boolean genID = false;//for next step
		boolean containID = false;
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
		//String collectEntry;
		ArrayList<RootExp> where;
	}
	static class OpRelAggr extends OpBase{
		int aggrReducerN;
		//RootExp[] aggrExprs;
		//String[] groupby;//if NULL, is for relative-sub-query
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
	
	 ArrayList<String> getPredCoverSrc(RootExp pred)
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
	boolean checkCanPushDown(String[] srcs, ArrayList<String> covers){
		for(String c: covers){
			int l = Util.findStr(c, srcs);
			if(l == -1)
				return false;
		}
		return true;
	}
	 
	 
	int getPushdownLevel(String[] srcs, ArrayList<String> covers){
		int level = 0;
		for(String c: covers){
			int l = Util.findStr(c, srcs);
			if(l != -1 && l > level)
				level = l;
		}
		return level;
	}
	ArrayList<RootExp>[] genPushDownPredsArray(String[] src, ArrayList<RootExp> preds){
		ArrayList<RootExp>[] pdList = new ArrayList[src.length];
		for(int i = 0; i < src.length; ++i){
			pdList[i] = new ArrayList<RootExp>();
		}
		for(RootExp pred: preds){
			int lv = getPushdownLevel(src, getPredCoverSrc(pred));
			pdList[lv].add(pred);
		}
		return pdList;
	}
	ArrayList<RootExp> mergePredArray(ArrayList<RootExp>[] preds, int beg, int end){
		ArrayList<RootExp> list = new ArrayList<RootExp>();
		for(int i = beg; i < end; ++i){
			list.addAll(preds[i]);
		}
		return list;
	}
	
	static String genCol(String src, String col){
		return src + '.' + col;
	}
	static String getColSrc(String col){
		return col.substring(0, col.indexOf('.'));
	}
	static String getColName(String col){
		return col.substring(col.indexOf('.') + 1);
	}
	ArrayList<String> getCoverCol(RootExp exp){
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
	ArrayList<String> getCoverCol(ArrayList<RootExp> expList){
		ArrayList<String> list = new ArrayList<String>();
		for(RootExp e: expList){
			list.addAll(getCoverCol(e));
		}
		return list;
	}
	ArrayList<String> getCoverCol(RootExp[] expList){
		ArrayList<String> list = new ArrayList<String>();
		for(RootExp e: expList){
			list.addAll(getCoverCol(e));
		}
		return list;
	}
	ArrayList<String> filteCoverCol(String[] cutSrc, ArrayList<String> list){
		ArrayList<String> rlist = new ArrayList<String>();
		for(String col: list){
			String s = getColSrc(col);
			if(Util.findStr(s, cutSrc) == -1)
				rlist.add(col);
		}
		return rlist;
	}
	
	String genTmpTableName(String qbName, int step){
		return String.format("$$ts%d_%s_%d", qid, qbName, step); 
	}
	
	FetchDataOperator[] genSubQBPlan(QB qb){
		
		ArrayList<OpBase> opList = new ArrayList<OpBase>();
		
		//push predicates down
		ArrayList<RootExp>[] pdList = genPushDownPredsArray(qb.src, qb.where);
		//special process for RIGHT-JOIN
		if(qb.src.length == 2 && qb.joinType == JoinType.RIGHT){
			pdList[1].addAll(pdList[0]);
			pdList[0].clear();
		}
		//process Join condition
		if(qb.src.length == 2 && qb.joinType != JoinType.INNER && qb.joinCond != null){
			if(qb.joinType == JoinType.RIGHT){
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
		ArrayList<RootExp>[][] spdList = new ArrayList[qb.relSubQ.length][];
		for(int i = 0; i < spdList.length; ++i){
			RelSubQuery rsq = qb.relSubQ[i];
			spdList[i] = genPushDownPredsArray(rsq.src, rsq.wherePreds);
		}
		//process LEFT/RIGHT JOIN condition 
		
		
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
		ofp.where = mergePredArray(pdList, 0, pos);
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
				oj.where = mergePredArray(pdList, beg, pos);
				opList.add(oj);
			}
			
			boolean first = true;
			while(relPos < qb.relSubQ.length){
				
				if(!first && qb.relSubQ[relPos].pos != qb.relSubQ[relPos - 1].pos)
					break;
				
				RelSubQuery rsq = qb.relSubQ[relPos];
				opList.get(opList.size() - 1).genID = true;
				int k = 0;
				while(k < rsq.srcPhy.length && !Meta.getTab(rsq.srcPhy[k]).isDistributed())
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
					oj.where = mergePredArray(spdList[relPos], 0, k);
					oj.containID = true;
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
					oj.where = mergePredArray(spdList[relPos], beg, k);
					oj.containID = true;
					opList.add(oj);
				}
				//add aggregation node for Relative-Sub-query
				OpRelAggr oa = new OpRelAggr();
				oa.aggrReducerN = rsq.aggrReducerN;
				oa.havingPreds = new ArrayList<RootExp>();
				oa.havingPreds.add(rsq.havingPreds);
				oa.containID = true;
				
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
		
		/*
		 * FetchData, Join, ... Join(genID),| Join(containID), ... Aggr(containID, [genID]),| ...
		 */
		
		ArrayList<String> fcols;
		//get col set...
		fcols = getCoverCol(qb.selList);
		if(qb.groupby != null){
			ArrayList<String> t = new ArrayList<String>();
			for(ZColRef gb : qb.groupby)
				t.add(t.toString());
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
			if(ob instanceof OpRelAggr){
				OpRelAggr oa = (OpRelAggr)ob;
				fcols = Util.<String>mergeArrayList(fcols, getCoverCol(oa.havingPreds));
			}
			else if(ob instanceof OpJoin){
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
			OpBase ob = opList.get(i);
			
			FetchDataOperator fdo = null;
			
			if(ob instanceof OpFetchData){
				OpFetchData ofd = (OpFetchData)ob;
				fdo = new FetchDataOperator();
				fdo.schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i), ob.fetchCol);
				fdo.fetchSQL = Planner.genSelectClause(ofd.src, ofd.srcPhy,
						ofd.joinType,
						ofd.joinCond,
						ofd.where, ofd.fetchCol, null);
				fdo.entries = ofd.entry;
				//fdo.genID = ofd.genID;
				//fdo.tmpTabList = new ArrayList<String>();
				//fdo.tmpTabList.add(this.genTmpTableName(qb.schema.name, i));
			}
			else if(ob instanceof OpJoin){
				OpJoin oj = (OpJoin)ob;
				if(oj.joinPolicy == JoinPolicy.Mapside){
					fdo = new MapJoinOperator();
					fdo.schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i), ob.fetchCol);
					MapJoinOperator mjo = (MapJoinOperator)fdo;
					
					mjo.leftSrc = ops.get(i - 1);
					mjo.rhsEntries = oj.rhsEntry;
					if(mjo.leftSrc.entries.length == 1)
						mjo.entries = mjo.leftSrc.entries.clone();
					else
						mjo.entries = Meta.randomEntries(1);
					mjo.collectNode = mjo.entries[0];
					mjo.midTab = mjo.leftSrc.schema.name;
					
					fdo.genPrevID = oj.containID && !mjo.leftSrc.schema.containID;
					
					mjo.joinClause = Planner.genSelectClause(oj.src, oj.srcPhy, 
							oj.joinType,
							oj.joinCond,
							oj.where, oj.fetchCol, mjo.midTab,
							mjo.leftSrc.schema.containID && oj.containID
							);
					
					fdo.tmpTabList = new ArrayList<String>();
					fdo.tmpTabList.add(fdo.schema.name);
					//fdo.fetchSQL = "select * from " + fdo.schema.name;
					fdo.fetchSQL = null;
					//fdo.genID = false;
					//mjo.genMidTabID = oj.genID;
					fdo.schema.containID = oj.genID;
					
				}
				else if(oj.joinPolicy == JoinPolicy.Reduceside){
					fdo = new ReduceJoinOperator();
					fdo.schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i), ob.fetchCol);
					ReduceJoinOperator rjo = (ReduceJoinOperator)fdo;
					
					rjo.srcs = new FetchDataOperator[2];
					rjo.srcs[0] = ops.get(i - 1);
					
					ArrayList<RootExp> rhsWhere = genJoinRhsPred(oj.joinType, oj.joinCond, oj.src, oj.where);
					
					rjo.srcs[1] = new FetchDataOperator();
					//generate rhs-table column
					ArrayList<String> rhsCols = (ArrayList<String>)oj.fetchCol.clone();
					rhsCols = Util.<String>mergeArrayList(rhsCols, getCoverCol(oj.where));
					if(oj.joinCond != null)
						rhsCols = Util.<String>mergeArrayList(rhsCols, getCoverCol(oj.joinCond));
					rhsCols = filteCoverCol(oj.src, rhsCols);
					rjo.srcs[1].schema = genTabSchema(qb, this.genTmpTableName(qb.schema.name, i) + "_rhs", rhsCols);
					rjo.srcs[1].entries = oj.rhsEntry;
					rjo.srcs[1].fetchSQL = Planner.genSelectClause(oj.src, oj.srcPhy, JoinType.INNER, null, rhsWhere, rhsCols, null, false);
					
					fdo.tmpTabList = new ArrayList<String>();
					fdo.tmpTabList.add(rjo.srcs[0].schema.name);
					fdo.tmpTabList.add(rjo.srcs[1].schema.name);
					
					//rjo.joinKeyIdx
					String[][] jk = Planner.genJoinKeyIdx(oj.joinType, oj.joinCond, oj.where);
					
					int[][] jkIdx = new int[jk.length][2];
					
					for(int k = 0; k < jk.length; ++k){
						jkIdx[k][0] = Util.findStr(jk[k][0], rjo.srcs[0].schema.col);
						jkIdx[k][1] = Util.findStr(jk[k][1], rjo.srcs[1].schema.col);
					}
					rjo.joinKeyIdx = jkIdx;
					
					fdo.entries = oj.reducerEntry;
					String[] jsrc = new String[]{rjo.srcs[0].schema.name, rjo.srcs[1].schema.name};
					fdo.fetchSQL = Planner.genSelectClause(jsrc, jsrc, oj.joinType, oj.joinCond, oj.where, oj.fetchCol, null,
							
							);
					//fdo.genID = oj.genID;
				}
				else
					assert false;
			}
			else if(ob instanceof OpRelAggr){
				
			}
			
			//!!! gen ID !!!
		}
		/*
		OpRelAggr oa = new OpRelAggr();
		oa.aggrReducerN = qb.aggrReducerN;
		//oa.groupby = qb.groupby;
		oa.havingPreds = qb.havingPreds;
		//oa.aggrExprs = #;
		oa.endPos = qb.src.length;
		
		opList.add(oa);
		*/
		
		//JoinCond process..
		
		//column pruning
	}
	Schema genTabSchema(QB qb, String name, ArrayList<String> cols){
		Schema s = new Schema();
		s.name = name;
		s.col = ((ArrayList<String>)cols.clone()).toArray(new String[0]);
		for(String c : s.col)
			c.replace('.', '$');
		s.type = new String[s.col.length];
		for(int k = 0; k < s.col.length; ++k){
			s.type[k] = getColumnType(qb, s.col[k]);
		}
		s.containID = false;
		return s;
	}
	ArrayList<RootExp> genJoinRhsPred(JoinType joinType, ArrayList<RootExp> joinCond, String[] rhsSrcs, ArrayList<RootExp> where){
		
	}
	
	
	static String genSelectClause(String[] src, String[] srcPhy, 
			JoinType joinType, 
			ArrayList<RootExp> joinCond,
			ArrayList<RootExp> where, 
			ArrayList<String> cols,
			String prevSrc,
			boolean genID
			){
		
	}
	
	static String[][] genJoinKeyIdx(JoinType joinType, 
			ArrayList<RootExp> joinCond,
			ArrayList<RootExp> where){
		
	}
	
	static String src2PhySrc(QB qb, String src){
		int idx = Util.findStr(src, qb.src);
		if(idx != -1)
			return qb.srcPhy[idx];
		return null;
	}
	
	static String getColumnType(QB qb, String col){
		String src = Planner.getColSrc(col);
		String colName = Planner.getColName(col);
		
		String tab = Planner.src2PhySrc(qb, src);
		
		
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
