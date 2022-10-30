import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class Compiler {
    public static OutputStream output;
    public static OutputStreamWriter writer;
    public static ArrayList<Lexical> inputFile = new ArrayList<>();
    public static StringBuilder tempWord = new StringBuilder();
    public static int current_word = 0;
    public static SymbolTable currentSymbolTable = new SymbolTable();

    public static void add_new_word(String word, String type, int line) {
        if (word.equals("") || word.equals("//") || word.equals("/*") || type.equals("NULL"))
            return;
        switch (type) {
            case "IDENFR":
                inputFile.add(new Lexical(Lexical.lexical_type(word), word, line));
                break;
            case "SIGNAL":
            case "DOUBLE_SIGNAL":
                String temp_signal = Lexical.signal_type(word);
                if (!temp_signal.equals("ERROR_SIGNAL"))
                    inputFile.add(new Lexical(temp_signal, word, line));
                break;
            case "COMMENT_RELATED":
                String temp_type = Lexical.signal_type(word);
                inputFile.add(new Lexical(temp_type, word, line));
                break;
            default:
                inputFile.add(new Lexical(type, word, line));
                break;
        }
        tempWord = new StringBuilder();
    }

    public static void lexical_analysis() throws IOException {
        InputStream testFile = Files.newInputStream(Paths.get("testfile.txt"));
        int line = 1, flag = 0, previous_read = 0, ch = 0;
        HashMap<Integer, String> type_map = new HashMap<>();
        Lexical.map_init(type_map);
        while (true) {
            if (previous_read == 0)
                ch = testFile.read();
            if (ch == -1) {
                add_new_word(tempWord.toString(), type_map.get(flag), line);
                break;
            }
            if (flag == 7) {
                if (ch == '\n' || ch == '\r') {
                    flag = 0;
                    if (ch == '\n')
                        line ++;
                    tempWord = new StringBuilder();
                }
                continue;
            } else if (flag == 8) {
                /*
                if (ch == '\n' || ch == '\r')
                    line ++;

                 */
                if (ch == '\n')
                    line ++;
                if (ch == '*') {
                    ch = testFile.read();
                    if (ch == '/') {
                        flag = 0;
                        tempWord = new StringBuilder();
                        previous_read = 0;
                    } else
                        previous_read = 1;
                } else
                    previous_read = 0;
                continue;
            } else if (flag == 3) {
                tempWord.append((char) ch);
                if (ch == '"') {
                    add_new_word(tempWord.toString(), type_map.get(flag), line);
                    flag = 0;
                }
                continue;
            }
            if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
                add_new_word(tempWord.toString(), type_map.get(flag), line);
                flag = 0;
                //if (ch == '\n' || ch == '\r')
                  //  line++;
                if (ch == '\n')
                    line ++;
            } else if (Lexical.isComment(ch)) {
                if (flag != 6) {
                    add_new_word(tempWord.toString(), type_map.get(flag), line);
                    flag = 6;
                }
                tempWord.append((char) ch);
                if (tempWord.toString().equals("//"))
                    flag = 7;
                else if (tempWord.toString().equals("/*"))
                    flag = 8;
            } else if (Lexical.isAlpha(ch)) {
                if (flag != 1) {
                    add_new_word(tempWord.toString(), type_map.get(flag), line);
                    flag = 1;
                }
                tempWord.append((char) ch);
            } else if (Lexical.isDigit(ch)) {// 整数判断
                if (flag != 1 && flag != 2) {
                    add_new_word(tempWord.toString(), type_map.get(flag), line);
                    flag = 2;
                }
                tempWord.append((char) ch);
            } else if (ch == '"') {// 字符串判断
                add_new_word(tempWord.toString(), type_map.get(flag), line);
                flag = 3;
                tempWord.append((char) ch);
            } else if (Lexical.isSingleChar(ch)) {// 符号判断 P.S 现在还没有判断注释情况
                add_new_word(tempWord.toString(), type_map.get(flag), line);
                flag = 4;
                tempWord.append((char) ch);
            } else if (Lexical.isDoubleChar(ch)) {// 两个字符组成的符号判断
                if (flag != 5) {
                    add_new_word(tempWord.toString(), type_map.get(flag), line);
                    flag = 5;
                }
                tempWord.append((char) ch);
                if (tempWord.length() == 2) {
                    String temp_type = Lexical.signal_type(tempWord.toString());
                    if (temp_type.equals("ERROR_SIGNAL")) {
                        add_new_word(tempWord.substring(0, 1), type_map.get(flag), line);
                        add_new_word(tempWord.substring(1, 2), type_map.get(flag), line);
                    } else
                        add_new_word(tempWord.toString(), type_map.get(flag), line);
                }
            }
        }
        /*

        for (Lexical i : inputFile) {
            writer.append(i.lexical_type).append(" ").append(i.lexical_content).append("\n");
        }
        writer.close();
        output.close();

         */
    }

    public static void syntactic_analysis() throws IOException {
        output = Files.newOutputStream(Paths.get("error.txt"));
        writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        Syntactic.CompUnit();
        writer.close();
    }

    public static void current_word() {
        if (current_word >= inputFile.size()) {
            Syntactic.current_word = null;
            return;
        }
        Syntactic.current_word = inputFile.get(current_word);
        current_word ++;
    }

    public static void previous_word() {
        current_word -= 2;
        Compiler.current_word();
    }

    public static void print_word(Lexical x) throws IOException {
        //writer.append(x.lexical_type).append(" ").append(x.lexical_content).append("\n");
        current_word();
    }

    public static void print_syntactic(String x) throws IOException {
        //writer.append(x).append("\n");
    }

    public static void add_word_to_symbol_table(Lexical word, String type, String var_type) {
        currentSymbolTable.map.put(word.lexical_content, new Symbol(word, type, var_type));
    }

    public static void print_symbol(Lexical word) throws IOException {
        //writer.append(word.lexical_content).append("\n");
    }

    public static void new_symbol_table() throws IOException {
        SymbolTable temp = new SymbolTable();
        temp.parent = Compiler.currentSymbolTable;
        currentSymbolTable = temp;
    }

    public static Symbol search_symbol_table(Lexical word) {
        SymbolTable temp = currentSymbolTable;
        while (temp != null) {
            if (temp.map.containsKey(word.lexical_content))
                return temp.map.get(word.lexical_content);
            else
                temp = temp.parent;
        }
        return null;
    }

    public static void error_analysis(char error_code, int error_line) throws IOException {
        writer.append(String.valueOf(error_line)).append(" ").append(String.valueOf(error_code)).append("\n");
    }

    public static void main(String[] args) throws IOException {
        Compiler.lexical_analysis();
        Compiler.syntactic_analysis();
    }

}
