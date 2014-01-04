package anders.akita.meta;

import java.util.*;

import anders.akita.parser.*;

public final class Meta {

	//static final Meta INSTANCE = new Meta();
	
	static HashMap<String, Table> tables;
	static String[] entries;
	static final int NODE_N = 3;
	static Random rand;
	
	static{
		tables = new HashMap<String, Table>();
		rand = new Random();
		entries = new String[NODE_N];
		
		for(int i = 1; i <= NODE_N; ++i){
			entries[i - 1] = "Node" + i;
		}
	}
	
	static public Table getTab(String tab){
		return tables.get(tab);
	}
	
	static public boolean containCol(String tab, String col){
		return tables.get(tab).containCol(col);
	}
	
	static public String[] randomEntries(int N){
		if(N > NODE_N)
			N = NODE_N;
		if(N == NODE_N)
			return entries.clone();

		ArrayList<String> l = new ArrayList<String>();
		
		while(l.size() < N){
			String s = entries[rand.nextInt(NODE_N)];
			if(!l.contains(s)){
				l.add(s);
			}
		}
		return l.toArray(new String[0]);
	}
	
	static public void createTable(ZCreateTable ct){
		Table t = new Table();
		t.name = ct.name;
		t.isDistr = ct.distributed;
		if(t.isDistr)
			t.entries = entries.clone();
		else
			t.entries = null;
		t.col = new Column[ct.cols.size()];
		for(int i = 0; i < t.col.length; ++i){
			t.col[i] = new Column(ct.cols.get(i), ct.types.get(i));
		}
		t.colName = (String[]) ct.cols.toArray(new String[0]);
		tables.put(t.name, t);
	}
	
}
