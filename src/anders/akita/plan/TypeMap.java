package anders.akita.plan;

/**
 * 
 * @author anders
 *	
 *	
��������           ��ʾ����            ���ݿ�����         JAVA����                                      JDBC��������(int) ����
VARCHAR        L+N                  VARCHAR            java.lang.String                                  12   
CHAR               N                      CHAR                   java.lang.String                                  1   
BLOB               L+N                  BLOB                   java.lang.byte[]                                   -4   
TEXT               65535              VARCHAR             java.lang.String                                  -1               
INTEGER         4                      INTEGER              UNSIGNED java.lang.Long                  4   
TINYINT           3                      TINYINT                UNSIGNED java.lang.Integer               -6   
SMALLINT       5                      SMALLINT            UNSIGNED java.lang.Integer                5   
MEDIUMINT     8                      MEDIUMINT          UNSIGNED java.lang.Integer                4   
BIT                  1                      BIT                        java.lang.Boolean                                -7   
BIGINT             20                   BIGINT                  UNSIGNED java.math.BigInteger         -5   
FLOAT            4+8                  FLOAT                 java.lang.Float                                      7   
DOUBLE          22                   DOUBLE               java.lang.Double                                   8   
DECIMAL         11                   DECIMAL              java.math.BigDecimal                           3  
 
BOOLEAN        1                    ͬTINYINT       
            
ID                    11                    PK                       (INTEGER UNSIGNED)java.lang.Long     4   
            
DATE              10                    DATE                   java.sql.Date                                          91   
TIME                8                     TIME                    java.sql.Time                                          92   
DATETIME      19                    DATETIME           java.sql.Timestamp                                93   
TIMESTAMP   19                    TIMESTAMP         java.sql.Timestamp                                93   
YEAR               4                     YEAR                  java.sql.Date
 *
 *
 *
 */

import anders.akita.parser.ExecException;

public final class TypeMap {

	String getType(Object obj, int len)
		throws ExecException
	{
		if(obj instanceof String)
			return "VARCHAR" + "(" + len + ")";
		else if(obj instanceof Long)
			return "INTEGER";
		else if(obj instanceof Integer)
			return "MEDIUMINT";
		else if(obj instanceof Boolean)
			return "BIT";
		else if(obj instanceof java.math.BigInteger)
			return "BIGINT";				
		else if(obj instanceof Float)
			return "FLOAT";
		else if(obj instanceof Double)
			return "DOUBLE";		
		else if(obj instanceof java.math.BigDecimal)
			return "DECIMAL";
		else if(obj instanceof java.sql.Date)
			return "DATE";
		else if(obj instanceof java.sql.Time)
			return "TIME";
		else if(obj instanceof java.sql.Timestamp)
			return "DATETIME";
		else throw new ExecException("Illegal type");
	}
	
}
