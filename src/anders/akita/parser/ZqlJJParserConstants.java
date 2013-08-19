/* Generated By:JavaCC: Do not edit this line. ZqlJJParserConstants.java */
package anders.akita.parser;

public interface ZqlJJParserConstants {

  int EOF = 0;
  int K_ALL = 5;
  int K_AND = 6;
  int K_ANY = 7;
  int K_AS = 8;
  int K_ASC = 9;
  int K_AVG = 10;
  int K_BETWEEN = 11;
  int K_BINARY_INTEGER = 12;
  int K_BOOLEAN = 13;
  int K_BY = 14;
  int K_CASE = 15;
  int K_CHAR = 16;
  int K_COMMENT = 17;
  int K_COMMIT = 18;
  int K_CONNECT = 19;
  int K_COUNT = 20;
  int K_DATE = 21;
  int K_DAY = 22;
  int K_DELETE = 23;
  int K_DESC = 24;
  int K_DISTINCT = 25;
  int K_DIV = 26;
  int K_ELSE = 27;
  int K_END = 28;
  int K_EXCLUSIVE = 29;
  int K_EXISTS = 30;
  int K_EXIT = 31;
  int K_FLOAT = 32;
  int K_FOR = 33;
  int K_FALSE = 34;
  int K_FROM = 35;
  int K_GROUP = 36;
  int K_HAVING = 37;
  int K_HOUR = 38;
  int K_IN = 39;
  int K_INSERT = 40;
  int K_INTEGER = 41;
  int K_INTERSECT = 42;
  int K_INTERVAL = 43;
  int K_INTO = 44;
  int K_IS = 45;
  int K_JOIN = 46;
  int K_LEFT = 47;
  int K_LIKE = 48;
  int K_LOCK = 49;
  int K_MAX = 50;
  int K_MIN = 51;
  int K_MOD = 52;
  int K_MINUS = 53;
  int K_MINUTE = 54;
  int K_MODE = 55;
  int K_MONTH = 56;
  int K_NATURAL = 57;
  int K_NOT = 58;
  int K_NOWAIT = 59;
  int K_NULL = 60;
  int K_NUMBER = 61;
  int K_OF = 62;
  int K_ON = 63;
  int K_ONLY = 64;
  int K_OR = 65;
  int K_ORDER = 66;
  int K_OUTER = 67;
  int K_PRIOR = 68;
  int K_QUARTER = 69;
  int K_QUIT = 70;
  int K_READ = 71;
  int K_REAL = 72;
  int K_RIGHT = 73;
  int K_REGEXP = 74;
  int K_RLIKE = 75;
  int K_ROLLBACK = 76;
  int K_ROW = 77;
  int K_SECOND = 78;
  int K_SELECT = 79;
  int K_SET = 80;
  int K_SHARE = 81;
  int K_SMALLINT = 82;
  int K_SOME = 83;
  int K_START = 84;
  int K_SUM = 85;
  int K_TABLE = 86;
  int K_THEN = 87;
  int K_TRANSACTION = 88;
  int K_TRUE = 89;
  int K_UNION = 90;
  int K_UPDATE = 91;
  int K_VALUES = 92;
  int K_VARCHAR2 = 93;
  int K_VARCHAR = 94;
  int K_WEEK = 95;
  int K_WHEN = 96;
  int K_WHERE = 97;
  int K_WITH = 98;
  int K_WORK = 99;
  int K_WRITE = 100;
  int K_XOR = 101;
  int K_YEAR = 102;
  int S_NUMBER = 103;
  int FLOAT = 104;
  int INTEGER = 105;
  int DIGIT = 106;
  int LINE_COMMENT = 110;
  int S_IDENTIFIER = 111;
  int LETTER = 112;
  int SPECIAL_CHARS = 113;
  int S_CHAR_LITERAL = 114;

