package anders.akita.parser;

import java.util.*;

public class ZCreateTable implements ZStatement{

	public String name;
	
	public String storageEngine;
	
	public boolean distributed = true;

	public ArrayList<String> cols;
	
	public ArrayList<String> types;	
	
	public ZCreateTable(){
		cols = new ArrayList<String>();
		types = new ArrayList<String>();
	}
	
}
