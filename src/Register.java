import java.util.ArrayList;

public class Register {
    public int registerNumber;
    public String label;
    public ArrayList<Integer> value = new ArrayList<>();
    public boolean isGlobal;
    public boolean isParam;
    public boolean isDirectParam;

    public Register(int size) {
        this.registerNumber = size;
    }

    public Register(int registerNumber, String label) {
        this.registerNumber = registerNumber;
        this.label = label;
        this.isParam = false;
        this.isDirectParam = false;
    }

    public Register() {
        this.isParam = false;
    }
}
