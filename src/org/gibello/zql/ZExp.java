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

package org.gibello.zql;

import java.io.* ;
import java.util.* ;

/**
 * A common interface for all SQL Expressions (ZQueries, ZExpressions and
 * ZConstants are ZExps).
 */
public interface ZExp extends java.io.Serializable {
	
	public static final int SELECT_EXPR = 1;
	public static final int WHERE_EXPR = 2;
	public static final int HAVING_EXPR = 3;	
	public static final int FROM_EXPR = 4;
};

