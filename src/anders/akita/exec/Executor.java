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
	

	void buildQueryTree(ZQuery q){
		
		int fN = q.getFrom().getItemN();
		
		//for(Object o)
		
		for(int i = 0; i < fN; ++i){
			ZFromItemEx item = (ZFromItemEx)q.getFrom().getItem(i);
			if(item.isSubQuery()){
				ZQuery subQ = item.getSubQuery();
				subQ.parent = q;
				buildQueryTree(subQ);
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
	
	interface ExprIterCallback{
		void handleColRef(ZColRef colRef);
		void handleSubQuery(ZQuery subQ);
	}
	//iterator of Columne in Expr
	void colInExprIter(ZExp expr, ExprIterCallback callback){
		if(expr == null)
			return;
		if(expr instanceof ZExpression){
			ZExpression e = (ZExpression)expr;
			for(int i = 0; i < e.nbOperands(); ++i){
				colInExprIter(e.getOperand(i), callback);
			}
		}
		else if(expr instanceof ZColRef){
			ZColRef c = (ZColRef)expr;
			callback.handleColRef(c);
		}
		else if(expr instanceof ZInterval){
			ZInterval i = (ZInterval)expr;
			colInExprIter(i.getExpr(), callback);		
		}
		else if(expr instanceof ZSwitchExpr){
			ZSwitchExpr s = (ZSwitchExpr)expr;
			for(Object sube: s.getCond()){
				colInExprIter((ZExp)sube, callback);
			}
			for(Object sube: s.getResult()){
				colInExprIter((ZExp)sube, callback);
			}
			if(s.getCmpVal() != null)
				colInExprIter((ZExp)s.getCmpVal(), callback);
			if(s.getElseResult() != null)
				colInExprIter((ZExp)s.getElseResult(), callback);		
		}
		else if(expr instanceof ZQuery){
			callback.handleSubQuery((ZQuery)expr);
		}
		else if(expr instanceof ZConstant){
			//do nothing
		}
		else throw new ExecException("Parse error #colInExprIter");
	}
	
	
	void pass2_fillColRefTab(ZQuery q){
		
	}
	
	ArrayList<Row> exec(){
		
		return null;
	}
	
}
