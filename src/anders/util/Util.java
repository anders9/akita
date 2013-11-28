package anders.util;

import org.apache.log4j.*;
import java.io.*;
import java.util.*;

public final class Util {

	public static <E> ArrayList<E> mergeArrayList(ArrayList<E> ... ll){
		
		ArrayList<E> list = new ArrayList<E>();
		for(ArrayList<E> l: ll){
			for(E e: l){
				if(!list.contains(e))
					list.add(e);
			}
		}
		return list;
	}
	
	public static int findStr(String name, String[] array){
		if(name != null){
			for(int i = 0; i < array.length; ++i){
				if(name.equals(array[i]))
					return i;
			}
		}
		return -1;
	}
	
	public static void initLog4j(){
		PropertyConfigurator.configure("conf/log4j.properties");
	}
	
	static public String exceptionStackTrace(Exception e){
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		pw.close();
		return sw.toString();
	}
	
}
