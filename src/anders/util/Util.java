package anders.util;

import org.apache.log4j.PropertyConfigurator;

public final class Util {

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
}
