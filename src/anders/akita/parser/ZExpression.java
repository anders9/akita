/*
 * This file is part of Zql.
 *
 * Zql is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Zql is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Zql.  If not, see <http://www.gnu.org/licenses/>.
 */

package anders.akita.parser;

import java.util.*;

/**
 * ZExpression: an SQL Expression An SQL expression is an operator and one or
 * more operands Example: a AND b AND c -> operator = AND, operands = (a, b, c)
 */
public class ZExpression extends ZExp {

	public final static int OPERATOR = 0;
	public final static int FUCTION = 1;
	public final static int AGGR_ALL = 2;
	public final static int AGGR_DISTINCT = 3;
	public final static int UDF = 4;
	
	// public final static int NOT_AGGR = 0;
	public final static int VAR_PARAM = 1000000;

	// boolean is_funciton_ = false;
	Operator op_ = null;
	public String funcOrAggrName;
	Vector<ZExp> operands_ = null;
	public int type = OPERATOR;
	
	public String inType;//used for aggregation
	public String outType;//used for aggregation
	
	public boolean isAggr(){
		return type == ZExpression.AGGR_ALL || type == ZExpression.AGGR_DISTINCT;
	}

	/**
	 * Create an SQL Expression given the operator
	 * 
	 * @param op
	 *            The operator
	 */
	public ZExpression(Operator op) {
		op_ = op;
	}

	/**
	 * Create an SQL Expression given the operator and 1st operand
	 * 
	 * @param op
	 *            The operator
	 * @param o1
	 *            The 1st operand
	 */
	public ZExpression(Operator op, ZExp o1) {
		op_ = op;
		addOperand(o1);
	}

	/**
	 * Create an SQL Expression given the operator and operands array
	 * 
	 * @param op
	 *            The operator
	 * @param v
	 *            The operands array
	 */
	public ZExpression(Operator op, Vector<ZExp> v) {
		op_ = op;
		operands_ = v;
		// this.is_funciton_ = true;
	}

	/**
	 * construct normal function expression
	 * 
	 * @param op
	 *            function-name
	 * @param v
	 *            params
	 */
	public ZExpression(String funcName, Vector<ZExp> v) {
		this.funcOrAggrName = funcName;
		this.type = ZExpression.FUCTION;
		this.operands_ = v;
	}
	
	/**
	 * construct normal function expression
	 * 
	 * @param op
	 *            function-name
	 * @param v
	 *            params
	 * @param type
	 *            must be UDF/FUNCTION
	 */	
	public ZExpression(String funcName, Vector<ZExp> v, int type) {
		this.funcOrAggrName = funcName;
		this.type = type;
		this.operands_ = v;
	}
	/**
	 * construct aggregation function expression
	 * 
	 * @param aggrName
	 *            Aggregation function name
	 * @param o1
	 *            The 1st operand
	 * @param type
	 *            must be AGGR_ALL or AGGR_DISTINCT
	 */
	public ZExpression(String aggrName, ZExp o1, boolean distinct) {
		this.funcOrAggrName = aggrName;
		this.type = distinct ? ZExpression.AGGR_DISTINCT : ZExpression.AGGR_ALL;
		addOperand(o1);
	}

	/**
	 * construct aggregation function expression
	 * 	
	 * 	only used in:
	 * 		COUNT(*)
	 * 		COUNT(DISTINCT exp1 [, exp2, ... ] )
	 * 
	 * @param aggrName
	 *            Aggregation function name
	 *            
	 * @param operands
	 *            The operands
	 * @param type
	 *            must be AGGR_ALL or AGGR_DISTINCT
	 */
	@SuppressWarnings("unchecked")
	public ZExpression(String aggrName, Vector<ZExp> operands, boolean distinct) {
		this.funcOrAggrName = aggrName;
		this.type = distinct ? ZExpression.AGGR_DISTINCT : ZExpression.AGGR_ALL;
		this.operands_ = (Vector<ZExp>)operands.clone();
	}
	
	
	/**
	 * Create an SQL Expression given the operator, 1st and 2nd operands
	 * 
	 * @param op
	 *            The operator
	 * @param o1
	 *            The 1st operand
	 * @param o2
	 *            The 2nd operand
	 */
	public ZExpression(Operator op, ZExp o1, ZExp o2) {
		op_ = op;
		addOperand(o1);
		addOperand(o2);
	}

	/**
	 * Create an SQL Expression given the operator, 1st, 2nd and 3rd operands
	 * 
	 * @param op
	 *            The operator
	 * @param o1
	 *            The 1st operand
	 * @param o2
	 *            The 2nd operand
	 * @param o3
	 *            the 3rd operand
	 */
	public ZExpression(Operator op, ZExp o1, ZExp o2, ZExp o3) {
		op_ = op;
		addOperand(o1);
		addOperand(o2);
		addOperand(o3);
	}

	/**
	 * Get this expression's operator.
	 * 
	 * @return the operator.
	 */
	public Operator getOperator() {
		return op_;
	}

	/**
	 * Set the operands list
	 * 
	 * @param v
	 *            A vector that contains all operands (ZExp objects).
	 */
	public void setOperands(Vector<ZExp> v) {
		operands_ = v;
	}

	/**
	 * Get this expression's operands.
	 * 
	 * @return the operands (as a Vector of ZExp objects).
	 */

