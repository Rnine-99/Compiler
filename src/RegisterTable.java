import java.util.HashMap;

public class RegisterTable {
    public RegisterTable parent;
    public HashMap<String, Register> map = new HashMap<>();

    public RegisterTable() {
        this.parent = null;
    }
}
