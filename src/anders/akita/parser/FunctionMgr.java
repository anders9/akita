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
import java.io.*;

import org.apache.log4j.Logger;

public class FunctionMgr {

	final static private Logger logger = Logger.getLogger(FunctionMgr.class);

	public static class AggrDef {
		private boolean canDistrAggr;
		private String merger;
		private boolean canUseDistinct;

		public AggrDef(boolean canUseDistinct, boolean canDistrAggr,
				String mergeAggr) {
			this.canUseDistinct = canUseDistinct;
			this.canDistrAggr = canDistrAggr;
			this.merger = mergeAggr;
		}

		public boolean canDistrAggr() {
			return canDistrAggr;
		}

		public String merger() {
			return merger;
		}

		public boolean canUseDistinct() {
			return canUseDistinct;
		}
		
		public String toString(){
			return "{" + canUseDistinct + "," + canDistrAggr + "," + merger + "}";
		}
	}

	private static HashMap<String, Integer> functions = new HashMap<String, Integer>();
	private static HashMap<String, AggrDef> aggrs = new HashMap<String, AggrDef>();
	private static HashMap<String, ZUdf> udfs = new HashMap<String, ZUdf>();
	
	static boolean checkFuncName(String name) {
		return name.matches("[a-zA-Z_][a-zA-Z_0-9]*");
	}

	static {

		// load mysql function
		for (Object[] a : MySQLFunc.func) {
			functions.put( ((String) a[0]).toUpperCase(), (Integer) a[1]);
		}
		// load raw UDF for mysql
		Scanner sc = null;
		try {
			sc = new Scanner(new FileInputStream("conf/raw_udf.txt"));
			while (sc.hasNextLine()) {
				String line = sc.nextLine().trim();
				if (!line.equals("") && !line.startsWith("//")) {
					String[] r = line.split(" ");
					if (r.length != 2 || !checkFuncName(r[0])
							|| !r[1].matches("[0-9]+"))
						throw new ParseException(
								"Illegal line in raw_UDF config file: " + line);
					functions.put(r[0].toUpperCase(), Integer.parseInt(r[1]));
				}
			}
		} catch (Exception e) {
			logger.warn("error when handling raw_UDF config file: "
					+ e.toString() + ":" + e.getMessage());
		} finally {
			if (sc != null)
				sc.close();
		}

		// load mysql aggregation function
		for (Object[] s : MySQLFunc.aggr) {
			String merger = null;
			boolean canDistr = (Boolean) s[2];
			if (canDistr) {
				if (s.length == 4)
					merger = (String) s[3];
				else
					merger = (String) s[0];
			}
			if(merger != null)
				merger = merger.toUpperCase();
			aggrs.put(((String) s[0]).toUpperCase(), new AggrDef((Boolean) s[1], canDistr,
					merger));
		}
		// load raw UDAF for mysql
		sc = null;
		try {
			sc = new Scanner(new FileInputStream("conf/raw_udaf.txt"));

			while (sc.hasNextLine()) {
				String line = sc.nextLine().trim();
				if (!line.equals("") && !line.startsWith("//")) {
					String[] r = line.split(" ");
					if (r.length != 3 || !checkFuncName(r[0])
							|| !r[1].matches("[01]") || !r[2].matches("[01]"))
						throw new ParseException(
								"Illegal line in raw_UDAF config file: " + line);
					aggrs.put(r[0].toUpperCase(),
							new AggrDef(r[1].equals("1"), r[2].equals("1"),
									r[2].equals("1") ? r[0].toUpperCase() : null));
				}
			}
		} catch (Exception e) {
			logger.warn("error when handling raw_UDAF config file: "
					+ e.toString() + ":" + e.getMessage());
		} finally {
			if (sc != null)
				sc.close();
		}

		// load UDF
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("conf/udf.txt");
			ZqlJJParser parser = new ZqlJJParser( fis );
			while (true) {
				ZUdf udf = parser.SQLUdf();
				if (udf == null)
					break;
				udfs.put(udf.getName(), udf);
			}

		} catch (Exception e) {
			logger.warn("error when handling UDF config file: "
					+ e.toString() + ":" + e.getMessage());
		} 
	}

	// public static final int VARIABLE_PLIST = 10000;
	/*
	 * public static void addCustomFunction(String fct, int nparm) { if(nparm <
	 * 0) nparm = 1; fcts_.put(fct.toUpperCase(), new Integer(nparm)); }
	 * 
	 * public static void addCustomAggregation(String aggr) {
	 * aggrs_.put(aggr.toUpperCase(), null); }
	 */

	public static boolean isUdf(String udfName){
		return udfs.containsKey(udfName);
	}
	
	public static int getUdfParmN(String udfName){
		return udfs.get(udfName).getParmN();
	}
	
	public static String getUdfExp(String udfName){
		return udfs.get(udfName).getExp();
	}
	
	public static boolean getUdfParmToAggr(String udfName, int idx){
		return udfs.get(udfName).getParmAggr(idx);
	}
	
	public static boolean aggrCanUseDistinct(String aggrName){
		return aggrs.get(aggrName).canUseDistinct;
	}
	public static boolean aggrCanDistrAggr(String aggrName){
		return aggrs.get(aggrName).canDistrAggr;
	}
	public static String getAggrMerger(String aggrName){
		return aggrs.get(aggrName).merger;
	}
	
	public static boolean isFunction(String funcName){
		return functions.containsKey(funcName);
	}
	
	public static int getFuncParmN(String funcName) {
		return functions.get(funcName);
	}

	public static boolean isAggregation(String aggrName) {
		if (aggrName == null)
			return false;
		return aggrs.containsKey(aggrName.toUpperCase().trim());
	}

};
