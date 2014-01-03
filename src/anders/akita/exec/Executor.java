package anders.akita.exec;

import anders.akita.meta.*;
import anders.akita.parser.*;
import anders.akita.plan.*;
import java.util.concurrent.atomic.*;

public class Executor {

	private Executor(){
		
	}
	
	public static final Executor INSTANCE = new Executor();
	
	AtomicLong qid = new AtomicLong(1);
	
	private long genQid(){
		return qid.getAndIncrement();
	}
	
	public void exec(ZStatement stmt)
		throws ExecException
	{
		if(stmt instanceof ZCreateTable){
			Meta.createTable((ZCreateTable)stmt);
		}
		else if(stmt instanceof ZQuery){
			long qid = genQid();
			QBParser qbp = new QBParser(qid, (ZQuery)stmt);
			QB qb = qbp.parse();
			Planner planner = new Planner(qb, qid);
			QBPlan plan = planner.genQBPlan();
			printPlan(plan);
		}
	}
	
	
	private void printPlan(QBPlan qbp){
		
	}
	
}
