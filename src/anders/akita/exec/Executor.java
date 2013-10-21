package anders.akita.exec;

/**
 * 

check from item, alias, repeat, subQ need alias

fill outer/parent query

inner-query can't contain subQuery/innerQuery,and group by, order by, limit ,
inner-query distinct move into aggr-function.
inner-query 's rel-condition
inner-query fields-list cond:
	1. exists => *
	2. all/any/in => 1 expr
	3. noraml => with aggregation
	
expand * , tab.*
check col repeat
fill col'tab,check alias, may ref outer-query's col
if alias exit, then parent-query ref it must use alias(field list),but self-query not need.

check aggr function in select list, 
if no group-by-claus, then add one(total)

group-by /having/ order-by can ref self-select-list' alias, but where condition can't 
check group by list, having expr and order by list, can ref alias in select list
re-check select list and having expr: if exist aggr-fun, ref group-by list OR aggr-Expr
re-check order by list, if exist aggr, then...

corelate-subquery add distinct-ID...

col in ( select icol)      exists ( select * where col = icol )    0 < (select count(*) where ...)

col > ALL()     col > (  select max() ...) //if no corelated, transform to sub-Q
col > ANY()     col > (  select min() ...) //if no corelated, transform to sub-Q
col != ALL      col not in(..)
col = ANY       col in(..)
col = ALL       col = max() && col = min()    not exists ( select * where col != icol)  
col != ANY      exists ( select * where col != icol)


Annotation

join:
@map
@reduce(reducerNumber/reducerProportion)

aggregate:
@hash(reducerNumber/reducerProportion)

@balance(splitNumber/splitProportion)
@hash(reducerNumber/reducerProportion,key-list)


 */

import anders.akita.parser.*;

import java.util.*;

public class Executor {
	

	ZQuery query;
	
	public Executor(ZQuery query){
		this.query = query;
		
		//syntax check
	}
	
	static public enum ExprType { SELECT, WHERE, GROUPBY, HAVING, ORDERBY, SELECT_INNER, WHERE_INNER,  };
	
	interface ExprCallback{
		public void handleColRef(ZQuery q, ZColRef colRef, ExprType type);
		public void handleAggr(ZQuery q, ZExpression aggr, int depth, ExprType type);
	}
	interface InnerQueryCallback{
		public void handleInnerQuery(ZQuery q, ZQuery innerQ, ExprType type);		
	}
	
	ZExp expRoot(ZExp e){
		while(e.parentExp != null)
			e = e.parentExp;
		return e;
	}
	
	void splitExprByAND(ZExp exp, ArrayList<ZExp> list){
		if(
			exp instanceof ZExpression
			&& ((ZExpression)exp).type == ZExpression.OPERATOR
			&& ((ZExpression)exp).getOperator() == Operator.AND
		
		){
			for(ZExp sube: ((ZExpression)exp).getOperands()){
				splitExprByAND(sube, list);
			}
		}
		else{
			list.add(exp);
		}
	}

	static final ExprCallback exprCb = new ExprCallback(){
		
		public void handleAggr(ZQuery q, ZExpression aggr, int depth, ExprType type){
			String errStr = "Illegal use aggreation function: " + aggr.toString() + " in query: " + q.toString();
			switch(type){
			case SELECT:
			case HAVING:
			case ORDERBY:
				if(depth > 0)
					throw new ExecException(errStr);
				break;
			case WHERE:
			case GROUPBY:
				throw new ExecException(errStr);
			case SELECT_INNER:
				break;
			case WHERE_INNER:
				break;
			}
		}
		
		public void handleColRef(ZQuery q, ZColRef colRef, ExprType type){
			
			colRef.query = q;
			
			if(colRef.table != null){
				ZFromItemEx tab = q.tabList.get(colRef.table);
				if(tab == null || !tab.existField(colRef.col))
					throw new ExecException("Illegal select item: " + colRef.toString());
			}
			else{
				//alias check first!!
				boolean findAlias = false;
				//if(useAlias){
					if(q.fieldList.containsKey(colRef.col))
						findAlias = true;
				//}
				if(!findAlias){
					for(String key: q.tabList.keySet()){
						ZFromItemEx tab = q.tabList.get(key);
						if(tab.existField(colRef.col)){
							if(colRef.table == null)
								colRef.table = key;
							else
								throw new ExecException("Illegal select item: " + colRef.toString());
						}
					}
				}
			}

		}

	};
	
	
	static final InnerQueryCallback innerQCb = new InnerQueryCallback(){
		
		public void handleInnerQuery(ZQuery q, ZQuery innerQ, ExprType type){
			if(type == ExprType.WHERE_INNER || type == ExprType.SELECT_INNER)
				throw new ExecException("Inner-Query cannot be nested: " + q.toString());
			innerQ.outerQuery = q;
			switch(type){
			case WHERE:
				innerQ.inWhereClause = true;
				q.innerQinWhereList.add(innerQ);
				break;
			case HAVING:
				innerQ.inWhereClause = false;
				q.innerQinHavingList.add(innerQ);
				break;
			default:
				throw new ExecException("Illegal use of inner sub-Query: " + innerQ.toString());
			}
			
			//check syntax for inner-Query
			
			//1. check & build from item list of inner-query
			if(q.getFrom().join_type != ZFromClause.INNER_JOIN)
				throw new ExecException("inner sub-Query not support OUTER-JOIN: " + innerQ.toString());
			
			for(ZFromItemEx item: innerQ.getFrom().items){
				if(item.isSubQuery())
					throw new ExecException("For inner query, sub-query in from clause is not supported: " + item.toString());
				
				String alias = item.alias;
				if(alias == null)alias = item.table;
				if(innerQ.tabList.get(alias) != null)
					throw new ExecException("alias/table name duplicate: " + alias);
				innerQ.tabList.put(alias, item);
			}
			//2. iterate select expr
			exprIter(q, q.getSelect().get(0).expr, ExprType.SELECT_INNER, exprCb, innerQCb);
			
			//3. iterate where expr
			
		}
	};
	
	
	
