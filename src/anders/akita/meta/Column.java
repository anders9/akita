package anders.akita.meta;

public class Column {
	
	String name;
	//int len;//used for char/varchar type
	
	String type;
	
	public Column(String name, String type){
		this.name = name;
		this.type = type;
	}
	
	public String getName(){
		return name;
	}
	
	public String getType(){
		return type;
	}
}