  int DEFAULT = 0;
  int WithinComment = 1;

  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\t\"",
    "\"\\r\"",
    "\"\\n\"",
    "\"ALL\"",
    "\"AND\"",
    "\"ANY\"",
    "\"AS\"",
    "\"ASC\"",
    "\"AVG\"",
    "\"BETWEEN\"",
    "\"BINARY_INTEGER\"",
    "\"BOOLEAN\"",
    "\"BY\"",
    "\"CASE\"",
    "\"CHAR\"",
    "\"COMMENT\"",
    "\"COMMIT\"",
    "\"CONNECT\"",
    "\"COUNT\"",
    "\"DATE\"",
    "\"DAY\"",
    "\"DELETE\"",
    "\"DESC\"",
    "\"DISTINCT\"",
    "\"DIV\"",
    "\"ELSE\"",
    "\"END\"",
    "\"EXCLUSIVE\"",
    "\"EXISTS\"",
    "\"EXIT\"",
    "\"FLOAT\"",
    "\"FOR\"",
    "\"FALSE\"",
    "\"FROM\"",
    "\"GROUP\"",
    "\"HAVING\"",
    "\"HOUR\"",
    "\"IN\"",
    "\"INSERT\"",
    "\"INTEGER\"",
    "\"INTERSECT\"",
    "\"INTERVAL\"",
    "\"INTO\"",
    "\"IS\"",
    "\"JOIN\"",
    "\"LEFT\"",
    "\"LIKE\"",
    "\"LOCK\"",
    "\"MAX\"",
    "\"MIN\"",
    "\"MOD\"",
    "\"MINUS\"",
    "\"MINUTE\"",
    "\"MODE\"",
    "\"MONTH\"",
    "\"NATURAL\"",
    "\"NOT\"",
    "\"NOWAIT\"",
    "\"NULL\"",
    "\"NUMBER\"",
    "\"OF\"",
    "\"ON\"",
    "\"ONLY\"",
    "\"OR\"",
    "\"ORDER\"",
    "\"OUTER\"",
    "\"PRIOR\"",
    "\"QUARTER\"",
    "\"QUIT\"",
    "\"READ\"",
    "\"REAL\"",
    "\"RIGHT\"",
    "\"REGEXP\"",
    "\"RLIKE\"",
    "\"ROLLBACK\"",
    "\"ROW\"",
    "\"SECOND\"",
    "\"SELECT\"",
    "\"SET\"",
    "\"SHARE\"",
    "\"SMALLINT\"",
    "\"SOME\"",
    "\"START\"",
    "\"SUM\"",
    "\"TABLE\"",
    "\"THEN\"",
    "\"TRANSACTION\"",
    "\"TRUE\"",
    "\"UNION\"",
    "\"UPDATE\"",
    "\"VALUES\"",
    "\"VARCHAR2\"",
    "\"VARCHAR\"",
    "\"WEEK\"",
    "\"WHEN\"",
    "\"WHERE\"",
    "\"WITH\"",
    "\"WORK\"",
    "\"WRITE\"",
    "\"XOR\"",
    "\"YEAR\"",
    "<S_NUMBER>",
    "<FLOAT>",
    "<INTEGER>",
    "<DIGIT>",
    "\"/*\"",
    "\"*/\"",
    "<token of kind 109>",
    "<LINE_COMMENT>",
    "<S_IDENTIFIER>",
    "<LETTER>",
    "\"_\"",
    "<S_CHAR_LITERAL>",
    "\"(\"",
    "\",\"",
    "\")\"",
    "\";\"",
    "\":\"",
    "\"=\"",
    "\".\"",
    "\"+\"",
    "\"-\"",
    "\"*\"",
    "\".*\"",
    "\"||\"",
    "\"&&\"",
    "\"!=\"",
    "\"<=>\"",
    "\"<>\"",
    "\">\"",
    "\">=\"",
    "\"<\"",
    "\"<=\"",
    "\"?\"",
    "\"|\"",
    "\"&\"",
    "\"<<\"",
    "\">>\"",
    "\"/\"",
    "\"%\"",
    "\"^\"",
    "\"~\"",
    "\"!\"",
  };

}