	void buildQueryTree(final ZQuery q){
		
		//!!!
		//COUNT(*) ==> COUNT(1)
		
		//!!!!
		//!!!
		/*
		 * now order by must ref only field list
		 * & group by, having can't ref alias
		 */
		

		//if(q.getGroupBy() != null){
		//	q.groupByKey.addAll(q.getGroupBy().getGroupBy());
		//}

		//check where, group-by, having, order by, split AND operator
		
		q.needAggr = false;
		
		if(q.getGroupBy() != null){
			q.needAggr = true;
			q.groupByKey = new ArrayList<ZExp>(q.getGroupBy().getGroupBy());
			//q.groupByKey.a
		}
		//check select list to find it whether exist AGGREGATION-function
		if(!q.needAggr){
			for(ZSelectItem item: q.getSelect()){
				if(item.type == ZSelectItem.EXPR){
					exprIter(q, item.expr, ExprType.SELECT, new ExprCallback(){
						public void handleAggr(ZQuery q, ZExpression aggr, int depth, ExprType type){
							q.needAggr = true;
						}
						public void handleColRef(ZQuery q, ZColRef colRef, ExprType type){}
					}, null);
				}
			}
		}
		
		if(q.getWhere() != null)
			splitExprByAND(q.getWhere(), q.whereList);
		if(q.getGroupBy() != null && q.getGroupBy().getHaving() != null)
			splitExprByAND(q.getGroupBy().getHaving(), q.getGroupBy().havingList);
		
		//construct from tab list: tab-alias=>FromItem(table or subQuery)
		for(ZFromItemEx item: q.getFrom().items){
			if(item.isSubQuery()){
				if(item.alias == null)
					throw new ExecException("subQuery must has an alias: " + item.toString());
				ZQuery subQ = item.getSubQuery();
				subQ.parentQuery = q;
				buildQueryTree(subQ);
			}
			
			String alias = item.alias;
			if(alias == null)alias = item.table;
			if(q.tabList.get(alias) != null)
				throw new ExecException("alias/table name duplicate: " + alias);
			q.tabList.put(alias, item);
		}
		
		//check select list and construct field list for the query
		
		//1. expand * and TAB_NAME.* into ZColRef struct
		Vector<ZSelectItem> newlist = new Vector<ZSelectItem>();
		
		for(ZSelectItem item: q.getSelect()){
			if(item.type == ZSelectItem.STAR){
				for(String key: q.tabList.keySet()){
					ZFromItemEx tab = q.tabList.get(key);
					for(String field: tab.getFieldList()){
						ZColRef cr = new ZColRef(key, field);
						newlist.add(new ZSelectItem(cr, null));
					}
				}
			}
			else if(item.type == ZSelectItem.TAB_DOT_STAR){
				if(q.tabList.get(item.table) == null)
					throw new ExecException("Illegal select item: " + item.table + ".*");
				for(String field: q.tabList.get(item.table).getFieldList()){
					ZColRef cr = new ZColRef(item.table, field);
					newlist.add(new ZSelectItem(cr, null));					
				}
			}
			else newlist.add(item);
		}
		q.setSelect(newlist);
		
		
		// 2. check tab column ref & subQuery in select list
		for(ZSelectItem item: q.getSelect()){
			exprIter(q, item.expr, ExprType.SELECT, exprCb, innerQCb);
		}
		
		// 3. construct query's field list
		
		for(ZSelectItem item: q.getSelect()){
			String alias = item.alias;
			if(alias == null && item.expr instanceof ZColRef){
				alias = ((ZColRef)item.expr).col;
			}
			if(alias != null){
				if(q.fieldList.get(alias) != null)
					throw new ExecException("duplicated select column/alias: " + alias);
				q.fieldList.put(alias, item);
			}
		}
		
		

	}
	
