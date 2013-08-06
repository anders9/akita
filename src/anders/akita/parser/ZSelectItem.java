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

import java.io.* ;
import java.util.* ;

/**
 * ZSelectItem: an item in the SELECT part of an SQL query.
 * (The SELECT part of a query is a Vector of ZSelectItem).
 */
public class ZSelectItem {

	public final static int STAR = 1;
	public final static int TAB_DOT_STAR = 2;
	public final static int EXPR = 3;
	
	public ZExp expr;
	
	public String table;
	
	public int type;
	
	public String alias;
	
	public ZSelectItem(ZExp exp, String alias){
		this.expr = exp;
		this.alias = alias;
		this.type = EXPR;
	}
	public ZSelectItem(String table){
		this.table = table;
		this.type = TAB_DOT_STAR;
	}
	public ZSelectItem(){
		this.type = STAR;
	}
	
	public String toString(){
		String s;
		switch(type){
		case STAR:
			return "*";
		case TAB_DOT_STAR:
			return table + ".*";
		case EXPR:
			s = expr.toString();
			if(alias != null)
				s += (" AS " + alias);
			return s;
		default:
			throw new ExecException("Parse error#ZSelectItem");
		}
	}
};

