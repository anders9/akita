package anders.akita.meta;

public class Column {
	
	String name;
	int len;//used for char/varchar type
	
	public String getName(){
		return name;
	}
	
	public int getLen(){
		return len;
	}
}
