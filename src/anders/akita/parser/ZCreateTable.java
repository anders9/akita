package anders.akita.parser;

import java.util.*;

public class ZCreateTable implements ZStatement{

	public String name;
	
	public String storageEngine;
	
	public String distributed;

	public ArrayList<String> cols;
	
	public ArrayList<String> types;	
	
}
