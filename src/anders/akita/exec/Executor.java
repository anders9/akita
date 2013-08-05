package anders.akita.exec;

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
				fi.alias = fi.getSubQuery().getAlias();
				pass1_fillTabAliasName(fi.getSubQuery());
			}
			else{
				fi.table = fi.getFromItem().getTable();
				fi.alias = fi.getFromItem().getAlias();
				if(fi.alias == null)
					fi.alias = fi.table;
			}
		}
	}
	
	interface ExprIterCallback{
		void handleColRef(ZConstant colRef);
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
		else if(expr instanceof ZConstant){
			ZConstant c = (ZConstant)expr;
			if(c.getType() == ZConstant.COLUMNNAME){
				callback.handleColRef(c);
			}
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
		else throw new ExecException("Parse error #colInExprIter");
	}
	
	
	void pass2_fillColRefTab(ZQuery q){
		
	}
	
	ArrayList<Row> exec(){
		
		return null;
	}
	
}
