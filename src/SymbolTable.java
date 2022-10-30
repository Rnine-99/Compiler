import java.util.HashMap;

public class SymbolTable {
    public HashMap<String, Symbol> map = new HashMap<>();
    public SymbolTable parent;

    public SymbolTable() {
        parent = null;
    }
}
