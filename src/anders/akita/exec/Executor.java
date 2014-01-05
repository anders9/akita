package anders.akita.exec;

import anders.akita.meta.*;
import anders.akita.parser.*;
import anders.akita.plan.*;
import java.util.concurrent.atomic.*;
import java.io.*;

public class Executor {

	private Executor(){
		
	}
	
	public static final Executor INSTANCE = new Executor();
	
	AtomicLong qid = new AtomicLong(1);
	
	private long genQid(){
		return qid.getAndIncrement();
	}
	
	public void exec(ZStatement stmt, PrintStream out)
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
			printPlan(plan, out, true);
		}
	}
	
	private String strArr2Str(String[] strArr){
		String s = "";
		for(int i = 0; i < strArr.length; ++i){
			if(i > 0) s += ",";
			s += strArr[i];
		}
		return s;
	}
	
	private void printPlan(QBPlan qbp, PrintStream out, boolean isRoot){
		
		for(QBPlan subp : qbp.prevQBPlans){
			printPlan(subp, out, false);
		}
		
		out.println("# plan for table: " + qbp.schema.name + (isRoot?"(root)":""));
		out.println();
		
		for(int i = 0; i < qbp.operators.length; ++i){
			FetchDataOperator fdo = qbp.operators[i];
			out.println("step " + i + " :");
			if(fdo instanceof MapJoinOperator){
				MapJoinOperator mjo = (MapJoinOperator)fdo;
				String s1 = String.format(
						"Execute query \"%s\" on %s,\ncollect data into table %s on %s,\n"
					+	"then copy the whole table into %s",
						mjo.leftSrc.fetchSQL, strArr2Str(mjo.leftSrc.entries), 
						mjo.leftSrc.schema.name, mjo.collectNode,
						strArr2Str(mjo.entries));
				out.println(s1);
			}
			else if(fdo instanceof ReduceJoinOperator){
				ReduceJoinOperator rjo = (ReduceJoinOperator)fdo;
				String[] idxL = new String[rjo.joinKeyIdx.length];
				String[] idxR = new String[rjo.joinKeyIdx.length];
				for(int k = 0; k < rjo.joinKeyIdx.length; ++k){
					idxL[k] = rjo.srcs[0].schema.col[rjo.joinKeyIdx[k][0]];
					idxR[k] = rjo.srcs[1].schema.col[rjo.joinKeyIdx[k][1]];
				}
				
				String s1 = String.format(
						"Execute query \"%s\" on %s then shuffle by %s,\n"
					+	"and execute query \"%s\" on %s then shuffle by %s,\n"
					+	"shuffle into table %s and %s on %s",
					rjo.srcs[0].fetchSQL, strArr2Str(rjo.srcs[0].entries), strArr2Str(idxL), 
					rjo.srcs[1].fetchSQL, strArr2Str(rjo.srcs[1].entries), strArr2Str(idxR),
					rjo.srcs[0].schema.name, rjo.srcs[1].schema.name, strArr2Str(rjo.entries)
						);
				out.println(s1);
			}
			else if(fdo instanceof AggrOperator){
				AggrOperator ao = (AggrOperator)fdo;
				String[] idx = new String[ao.shuffleKeyIdx.length];
				for(int k = 0; k < idx.length; ++k)
					idx[k] = ao.src.schema.col[k];
				String s1 = String.format(
						"Execute query \"%s\" on %s then shuffle by %s,\n"
					+	"shuffle into table %s on %s",
					ao.src.fetchSQL, strArr2Str(ao.src.entries), strArr2Str(idx),
					ao.src.schema.name, strArr2Str(ao.entries)
						);
				out.println(s1);
			}
			else if(fdo instanceof ShuffleOperator){
				ShuffleOperator so = (ShuffleOperator)fdo;
				String s1 = String.format(
						"Execute query \"%s\" on %s,\nthen shuffle%s into table %s on %s",
					so.src.fetchSQL, strArr2Str(so.src.entries),
					so.distinctShuffle? " by all column": "",
					so.src.schema.name, strArr2Str(so.entries)
						);
				out.println(s1);
			}
		}
		FetchDataOperator lastOp = qbp.operators[qbp.operators.length - 1];
		out.println("step " + qbp.operators.length + " :");
		String s1 = String.format(
				"Execute query \"%s\" on %s then store into table %s on each node",
				lastOp.fetchSQL, strArr2Str(lastOp.entries), qbp.schema.name
				);
		out.println(s1);
	}
}
