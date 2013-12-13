package anders.akita.test;

import java.util.*;

public class Test1 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		ArrayList<String>[] a1 = (ArrayList<String>[])new ArrayList[1];
		Object[] o1 = a1;
		ArrayList<Integer> t = new ArrayList<Integer>();
		o1[0] = t;
		a1[0].add("fuck");
		t.add(123);
		System.out.println("print 0: " + t.get(0) + " 1: " + t.get(1));
	}

}
