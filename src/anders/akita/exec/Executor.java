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

 */

import anders.akita.parser.*;

import java.util.*;

public class Executor {
	

	ZQuery query;
	
	public Executor(ZQuery query){
		this.query = query;
		
		//syntax check
	}
	

	void buildQueryTree(final ZQuery q){
		
		q.tabList = new HashMap<String, ZFromItemEx>();
		
		//construct from tab list: tab-alias=>FromItem(table or subQuery)
		for(ZFromItemEx item: q.getFrom().items){
			if(item.isSubQuery()){
				if(item.alias == null)
					throw new ExecException("subQuery must has an alias: " + item.toString());
				ZQuery subQ = item.getSubQuery();
				subQ.parent = q;
				buildQueryTree(subQ);
			}
			
			String alias = item.alias;
			if(alias == null)alias = item.table;
			if(q.tabList.get(alias) != null)
				throw new ExecException("alias/table name duplicate: " + alias);
			q.tabList.put(alias, item);
		}
		
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
		
		ExprCallback exprCb = new ExprCallback(){
			public void handleColRef(ZColRef colRef, boolean useAlias){
				
				colRef.query = q;
				
				if(colRef.table != null){
					ZFromItemEx tab = q.tabList.get(colRef.table);
					if(tab == null || !tab.existField(colRef.col))
						throw new ExecException("Illegal select item: " + colRef.toString());
				}
				else{
					//alias check first!!
					boolean findAlias = false;
					if(useAlias){
						if(q.fieldList.containsKey(colRef.col))
							findAlias = true;
					}
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
		
		InnerQueryCallback innerQNotAllowCb = new InnerQueryCallback(){
			public void handleInnerQuery(ZQuery innerQ, ExprType type){
				throw new ExecException("Illegal inner sub-query: " + innerQ.toString());
			}			
		};
		
		InnerQueryCallback innerQAllowCb = new InnerQueryCallback(){
			public void handleInnerQuery(ZQuery innerQ, ExprType type){
				innerQ.outer = q;
			}			
		};
		//check tab column ref & subQuery in select list
		for(ZSelectItem item: q.getSelect()){
			exprIter(item.expr, false, exprCb, innerQNotAllowCb);
		}
		//construct query's field list
		q.fieldList = new HashMap<String, ZSelectItem>();
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
	static public enum ExprType { SELECT, WHERE, GROUPBY, HAVING, ORDERBY };
	
	interface ExprCallback{
		public void handleColRef(ZColRef colRef, ExprType type);
	}
	interface InnerQueryCallback{
		public void handleInnerQuery(ZQuery innerQ, ExprType type);		
	}
	
	void exprListIter(Collection<ZExp> exprList, ExprType type, ExprCallback exprCb, InnerQueryCallback subQueryCb){
		for(ZExp e: exprList){
			exprIter(e, type, exprCb, subQueryCb);
		}
	}
	
	//iterator of Columne in Expr
	void exprIter(ZExp expr, ExprType type, ExprCallback exprCb, InnerQueryCallback subQueryCb){
		if(expr == null)
			return;
		if(expr instanceof ZExpression){
			ZExpression e = (ZExpression)expr;
			for(ZExp sube: e.getOperands()){
				exprIter(sube, type, exprCb, subQueryCb);
			}
		}
		else if(expr instanceof ZColRef){
			ZColRef c = (ZColRef)expr;
			if(exprCb != null)exprCb.handleColRef(c, type);
		}
		else if(expr instanceof ZInterval){
			ZInterval i = (ZInterval)expr;
			exprIter(i.getExpr(), type, exprCb, subQueryCb);		
		}
		else if(expr instanceof ZSwitchExpr){
			ZSwitchExpr s = (ZSwitchExpr)expr;
			for(ZExp sube: s.getCond()){
				exprIter(sube, type, exprCb, subQueryCb);
			}
			for(ZExp sube: s.getResult()){
				exprIter(sube, type, exprCb, subQueryCb);
			}
			if(s.getCmpVal() != null)
				exprIter(s.getCmpVal(), type, exprCb, subQueryCb);
			if(s.getElseResult() != null)
				exprIter(s.getElseResult(), type, exprCb, subQueryCb);		
		}
		else if(expr instanceof ZQuery){
			if(subQueryCb != null)subQueryCb.handleInnerQuery((ZQuery)expr, type);
		}
		else if(expr instanceof ZConstant){
			//do nothing
		}
		else throw new ExecException("Parse error #colInExprIter");
	}
	
	
	ArrayList<Row> exec(){
		
		return null;
	}
	
}