	//fill table/alias in FromClause->FromItem-Vector
	void pass1_fillTabAliasName(ZQuery q){
		ZFromClause fc = q.getFrom();
		
		for(int i = 0; i < fc.getItemN(); ++i){
			ZFromItemEx fi = fc.getItem(i);
			if(fi.isSubQuery()){
				fi.table = null;
				//fi.alias = fi.getSubQuery().getAlias();
				pass1_fillTabAliasName(fi.getSubQuery());
			}
			else{
				//fi.table = fi.getFromItem().getTable();
				//fi.alias = fi.getFromItem().getAlias();
				if(fi.alias == null)
					fi.alias = fi.table;
			}
		}
	}

	void exprListIter(ZQuery q, Collection<ZExp> exprList, ExprType type, ExprCallback exprCb, InnerQueryCallback subQueryCb){
		for(ZExp e: exprList){
			exprIter(q, e, type, exprCb, subQueryCb);
		}
	}
	
	//iterator of Columne in Expr
	static void exprIter(ZQuery q, ZExp expr, ExprType type, ExprCallback exprCb, InnerQueryCallback subQueryCb){
		exprIter(q, expr, type, 0, exprCb, subQueryCb);
	}
	static void exprIter(ZQuery q, ZExp expr, ExprType type, int aggrDepth, ExprCallback exprCb, InnerQueryCallback subQueryCb){
		if(expr == null)
			throw new ExecException("Internal error #exprIter");
		
		if(expr instanceof ZExpression){
			ZExpression e = (ZExpression)expr;
			int ad = aggrDepth;
			if(e.type == ZExpression.AGGR_ALL || e.type == ZExpression.AGGR_DISTINCT){
				//it is an aggregation function
				++ad;
			}
			for(ZExp sube: e.subExpSet()){
				sube.parentExp = expr;
				exprIter(q, sube, type, ad, exprCb, subQueryCb);
			}
			exprCb.handleAggr(q, e, aggrDepth, type);
		}
		else if(expr instanceof ZColRef){
			ZColRef c = (ZColRef)expr;
			if(exprCb != null)exprCb.handleColRef(q, c, type);
		}
		else if(expr instanceof ZQuery){
			if(subQueryCb != null)subQueryCb.handleInnerQuery(q, (ZQuery)expr, type);
		}
		else{
			for(ZExp sube: expr.subExpSet()){
				sube.parentExp = expr;
				exprIter(q, sube, type, aggrDepth, exprCb, subQueryCb);
			}
		}
	}

	ArrayList<ZColRef> getRefColList(RootExp[] expList){
		
	}
	ArrayList<ZColRef> getRefColList(ArrayList<RootExp> list){
		return getRefColList(list.toArray(new RootExp[0]));
	}
	ArrayList<ZColRef> mergeRefColList(ArrayList<ZColRef> ... subLists){
		
	}
	
	ArrayList<RootExp> filterExprBy(ArrayList<RootExp> exp, ArrayList<String> set){
		
	}
	ArrayList<ZColRef> filterColBy(ArrayList<ZColRef> list, ArrayList<String> set){
		
	}
	
	ArrayList<RootExp> bridgeExprBy(ArrayList<RootExp> exp, ArrayList<String> set1, ArrayList<String> set2){
		
	}
	
