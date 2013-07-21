package anders.akita.exec;

public final class Row {

	Object[] row;
	
	public Row(Object[] row){
		this.row = row;
	}
	
	public Object get(int index){
		return row[index];
	}
	
}
