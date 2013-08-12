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


import java.util.* ;

/**
 * ZQuery: an SQL SELECT statement
 */

public class ZQuery implements ZStatement, ZExp {
	
	public ZQuery parent;
	public ZQuery outer;
	
	static class InnerQuery{
		ZExp cond;
		ZQuery query;
	}
	
	public ArrayList<InnerQuery> innerQueryInWhere = new ArrayList<InnerQuery>();
	public ArrayList<InnerQuery> innerQueryInHaving = new ArrayList<InnerQuery>();
	
  Vector<ZSelectItem> select_;
  boolean distinct_ = false;
  ZFromClause from_;
  ZExp where_ = null;
  ZGroupBy groupby_ = null;
  //ZExpression setclause_ = null;
  Vector<ZExp> orderby_ = null;
  //boolean forupdate_ = false;

  /**
   * Create a new SELECT statement
   */
  public ZQuery() {}
  
  
  /**
   * Insert the SELECT part of the statement
   * @param s A vector of ZSelectItem objects
   */
  public void setSelect(Vector<ZSelectItem> s) { select_ = s; }

  /**
   * Insert the FROM part of the statement
   * @param f a Vector of ZFromItem objects
   */
  public void setFrom(ZFromClause f) { from_ = f; }

  /**
   * Insert a WHERE clause
   * @param w An SQL Expression
   */
  public void setWhere(ZExp w) { where_ = w; }

  /**
   * Insert a GROUP BY...HAVING clause
   * @param g A GROUP BY...HAVING clause
   */
  public void setGroupBy(ZGroupBy g) { groupby_ = g; }

  /**
   * Insert a SET clause (generally UNION, INTERSECT or MINUS)
   * @param s An SQL Expression (generally UNION, INTERSECT or MINUS)
   */
  //public void addSet(ZExpression s) { setclause_ = s; }

  /**
   * Insert an ORDER BY clause
   * @param v A vector of ZOrderBy objects
   */
  public void setOrderBy(Vector<ZExp> v) { orderby_ = v; }

  /**
   * Get the SELECT part of the statement
   * @return A vector of ZSelectItem objects
   */
  public Vector<ZSelectItem> getSelect() { return select_; }

  /**
   * Get the FROM part of the statement
   * @return A vector of ZFromItem objects
   */
  public ZFromClause getFrom() { return from_; }

  /**
   * Get the WHERE part of the statement
   * @return An SQL Expression or sub-query (ZExpression or ZQuery object)
   */
  public ZExp getWhere() { return where_; }

  /**
   * Get the GROUP BY...HAVING part of the statement
   * @return A GROUP BY...HAVING clause
   */
  public ZGroupBy getGroupBy() { return groupby_; }

  /**
   * Get the SET clause (generally UNION, INTERSECT or MINUS)
   * @return An SQL Expression (generally UNION, INTERSECT or MINUS) 
   */
  //public ZExpression getSet() { return setclause_; }

  /**
   * Get the ORDER BY clause
   * @param v A vector of ZOrderBy objects
   */
  public Vector<ZExp> getOrderBy() { return orderby_; }

  /**
   * @return true if it is a SELECT DISTINCT query, false otherwise.
   */
  public boolean isDistinct() { return distinct_; }

  /**
   * @return true if it is a FOR UPDATE query, false otherwise.
   */
  //public boolean isForUpdate() { return forupdate_; }


  public String toString() {
    StringBuffer buf = new StringBuffer("select ");
    if(distinct_) buf.append("distinct ");

    //buf.append(select_.toString());
    int i;
    buf.append(select_.elementAt(0).toString());
    for(i=1; i<select_.size(); i++) {
      buf.append(", " + select_.elementAt(i).toString());
    }

    //buf.append(" from " + from_.toString());
    if(from_ != null){
	    buf.append(" from ");
	    if(from_.join_type == ZFromClause.INNER_JOIN){
	    	Vector<ZFromItemEx> from_ = this.from_.items;
		    
		    buf.append(from_.elementAt(0).toString());
		    for(i=1; i<from_.size(); i++) {
		      buf.append(", " + from_.elementAt(i).toString());
		    }
	    }
	    else if(from_.join_type == ZFromClause.LEFT_JOIN){
	    	buf.append(this.from_.items.elementAt(0).toString())
	    	.append(" LEFT JOIN ")
	    	.append(this.from_.items.elementAt(1).toString());
	    	if(from_.join_cond != null){
	    		buf.append(" ON ")
	    		.append(from_.join_cond.toString());
	    	}
	    }
    }

    if(where_ != null) {
      buf.append(" where " + where_.toString());
    }
    if(groupby_ != null) {
      buf.append(" " + groupby_.toString());
    }
    //if(setclause_ != null) {
    //  buf.append(" " + setclause_.toString());
    //}
    if(orderby_ != null) {
      buf.append(" order by ");
      //buf.append(orderby_.toString());
      buf.append(orderby_.elementAt(0).toString());
      for(i=1; i<orderby_.size(); i++) {
        buf.append(", " + orderby_.elementAt(i).toString());
      }
    }
    //if(forupdate_) buf.append(" for update");

    return buf.toString();
  }

};