	ArrayList<RootExp> minusExpSet(ArrayList<RootExp> set, ArrayList<RootExp> ... list){
		
	}
	ArrayList<RootExp> mergeExprList(ArrayList<RootExp> ... subLists){
		
	}
	/**
	 * direct-shuffle: 
	 * 	1. group-by list, ex-list, to-aggr-expr list shuffle by group-by list
	 * 	2. aggr, having, out: EXPR-LIST(group-by list, ex-list, aggr-expr list)
	 * 
	 * already hash by keys:
	 * 	x. aggr, having, out: EXPR-LIST(group-by list, ex-list, aggr-expr list)
	 * 
	 * pre-aggr:
	 * 	1. in: group-by list, ex-list, to-aggr-expr list, pack with aggr shuffle by group-by list, 
	 * 	   out: group-by list, ex-list, aggr-expr list
	 * 	2. replace aggr exp with corresponding merger-function
	 *     out: EXPR-LIST(group-by list, ex-list, aggr-expr list)
	 */	
	MidResult execAggrOrSelect(SubQueryBlock subQB){
		
		if(subQB.aggrDesc == null){
			ArrayList<ZColRef> cols = getRefColList(subQB.selectList);
			MidResult mr = execQBFetchOrJoin(subQB, subQB.join.joinItems.length, cols);
			
			mr.selectAlias = subQB.selectAlias;
			mr.selectList = subQB.selectList;
			
			return mr;
		}
		
		//COUNT(*)?? COUNT(distinct xx,xxx,xxx)
		//MidField[] mf = new MidField[qb.aggrProc.groupBy.length
		//          + qb.aggrProc.outerTab != null? qb.aggrProc.outerTab.selectList.length, ];
		
		ArrayList<ZColRef> colsForAg = getRefColList(subQB.aggrDesc.preAggrExprs);
		
		if(subQB.aggrDesc.isRelSubQuery){
			ArrayList<ZColRef> tmp = new ArrayList<ZColRef>();
			tmp.add(new ZColRef(subQB.join.joinItems[0].table.alias(), AggrDesc.REL_SUB_QB_ID));
			tmp.addAll(subQB.colRefsForRelSubQ);
			
			colsForAg = mergeRefColList(colsForAg, tmp);
		}
		else{
			
			colsForAg = mergeRefColList(colsForAg, getRefColList(subQB.aggrDesc.groupBy));
		}
		MidResult mr = execQBFetchOrJoin(subQB, subQB.join.joinChain, colsForAg, subQB.join.wherePreds);
		
		if(/*hashed by key or 1 fragment*/){
			return packAggr(subQB, mr, 1);
		}
		else if(/*direct-shuffle condition*/){
			MidResult mr2 = new MidResult();
			
			// TODO execute mr and shuffle into reducer node
			
			mr2.alias = XXX;
			mr2.jd = new JoinDesc();
			mr2.jd.joinItems = XXX;
			
			mr2.midQBTabList = XXX;
			
			return packAggr(subQB, mr2, 2);
		}
		else{
			/*
			 * two phase aggregation:
			 * 	1. pack phase-1 aggregation into MidResult
			 * 	2. shuffle
			 * 	3. execute phase-2 aggregation & having clause
			 */
			
			ArrayList<RootExp> selList = new ArrayList<RootExp>();
			ArrayList<String> selAlias = new ArrayList<String>();
			RootExp[] groupby = null;
			
			for(int i = 0; i < subQB.aggrDesc.preAggrExprs.length; ++i){
				selList.add(subQB.aggrDesc.expandAggrPhase1(i));
				selAlias.add(AggrDesc.genAggrPh1Alias(i));
			}
			if(!subQB.aggrDesc.isRelSubQuery){
				groupby = subQB.aggrDesc.groupBy;
			}
			mr.selectList = selList.toArray(new RootExp[0]);
			mr.selectAlias = selAlias.toArray(new String[0]);
			
			mr.groupby = groupby;
			mr.isRelSubQ = subQB.aggrDesc.isRelSubQuery;
			
			mr.havingPreds = new ArrayList<RootExp>();
			
			MidResult mr2 = new MidResult();
			
			// TODO shuffle here
			
			mr2.alias = XXX;
			mr2.jd = new JoinDesc();
			mr2.jd.joinItems = XXX;
			
			mr2.midQBTabList = XXX;
			
			return packAggr(subQB, mr2, 2);
		}
		
	}
	
	
	MidResult packAggr(SubQueryBlock subQB, MidResult mr, int phase){
		// pack aggregation + having into MidResult
		for(RootExp re: subQB.selectList){
			if(phase == 1)subQB.aggrDesc.expandPreAggrCol(re);
			else if(phase == 2)subQB.aggrDesc.expandAggrPhase2(re);
		}
		for(RootExp re: subQB.aggrDesc.havingPreds){
			if(phase == 1)subQB.aggrDesc.expandPreAggrCol(re);
			else if(phase == 2)subQB.aggrDesc.expandAggrPhase2(re);
		}
		mr.selectList = subQB.selectList;
		mr.selectAlias = subQB.selectAlias;
		mr.groupby = subQB.aggrDesc.groupBy;
		mr.havingPreds = subQB.aggrDesc.havingPreds;
		
		mr.isRelSubQ = subQB.aggrDesc.isRelSubQuery;
		
		return mr;
	}
	
	
	
