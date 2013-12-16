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

/**
 * An SQL query ORDER BY clause.
 */
public class ZOrderBy {
  //ZExp col_;
  String col_;
  boolean asc_ = true;

  public ZOrderBy(String s) { col_ = s; }

  /**
   * Set the order to ascending or descending (defailt is ascending order).
   * @param a true for ascending order, false for descending order.
   */
  public void setAscOrder(boolean a) { asc_ = a; }

  /**
   * Get the order (ascending or descending)
   * @return true if ascending order, false if descending order.
   */
  public boolean getAscOrder() { return asc_; }

  /**
   * Get the ORDER BY expression.
   * @return An expression (generally, a ZConstant that represents a column
   * name).
   */
  public String getCol() { return col_; }

  public String toString() {
    return col_ + " " + (asc_ ? "ASC" : "DESC");
  }
};

