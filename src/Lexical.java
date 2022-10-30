import java.util.ArrayList;
import java.util.HashMap;

public class Lexical {
    public String lexical_type;
    public String lexical_content;
    public int lexical_line;
    public Lexical(String lexical_type, String lexical_content, int lexical_line) {
        this.lexical_type = lexical_type;
        this.lexical_content = lexical_content;
        this.lexical_line = lexical_line;
    }

    public Lexical() {

    }

    public static boolean isAlpha(int ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_';
    }

    public static boolean isDigit(int ch) {
        return ch >= '0' && ch <= '9';
    }

    public static boolean isSingleChar(int ch) {
        return ch == '+' || ch == '-' || ch == '%' || ch == ';' || ch == ','
                || ch == '(' || ch == ')' || ch == '[' || ch == ']' || ch == '{' || ch == '}';
    }
    public static boolean isDoubleChar(int ch) {
        return ch == '!' || ch == '&' || ch == '=' || ch == '<' || ch == '>' || ch == '|';
    }

    public static boolean isComment(int ch) {
        return ch == '/' || ch == '*';
    }

    public static String lexical_type(String word) {
        switch (word) {
            case "main":
                return "MAINTK";
            case "const":
                return "CONSTTK";
            case "int":
                return "INTTK";
            case "break":
                return "BREAKTK";
            case "continue":
                return "CONTINUETK";
            case "if":
                return "IFTK";
            case "else":
                return "ELSETK";
            case "while":
                return "WHILETK";
            case "getint":
                return "GETINTTK";
            case "printf":
                return "PRINTFTK";
            case "return":
                return "RETURNTK";
            case "void":
                return "VOIDTK";
            default:
                return "IDENFR";
        }
    }

    public static String signal_type(String word) {
        switch (word) {
            case "!":
                return "NOT";
            case "&&":
                return "AND";
            case "||":
                return "OR";
            case "+":
                return "PLUS";
            case "-":
                return "MINU";
            case "*":
                return "MULT";
            case "/":
                return "DIV";
            case "%":
                return "MOD";
            case "<":
                return "LSS";
            case "<=":
                return "LEQ";
            case ">":
                return "GRE";
            case ">=":
                return "GEQ";
            case "==":
                return "EQL";
            case "!=":
                return "NEQ";
            case "=":
                return "ASSIGN";
            case ";":
                return "SEMICN";
            case ",":
                return "COMMA";
            case "(":
                return "LPARENT";
            case ")":
                return "RPARENT";
            case "[":
                return "LBRACK";
            case "]":
                return "RBRACK";
            case "{":
                return "LBRACE";
            case "}":
                return "RBRACE";
            default:
                return "ERROR_SIGNAL";
        }
    }

    public static void map_init(HashMap<Integer, String> map) {
        map.put(0, "NULL");
        map.put(1, "IDENFR");
        map.put(2, "INTCON");
        map.put(3, "STRCON");
        map.put(4, "SIGNAL");
        map.put(5, "DOUBLE_SIGNAL");
        map.put(6, "COMMENT_RELATED");
    }
}
