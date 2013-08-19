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

public class FunctionMgr {

	public static class AggrDef{
		private boolean canDistrAggr;
		private String mergeAggr;
		private boolean canUseDistinct;
		
		public AggrDef(boolean canUseDistinct, boolean canDistrAggr, String mergeAggr){
			this.canUseDistinct = canUseDistinct;
			this.canDistrAggr = canDistrAggr;
			this.mergeAggr = mergeAggr;
		}
		
		public boolean canDistrAggr(){return canDistrAggr;}
		public String mergeAggr(){return mergeAggr;} 
		public boolean canUseDistinct(){return canUseDistinct;}
	}
	
  private static HashMap<String, Integer> fcts_ = new HashMap<String, Integer>();
  private static HashMap<String, AggrDef> aggrs_ = new HashMap<String, AggrDef>();
  
  static{
	  for(Object[] a: MySQLFunc.func){
		  fcts_.put((String)a[0], (Integer)a[1]);
	  }
	  for(Object[] s: MySQLFunc.aggr){
		  String mergeAggr = null;
		  boolean canDistr = (Boolean)s[2];
		  if(canDistr){
			  if(s.length == 4)
				  mergeAggr = (String)s[3];
			  else
				  mergeAggr = (String)s[0];
		  }
		  aggrs_.put((String)s[0], new AggrDef((Boolean)s[1], canDistr, mergeAggr));
	  }
  }
  
  
  //public static final int VARIABLE_PLIST = 10000;

  public static void addCustomFunction(String fct, int nparm) {
    if(nparm < 0) nparm = 1;
    fcts_.put(fct.toUpperCase(), new Integer(nparm));
  }

  public static void addCustomAggregation(String aggr) {
	 aggrs_.put(aggr.toUpperCase(), null);
  }
  
  
  public static int isCustomFunction(String fct) {
    Integer nparm;
    if(fct == null || fct.length()<1 || fcts_ == null
      || (nparm = (Integer)fcts_.get(fct.toUpperCase())) == null)
       return -1;
    return nparm.intValue();
  }

  public static boolean isAggregate(String op) {
    String tmp = op.toUpperCase().trim();
    return tmp.equals("SUM") || tmp.equals("AVG")
        || tmp.equals("MAX") || tmp.equals("MIN")
        || tmp.equals("COUNT");
  }
  public static boolean isCustomAggregate(String op) {
	  if(op == null)return false;
	  return aggrs_.containsKey(op.toUpperCase().trim());
  }
  
  
//add by wyn 2013.06.28
  public static boolean isJudge(String op){
	String tmp = op.trim();
	return tmp.equals(">") || tmp.equals(">=") 
		|| tmp.equals("<") || tmp.equals("<=")
		|| tmp.endsWith("=") || tmp.equals("!=");
  }
  
  public static String getAggregateCall(String c) {
    int pos = c.indexOf('(');
    if(pos <= 0) return null;
    String call = c.substring(0,pos);
    if(FunctionMgr.isAggregate(call)) return call.trim();
    else return null;
  }

};

