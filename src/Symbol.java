import llvm.Register;

import java.util.ArrayList;

public class Symbol {
    public Lexical word;
    public ArrayList<Symbol> fun_param = new ArrayList<>();
    public String type;
    // 总体类型如const, var, func等
    public String var_type;
    // 返回的具体数据类型如int, void等
    public int dimension;
    public ArrayList<Integer> fun_dimension = new ArrayList<>();
    public Register register;

    public Symbol(Lexical word, String type, String var_type) {
        this.word = word;
        this.type = type;
        this.var_type = var_type;
    }
}