	public Vector<ZExp> getOperands() {
		return operands_;
	}

	/**
	 * Add an operand to the current expression.
	 * 
	 * @param o
	 *            The operand to add.
	 */
	public void addOperand(ZExp o) {
		if (operands_ == null)
			operands_ = new Vector<ZExp>();
		operands_.addElement(o);
	}

	// add by wyn 2013.06.28
	public boolean setOperand(ZExp o, int index) {
		if (index < 0 || index > operands_.size())
			return false;
		operands_.set(index, o);
		return true;
	}

	/**
	 * Get an operand according to its index (position).
	 * 
	 * @param pos
	 *            The operand index, starting at 0.
	 * @return The operand at the specified index, null if out of bounds.
	 */
	public ZExp getOperand(int pos) {
		if (operands_ == null || pos >= operands_.size())
			return null;
		return operands_.elementAt(pos);
	}

	/**
	 * Get the number of operands
	 * 
	 * @return The number of operands
	 */
	public int nbOperands() {
		if (operands_ == null)
			return 0;
		return operands_.size();
	}

	/**
	 * String form of the current expression (reverse polish notation). Example:
	 * a > 1 AND b = 2 -> (AND (> a 1) (= b 2))
	 * 
	 * @return The current expression in reverse polish notation (a String)
	 */
	public String toReversePolish() {
		StringBuffer buf = new StringBuffer("(");
		buf.append(op_.toString());
		for (int i = 0; i < nbOperands(); i++) {
			ZExp opr = getOperand(i);
			if (opr instanceof ZExpression)
				buf.append(" " + ((ZExpression) opr).toReversePolish()); 
			else
				buf.append(" " + opr.toString());
		}
		buf.append(")");
		return buf.toString();
	}

	// public boolean isFunction(){
	// return is_funciton_;
	// }

	public String formatSubExp(ZExp e){
		/*
		if( e instanceof ZColRef || e instanceof ZConstant ){
			return e.toString();
		}
		String t = "(" + e.toString() + ")";
		if( e instanceof ZQuery){
			if( ((ZQuery)e).innerQType == ZQuery.InnerQType.ALL )
				t = "ALL " + t;
			else if(( (ZQuery)e).innerQType == ZQuery.InnerQType.ANY )
				t = "ANY " + t;
		}
		*/
		
		return e.toString();
	}
	
	public String toString() {

		// if(op_.equals("?")) return op_; // For prepared columns ("?")

		if (type != OPERATOR)
			return formatFunction();

		StringBuffer buf = new StringBuffer();
		buf.append("(");
		ZExp operand;
		if(nbOperands() == 1) {
			operand = getOperand(0);
			if(op_ == Operator.IS_NULL || op_ == Operator.IS_NOT_NULL)
				buf.append(operand.toString() + " " + op_.op());
			else
				buf.append(op_.op() + " " + operand.toString());
		}
		else if (op_ == Operator.BETWEEN || op_ == Operator.NOT_BETWEEN) {
				buf.append(
						getOperand(0).toString() +  " " + op_.op() + " " 
						+ getOperand(1).toString() + " AND "
						+ getOperand(2).toString());
		}
		else{
			int nb = nbOperands();
			for (int i = 0; i < nb; i++) {

				operand = getOperand(i);
				buf.append(formatSubExp(operand));
				
				if (i < nb - 1) {
					buf.append(" " + op_.op() + " ");
				}
			}
		}
		buf.append(")");
		return buf.toString();
	}

	private String formatFunction() {

		StringBuffer b = new StringBuffer();
		if(this.type == ZExpression.AGGR_ALL || this.type == ZExpression.AGGR_DISTINCT){
			
			if(this.funcOrAggrName.equalsIgnoreCase("COUNT")
				&& getOperand(0) == null)
					b.append("COUNT(*)");
			else{
				b.append(this.funcOrAggrName + "(");
				if(this.type == ZExpression.AGGR_DISTINCT)
					b.append(" DISTINCT ");
				b.append(getOperand(0).toString());
				if(this.funcOrAggrName.equalsIgnoreCase("COUNT")){
					for(int i = 1; i < this.operands_.size(); ++i)
						b.append("," + this.operands_.get(i));
				}
				b.append(")");
			}
		}
		else{
			b.append(this.funcOrAggrName + "(");
			int nb = nbOperands();
			for (int i = 0; i < nb; i++) {
				b.append(getOperand(i).toString() + (i < nb - 1 ? "," : ""));
			}
			b.append(")");
		}
		return b.toString();
	}
	
	public Iterable<ZExp> subExpSet(){
		ArrayList<ZExp> set = new ArrayList<ZExp>();
		set.addAll(getOperands());
		return set;
	}

	/**
	 * replace sub-expression of an exp-node, 
	 * if there are several sub-expression being the same object
	 *  (it is possible when the parser or optimizer
	 *   do some internal replacement for UDF or other cases), 
	 * all of them are replaced.
	 */
	public boolean replaceSubExp(ZExp oldExp, ZExp newExp) {

		boolean r = false;
		for (int i = 0; i < this.nbOperands(); ++i) {
			if (this.getOperand(i) == oldExp) {
				this.setOperand(newExp, i);
				r = true;
			}
		}

		return r;
	}
};
