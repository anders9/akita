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
 * ZGroupBy: an SQL GROUP BY...HAVING clause
 */
public class ZGroupBy {

  Vector<ZExp> groupby_;
  ZExp having_ = null;

  public ArrayList<ZExp> havingList = new ArrayList<ZExp>();

  
  /**
   * Create a GROUP BY given a set of Expressions
   * @param exps A vector of SQL Expressions (ZExp objects).
   */
  public ZGroupBy(Vector<ZExp> exps) { groupby_ = exps; }

  /**
   * Initiallize the HAVING part of the GROUP BY
   * @param e An SQL Expression (the HAVING clause)
   */
  public void setHaving(ZExp e) { having_ = e; }

  /**
   * Get the GROUP BY expressions
   * @return A vector of SQL Expressions (ZExp objects)
   */
  public Vector<ZExp> getGroupBy() { return groupby_; }

  /**
   * Get the HAVING clause
   * @return An SQL expression
   */
  public ZExp getHaving() { return having_; }

  public String toString() {
	  StringBuffer buf = new StringBuffer();
	  if(groupby_ != null){
	    buf.append("GROUP BY ");
	
	    //buf.append(groupby_.toString());
	    buf.append(groupby_.get(0).toString());
	    for(int i=1; i<groupby_.size(); i++) {
	      buf.append(", " + groupby_.get(i).toString());
	    }
	  }
	  if(groupby_ != null && having_ != null)
		  buf.append(' ');
    if(having_ != null) {
      buf.append("HAVING " + having_.toString());
    }
    return buf.toString();
  }
};

