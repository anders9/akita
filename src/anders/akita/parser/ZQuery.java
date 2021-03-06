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

public class ZQuery extends ZExp implements ZStatement {
	
	//boolean isInnerQuery = false;
	
	//public ZQuery parentQuery;
	//public ZQuery outerQuery;
	
	//Only use for inner-query, 
	//if exist in the where clause of outer-query, set it true, otherwise (exist in having clause) false
	//public boolean inWhereClause;

	/*
	 * Only use for inner-Query:
	 * 	ALL/any/exist/not exist inner-query not need return unique row
	 *  other inner-Query need return unique row
	 */
	public enum InnerQType {NORMAL, EXISTS, IN, ANY, ALL};
	public InnerQType innerQType = null;
	
	/*
	public ArrayList<ZQuery> innerQinWhereList = new ArrayList<ZQuery>();
	public ArrayList<ZQuery> innerQinHavingList = new ArrayList<ZQuery>();
	
	public HashMap<String, ZFromItemEx> tabList = new HashMap<String, ZFromItemEx>();
	public HashMap<String, ZSelectItem> fieldList = new HashMap<String, ZSelectItem>();
	
	public ArrayList<ZExp> whereList = new ArrayList<ZExp>();
	public ArrayList<ZExp> groupByKey = null;
	public ArrayList<ZExp> havingList = null;
	*/
	
	static class InnerQuery{
		ZExp cond;
		ZQuery query;
	}
	
  Vector<ZSelectItem> select_;
  boolean distinct_ = false;
  ZFromClause from_;
  ZExp where_ = null;
  ZGroupBy groupby_ = null;
  //ZExpression setclause_ = null;
  Vector<ZOrderBy> orderby_ = null;
  //boolean forupdate_ = false;

  public int topK = -1; //if with order-by, must with this value
  
  public int shuffleN = -1;  
  public MidTabStorageType shuffleMtst = MidTabStorageType.Memory;
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
  public void setOrderBy(Vector v) { orderby_ = v; }

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
  public Vector<ZOrderBy> getOrderBy() { return orderby_; }

  /**
   * @return true if it is a SELECT DISTINCT query, false otherwise.
   */
  public boolean isDistinct() { return distinct_; }

  /**
   * @return true if it is a FOR UPDATE query, false otherwise.
   */
  //public boolean isForUpdate() { return forupdate_; }


  public String toString() {
	  
	  
    StringBuffer buf = new StringBuffer();
	if(this.innerQType != null){
		  if(this.innerQType == InnerQType.ALL)
			  buf.append("ALL ");
		  else if(this.innerQType == InnerQType.ANY)
			  buf.append("ANY ");
		  buf.append("(");
	  }
    buf.append("select ");

    if(distinct_) buf.append("distinct ");

    //buf.append(select_.toString());
    int i;
    buf.append(select_.elementAt(0).toString());
    for(i=1; i<select_.size(); i++) {
      buf.append(", " + select_.elementAt(i).toString());
    }

    //buf.append(" from " + from_.toString());
    if(from_ != null){
    	buf.append(" " + from_.toString());
    }

    if(where_ != null) {
      buf.append(" where " + where_.toString());
    }
    if(groupby_ != null) {
      buf.append(" " + groupby_.toString());
    }

    if(orderby_ != null) {
      buf.append(" order by ");
      //buf.append(orderby_.toString());
      buf.append(orderby_.elementAt(0).toString());
      for(i=1; i<orderby_.size(); i++) {
        buf.append(", " + orderby_.elementAt(i).toString());
      }
    }
    if(this.innerQType != null)
    	buf.append(")");
	  
    return buf.toString();
  }

	public Iterable<ZExp> subExpSet(){
		return new ArrayList<ZExp>();
	}
	
	public boolean replaceSubExp(ZExp oldExp, ZExp newExp){
		return false;
	}  
  
};

