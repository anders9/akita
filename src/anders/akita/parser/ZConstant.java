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

import java.util.ArrayList;


/**
 * ZConstant: a representation of SQL constants
 */
public class ZConstant extends ZExp {

  /**
   * ZConstant types
   */
	
  //public static final int STAR = -2;
  //public static final int UNKNOWN = -1;
  //public static final int COLUMNNAME = 0;
  //public static final int PREPARED_COL = 0;
  public static final int NULL = 1;
  public static final int NUMBER = 2;
  public static final int STRING = 3;
  public static final int BOOL = 4;

  int type_;
  String val_ = null;

  /**
   * Create a new constant, given its name and type.
   */
  public ZConstant(String v, int typ) {
    val_ = new String(v);
    type_ = typ;
    super.valType = toMySQLType();
  }

  /*
   * @return the constant value
   */
  public String getValue() { return val_; }

  /*
   * @return the constant type
   */
  public int getType() { return type_; }

  public String toString() {
    //if( type_ == STAR) return  "*" ;
	  //if(type_ == PREPARED_COL)
		  //return "?";
    return val_;
  }
  
	public Iterable<ZExp> subExpSet(){
		return null;
	}
	
	public boolean replaceSubExp(ZExp oldExp, ZExp newExp){
		return false;
	}
  
	
	private String toMySQLType(){
		switch(type_){
		case NULL:
		case BOOL:
			return "TINYINT";
		case NUMBER:
			return "INT";
		case STRING:
			return "VARCHAR(" + val_.length() + ")";
		default:
			return null;
		}
	}
};

