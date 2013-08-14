package anders.akita.parser;


public class ZColRef extends ZExp {
	
	public String table;
	public String col;
	public ZQuery query;
	
	public ZColRef(String table, String col){
		
		this.table = table;
		this.col = col;
	}

	public String toString(){
		if(table == null)return col;
		return table + "." + col;
	}
}
