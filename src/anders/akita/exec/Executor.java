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

table:
@centralize
@distribute

join:



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
			q.groupByKey.a
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
	
	
	ArrayList<Row> exec(){
		
		return null;
	}
	
}
