import java.util.ArrayList;

public class Symbol {
    public Lexical word;
    public ArrayList<Symbol> fun_param = new ArrayList<>();
    public String type;
    // 总体类型如const, var, func等
    public String var_type;
    // 返回的具体数据类型如int, void等
    public int dimension;
    public ArrayList<Integer> dimensionValue = new ArrayList<>();
    public ArrayList<Integer> fun_dimension = new ArrayList<>();
    public Register register;

    public Symbol(Lexical word, String type, String var_type) {
        this.word = word;
        this.type = type;
        this.var_type = var_type;
    }

    public Symbol(Symbol temp_word) {
        this.word = temp_word.word;
        this.type = temp_word.type;
        this.var_type = temp_word.var_type;
        this.register = temp_word.register;
        this.dimensionValue = new ArrayList<>(temp_word.dimensionValue);
        this.dimension = temp_word.dimension;
        this.fun_param = new ArrayList<>(temp_word.fun_param);
        this.fun_dimension = new ArrayList<>(temp_word.fun_dimension);
    }
}