	MidResult execQBFetchOrJoin(SubQueryBlock subQB, JoinChain jChain, ArrayList<ZColRef> fields, ArrayList<RootExp> filters){

		
		if(jChain.prev == null){
			//first node
			MidResult mr = new MidResult();
			mr.fetchList = fields;
			mr.joinType = jChain.join_type;
			mr.joinCond = jChain.joinConds;
			mr.joinItems = new ITable[jChain.fromTabs.size()];
			for(int i = 0; i < mr.joinItems.length; ++i)
				mr.joinItems[i] = jChain.fromTabs.get(i);
			
			mr.alias = xxx;
			mr.entries = xxx;
			
			mr.midQBTabList = mr.joinItems[0].embeddedAlias();
			mr.wherePreds = filters;
			
			return mr;
		}

		//ArrayList<ZColRef> allFields = mergeRefColList(fields, getRefColList(filters.toArray(new RootExp[0])));
		//if(jChain.joinConds != null)
		//	allFields = mergeRefColList(allFields, getRefColList(jChain.joinConds.toArray(new RootExp[0])));
		
		ArrayList<String> set1, set2;
		set1 = xxx;
		set2 = xxx;
		set3 = xxx;
		
		ArrayList<RootExp> leftFilters = this.filterExprBy(filters, set1);
		ArrayList<RootExp> bridgeFilters = this.bridgeExprBy(filters, set1, set2);
		ArrayList<RootExp> rightFilters = this.filterExprBy(filters, set2);
		
		@SuppressWarnings("unchecked")
		ArrayList<RootExp> finalFilters = this.minusExpSet(filters, leftFilters, bridgeFilters, rightFilters);
		ArrayList<RootExp> joinCond = jChain.joinConds;
		//ArrayList<ZColRef> leftFields = this.filterColBy(allFields, set1);
		//@SuppressWarnings("unchecked")

		//For outer join, pushdown join-conds
		if(jChain.join_type == ZFromClause.LEFT_JOIN){
			ArrayList<RootExp> pushPreds = this.filterExprBy(joinCond, set2);
			//@SuppressWarnings("unchecked")
			joinCond = this.minusExpSet(joinCond, pushPreds);
			rightFilters.addAll(pushPreds);
		}
		else if(jChain.join_type == ZFromClause.RIGHT_JOIN){
			ArrayList<RootExp> pushPreds = this.filterExprBy(joinCond, set1);
			joinCond = this.minusExpSet(joinCond, pushPreds);
			leftFilters.addAll(pushPreds);			
		}
		@SuppressWarnings("unchecked")
		ArrayList<ZColRef> tmp1 = this.mergeRefColList(
				fields, this.getRefColList(
					this.mergeExprList(joinCond, bridgeFilters, finalFilters)));
		
		ArrayList<ZColRef> leftFields = this.filterColBy(tmp1, set1);
		ArrayList<ZColRef> rightFields = this.filterColBy(tmp1, set2);
		
		ArrayList<String> setLR = new ArrayList<String>(set1);
		setLR.addAll(set2);
		ArrayList<ZColRef> midFields = this.filterColBy(
				this.mergeRefColList(fields, this.getRefColList(finalFilters)), setLR);
		
		MidResult mr = execQBFetchOrJoin(subQB, jChain.prev, leftFields, leftFilters);
				
		if(jChain.optType == JoinItem.MAP_JOIN){
			
		}
		else if(jChain.optType == JoinItem.REDUCE_JOIN){
			
		}
		else throw new ExecException("Internal error #Executor.execQBFetchOrJoin");
	}
	
	MidResult execQB(QueryBlock qb){
		
	}
	
	
	/*
	 * join + where..
	 */
	MidResult execSel(){
		
	}
	
	MidResult execAggr(){
		
		MidResult r = execSel();
		
		if(){
			
			
		}
		
		return r;
	}
	
	/*
	 * balance/hash/sort+topK + [distinct]
	 */
	MidResult execShuffle(boolean isRootQ){
		
		if(isRootQ){
			//add @balance(1) if needed
			//set result location
		}
		
		MidResult r = execAggr();
		
		//check 
		if(/**/){
			
		}
		
		return r;
	}
	
	ArrayList<Row> exec(){
		
		
		
		return null;
	}
	
}
