package anders.akita.exec;

import anders.akita.parser.*;

public class MidField {

	String name;// "table$column_name" or "alias"
	
	ZColRef col;
		
	public MidField(String name, ZColRef col){
		this.name = name;
		this.col = col;
	}
}
