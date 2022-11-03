import java.io.IOException;
import java.util.ArrayList;

public class Syntactic {
    public static Lexical current_word;
    public static Symbol current_func;
    public static Lexical current_statement;
    public static int current_param_dimension;
    public static int fun_type_flag = 0;
    public static int fun_if_return = 0;
    public static int while_block_flag = 0;
    public static Lexical last_sentence = null;
    public static int block_layers = 0;
    // 该变量用于监控当前阶段，即{Decl}, {FuncDef}, MainFuncDef
    public static int stage = 1;
    public static int if_block_start = 1;
    public static ArrayList<String> printf_param = null;
    public static ArrayList<String> func_register = null;
    public static ArrayList<ArrayList<String>> func_stack = new ArrayList<>();
    public static void CompUnit() throws IOException {
        Compiler.llvmPrint("declare i32 @getint()\n"+
                "declare void @putint(i32)\n"+
                "declare void @putch(i32)\n\n", stage, true);
        Compiler.current_word();
        while (true) {
            Compiler.current_word();
            Compiler.current_word();
            if (current_word.lexical_content.equals("(")) {
                Compiler.previous_word();
                Compiler.previous_word();
                break;
            }
            Compiler.previous_word();
            Compiler.previous_word();
            Decl();
        }
        stage ++;
        Compiler.llvmPrint("\n", 1, true);
        while (true) {
            Compiler.current_word();
            if (current_word.lexical_content.equals("main")) {
                Compiler.previous_word();
                break;
            }
            Compiler.previous_word();
            FuncDef();
        }
        stage ++;
        Compiler.llvmPrint("\n", 1, true);
        MainFuncDef();
        Compiler.print_syntactic("<CompUnit>");
    }

    public static void Decl() throws IOException {
        if (current_word.lexical_content.equals("const")) {
            ConstDecl();
        } else {
            VarDecl();
        }
    }

    public static void FuncDef() throws IOException {
        String fun_type = FuncType();
        if (Compiler.currentSymbolTable.map.get(current_word.lexical_content) != null)
            Compiler.error_analysis('b', current_word.lexical_line);
        else {
            Compiler.add_word_to_symbol_table(current_word, "func", fun_type);
        }
        current_func = Compiler.search_symbol_table(current_word);
        Compiler.llvmPrint(" @"+current_word.lexical_content, 1, true);
        Ident();
        Compiler.new_symbol_table();
        Compiler.newRegisterTable();
        if (!current_word.lexical_content.equals("(")) {
            ERROR();
        } else {
            Compiler.llvmPrint(current_word.lexical_content, 1, true);
            Compiler.print_word(current_word);
        }
        if (!current_word.lexical_content.equals(")"))
            FuncFParams();
        for (int j = 0;j < current_func.fun_param.size();j ++) {
            Symbol i = current_func.fun_param.get(j);
            if (j != 0)
                Compiler.llvmPrint(" ,", 1, true);
            switch (i.dimension) {
                case 1:
                    Compiler.llvmPrint("i32", 1, true);
                    break;
                case 2:
                    Compiler.llvmPrint("i32*", 1, true);
                    break;
                default:
                    for (int k = i.dimension - 3;k >= 0;k --) {
                        Compiler.llvmPrint("["+current_func.fun_dimension.get(k)+"x", 1, true);
                    }
                    Compiler.llvmPrint("i32", 1, true);
                    for (int k = i.dimension - 3;k >= 0;k --) {
                        Compiler.llvmPrint("]", 1, true);
                    }
                    Compiler.llvmPrint("*", 1, true);
                    break;
            }
        }
        if (!current_word.lexical_content.equals(")")) {
            int temp_line = get_previous_line();
            Compiler.error_analysis('j', temp_line);
        } else {
            Compiler.llvmPrint(current_word.lexical_content, 1, true);
            Compiler.print_word(current_word);
        }
        if (fun_type.equals("int")) {
            fun_type_flag = 1;
            current_func.dimension = 1;
            fun_if_return = 0;
        } else
            fun_type_flag = 0;
        last_sentence = null;
        Compiler.llvmPrint(" {\n", 1, true);
        if_block_start = 1;
        Block();
        Compiler.llvmPrint("}\n", 1, true);
        Compiler.previous_word();
        if (fun_type_flag == 1 && (last_sentence == null || !last_sentence.lexical_content.equals("return")))
            Compiler.error_analysis('g', current_word.lexical_line);
        Compiler.current_word();
        Compiler.print_syntactic("<FuncDef>");
        Compiler.currentSymbolTable = Compiler.currentSymbolTable.parent;
        Compiler.currentRegisterTable = Compiler.currentRegisterTable.parent;
    }

    public static void MainFuncDef() throws IOException {
        if (!current_word.lexical_content.equals("int")) {
            ERROR();
        } else {
            Compiler.print_word(current_word);
        }
        Compiler.add_word_to_symbol_table(current_word, "func", "int");
        current_func = Compiler.search_symbol_table(current_word);
        Compiler.llvmPrint("define dso_local i32 @main() {\n", 1, true);
        if (!current_word.lexical_content.equals("main")) {
            ERROR();
        } else {
            Compiler.print_word(current_word);
        }
        if (!current_word.lexical_content.equals("(")) {
            ERROR();
        } else {
            Compiler.print_word(current_word);
        }
        if (!current_word.lexical_content.equals(")")) {
            int temp_line = get_previous_line();
            Compiler.error_analysis('j', temp_line);
        } else {
            Compiler.print_word(current_word);
        }
        fun_type_flag = 1;
        fun_if_return = 0;
        last_sentence = null;
        Compiler.new_symbol_table();
        Compiler.newRegisterTable();
        if_block_start = 1;
        Block();
        Compiler.llvmPrint("}\n", 1, true);
        Compiler.previous_word();
        Compiler.current_word();
        if (fun_type_flag == 1 && (last_sentence == null || !last_sentence.lexical_content.equals("return")))
            Compiler.error_analysis('g', current_word.lexical_line);
        Compiler.current_word();
        Compiler.print_syntactic("<MainFuncDef>");
        Compiler.currentSymbolTable = Compiler.currentSymbolTable.parent;
        Compiler.currentRegisterTable = Compiler.currentRegisterTable.parent;
    }

    public static void ConstDecl() throws IOException {
        if (!current_word.lexical_content.equals("const")) {
            ERROR();
        } else {
            Compiler.print_word(current_word);
        }
        BType();
        ConstDef();
        while (true) {
            if (current_word.lexical_content.equals(",")) {
                Compiler.print_word(current_word);
                ConstDef();
            } else
                break;
        }
        Compiler.previous_word();
        int temp_line = current_word.lexical_line;
        Compiler.current_word();
        if (!current_word.lexical_content.equals(";")) {
            Compiler.error_analysis('i', temp_line);
        } else {
            Compiler.print_word(current_word);
        }
        Compiler.print_syntactic("<ConstDecl>");
    }

    public static void VarDecl() throws IOException {
        BType();
        VarDef();
        while (true) {
            if (current_word.lexical_content.equals(",")) {
                Compiler.print_word(current_word);
                VarDef();
            } else
                break;
        }
        Compiler.previous_word();
        int temp_line = current_word.lexical_line;
        Compiler.current_word();
        if (!current_word.lexical_content.equals(";")) {
            Compiler.error_analysis('i', temp_line);
        } else {
            Compiler.print_word(current_word);
        }
        Compiler.print_syntactic("<VarDecl>");
    }

    public static String FuncType() throws IOException {
        String temp = current_word.lexical_content;
        if (!(current_word.lexical_content.equals("void") || current_word.lexical_content.equals("int")))
            ERROR();
        else {
            if (current_word.lexical_content.equals("void"))
                Compiler.llvmPrint("define dso_local "+temp, 1, true);
            else
                Compiler.llvmPrint("define dso_local "+"i32", 1, true);
            Compiler.print_word(current_word);
        }
        Compiler.print_syntactic("<FuncType>");
        return temp;
    }

    public static void Ident() throws IOException {
        if (!current_word.lexical_type.equals("IDENFR"))
            ERROR();
        else
            Compiler.print_word(current_word);
    }

    public static void FuncFParams() throws IOException {
        FuncFParam();
        while (current_word.lexical_content.equals(",")) {
            Compiler.print_word(current_word);
            FuncFParam();
        }
        Compiler.print_syntactic("<FuncFParams>");
    }

    public static void Block() throws IOException {
        block_layers ++;
        if (!current_word.lexical_content.equals("{")) {
            ERROR();
        } else {
            Compiler.print_word(current_word);
        }
        while (!current_word.lexical_content.equals("}")) {
            BlockItem();
        }
        if (block_layers != 1)
            last_sentence = current_word;
        block_layers --;
        Compiler.print_word(current_word);
        Compiler.print_syntactic("<Block>");
    }

    public static void BType() throws IOException {
        if (!current_word.lexical_content.equals("int"))
            ERROR();
        else
            Compiler.print_word(current_word);
    }

    public static void ConstDef() throws IOException {
        Symbol const_var = null;
        int temp_dimension = 1;
        if (Compiler.currentSymbolTable.map.get(current_word.lexical_content) != null)
            Compiler.error_analysis('b', current_word.lexical_line);
        else {
            Compiler.add_word_to_symbol_table(current_word, "const", "int");
            const_var = Compiler.search_symbol_table(current_word);
        }
        Ident();
        while (!current_word.lexical_content.equals("=")) {
            if (!current_word.lexical_content.equals("["))
                ERROR();
            else {
                Compiler.print_word(current_word);
                temp_dimension ++;
            }
            ConstExp();
            if (!current_word.lexical_content.equals("]")) {
                int temp_line = get_previous_line();
                Compiler.error_analysis('k', temp_line);
            }
            else
                Compiler.print_word(current_word);
        }
        if (const_var != null)
            const_var.dimension = temp_dimension;
        Compiler.print_word(current_word);
        // 此处常量变量定义未考虑数组情况（懒
        // 如果常量定义初始化是个计算式？
        if (const_var != null) {
            if (stage == 1) {
                if (temp_dimension == 1)
                    Compiler.llvmPrint("@"+const_var.word.lexical_content+" = dso_local constant i32 ", stage, true);
                else if (temp_dimension == 2)
                    Compiler.llvmPrint("@"+const_var.word.lexical_content+" = dso_local constant i32* ", stage, true);
            } else {
                if (temp_dimension == 1)
                    Compiler.llvmPrint("%"+Compiler.currentRegisterTable.map.size()+" = alloca i32\n", stage, true);
                else if (temp_dimension == 2)
                    Compiler.llvmPrint("%"+Compiler.currentRegisterTable.map.size()+" = alloca i32*\n", stage, true);
                //Compiler.llvmPrint();
            }
            const_var.register = Compiler.newRegister(const_var);
        }
        String constValue = ConstInitVal();
        if (const_var != null) {
            if (stage == 1) {
                if (!constValue.contains("%")) {
                    Compiler.llvmPrint(constValue+"\n", stage, true);
                    const_var.register.value = Integer.parseInt(constValue);
                } else {
                    Register tempRegister = Compiler.currentRegisterTable.map.get(constValue);
                    Compiler.llvmPrint(tempRegister.value+"\n", stage, true);
                    const_var.register.value = tempRegister.value;
                }
            } else {
                if (temp_dimension == 1) {
                    if (!constValue.contains("%")) {
                        Compiler.llvmPrint("store i32 " + constValue + ", i32* %" + const_var.register.registerNumber + "\n", stage, true);
                        const_var.register.value = Integer.parseInt(constValue);
                    } else {
                        Register tempRegister = Compiler.currentRegisterTable.map.get(constValue);
                        Compiler.llvmPrint("store i32 " + tempRegister.value + ", i32* %" + const_var.register.registerNumber + "\n", stage, true);
                        const_var.register.value = tempRegister.value;
                    }
                }
            }
        }
        Compiler.print_syntactic("<ConstDef>");
    }

    public static void VarDef() throws IOException {
        Symbol var = null;
        int temp_dimension = 1;
        if (Compiler.currentSymbolTable.map.get(current_word.lexical_content) != null)
            Compiler.error_analysis('b', current_word.lexical_line);
        else {
            Compiler.add_word_to_symbol_table(current_word, "var", "int");
            var = Compiler.search_symbol_table(current_word);
        }
        Ident();
        while (current_word.lexical_content.equals("[")) {
            temp_dimension ++;
            Compiler.print_word(current_word);
            ConstExp();
            if (!current_word.lexical_content.equals("]")) {
                int temp_line = get_previous_line();
                Compiler.error_analysis('k', temp_line);
            }
            else
                Compiler.print_word(current_word);
        }
        if (var != null) {
            var.dimension = temp_dimension;
            if (stage == 1) {
                if (temp_dimension == 1) {
                    Compiler.llvmPrint("@"+var.word.lexical_content+" = dso_local global i32 ", stage, true);
                } else if (temp_dimension == 2) {
                    Compiler.llvmPrint("@"+var.word.lexical_content+" = dso_local global i32* ", stage, true);
                }
            } else {
                if (temp_dimension == 1) {
                    Compiler.llvmPrint("%"+Compiler.currentRegisterTable.map.size()+" = alloca i32\n", stage, true);
                } else if (temp_dimension == 2) {
                    Compiler.llvmPrint("%"+Compiler.currentRegisterTable.map.size()+" = alloca i32*\n", stage, true);
                }
            }
            var.register = Compiler.newRegister(var);
        }
        if (!current_word.lexical_content.equals("=")) {
            if (stage == 1) {
                Compiler.llvmPrint("0\n", stage, true);
            }
            Compiler.print_syntactic("<VarDef>");
            return;
        } else {
            Compiler.print_word(current_word);
        }
        String temp = InitVal();
        if (stage == 1) {
            //目前函数体内声明常量主要问题：如何对表达式的值进行计算
            if (!temp.contains("%")) {
                Compiler.llvmPrint(temp+"\n", stage, true);
                assert var != null;
                var.register.value = Integer.parseInt(temp);
            } else {
                Register returnRegister = Compiler.currentRegisterTable.map.get(temp);
                Compiler.llvmPrint(String.valueOf(returnRegister.value)+"\n", stage, true);
                assert var != null;
                var.register.value = returnRegister.value;
            }
        } else {
            if (temp_dimension == 1) {
                assert var != null;
                Compiler.llvmPrint("store i32 "+temp+", i32* %"+var.register.registerNumber+"\n", stage, true);
                if (!temp.contains("%")) {
                    var.register.value = Integer.parseInt(temp);
                } else {
                    Register tempRegister = Compiler.currentRegisterTable.map.get(temp);
                    var.register.value = tempRegister.value;
                }
            }
        }
        Compiler.print_syntactic("<VarDef>");
    }

    public static void FuncFParam() throws IOException {
        int temp_dimension = 1, temp_line;
        Symbol param = null;
        BType();
        if (Compiler.currentSymbolTable.map.get(current_word.lexical_content) != null)
            Compiler.error_analysis('b', current_word.lexical_line);
        else {
            Compiler.add_word_to_symbol_table(current_word, "var", "int");
            param = Compiler.search_symbol_table(current_word);
        }
        Ident();
        if (current_word.lexical_content.equals("[")) {
            temp_dimension ++;
            Compiler.print_word(current_word);
            if (!current_word.lexical_content.equals("]")) {
                temp_line = get_previous_line();
                Compiler.error_analysis('k', temp_line);
            }
            else {
                Compiler.print_word(current_word);
            }
            while (current_word.lexical_content.equals("[")) {
                temp_dimension ++;
                Compiler.print_word(current_word);
                if (current_word.lexical_type.equals("INTCON") && param != null)
                    param.fun_dimension.add(Integer.valueOf(current_word.lexical_content));
                ConstExp();
                if (!current_word.lexical_content.equals("]")) {
                    temp_line = get_previous_line();
                    Compiler.error_analysis('k', temp_line);
                }
                else
                    Compiler.print_word(current_word);
            }
        }
        if (param != null) {
            param.dimension = temp_dimension;
            param.register = Compiler.newRegister(param);
        }
        current_func.fun_param.add(param);
        Compiler.print_syntactic("<FuncFParam>");
    }

    public static void BlockItem() throws IOException {
        if (if_block_start == 1) {
            Compiler.newLabelRegister();
            if_block_start = 0;
        }
        if (current_word.lexical_content.equals("const") || current_word.lexical_content.equals("int")) {
            Decl();
        } else {
            last_sentence = current_word;
            Stmt();
        }
    }

    public static String ConstExp() throws IOException {
        String returnValue = AddExp();
        Compiler.print_syntactic("<ConstExp>");
        return returnValue;
    }

    public static String ConstInitVal() throws IOException {
        String returnValue = null;
        if (current_word.lexical_content.equals("{")) {
            Compiler.print_word(current_word);
            if (!current_word.lexical_content.equals("}")) {
                ConstInitVal();
                while (current_word.lexical_content.equals(",")) {
                    Compiler.print_word(current_word);
                    ConstInitVal();
                }
                if (!current_word.lexical_content.equals("}"))
                    ERROR();
                else
                    Compiler.print_word(current_word);
            } else {
                Compiler.print_word(current_word);
            }
        } else {
            returnValue = ConstExp();
        }
        Compiler.print_syntactic("<ConstInitVal>");
        return returnValue;
    }

    public static void Stmt() throws IOException {
        int temp_line;
        String returnValue = null;
        if (current_word.lexical_content.equals("if")) {
            Compiler.print_word(current_word);
            if (!current_word.lexical_content.equals("("))
                ERROR();
            else
                Compiler.print_word(current_word);
            Cond();
            if (!current_word.lexical_content.equals(")")) {
                temp_line = get_previous_line();
                Compiler.error_analysis('j', temp_line);
            }
            else
                Compiler.print_word(current_word);
            Stmt();
            if (current_word.lexical_content.equals("else")) {
                Compiler.print_word(current_word);
                Stmt();
            }
        } else if (current_word.lexical_content.equals("{")) {
            Compiler.new_symbol_table();
            Block();
            Compiler.currentSymbolTable = Compiler.currentSymbolTable.parent;
        } else if (current_word.lexical_content.equals("while")) {
            Compiler.print_word(current_word);
            if (!current_word.lexical_content.equals("("))
                ERROR();
            else
                Compiler.print_word(current_word);
            Cond();
            if (!current_word.lexical_content.equals(")")) {
                temp_line = get_previous_line();
                Compiler.error_analysis('j', temp_line);
            }
            else
                Compiler.print_word(current_word);
            while_block_flag ++;
            Stmt();
            while_block_flag --;
        } else if (current_word.lexical_content.equals("break") || current_word.lexical_content.equals("continue")) {
            if (while_block_flag == 0)
                Compiler.error_analysis('m', current_word.lexical_line);
            temp_line = current_word.lexical_line;
            Compiler.print_word(current_word);
            if (!current_word.lexical_content.equals(";") || current_word.lexical_line != temp_line)
                Compiler.error_analysis('i', temp_line);
            else
                Compiler.print_word(current_word);
        } else if (current_word.lexical_content.equals("return")) {
            int return_line = current_word.lexical_line;
            String expResult = null;
            Compiler.print_word(current_word);
            if (!current_word.lexical_content.equals(";")) {
                expResult = Exp();
                Compiler.llvmPrint("ret i32 "+expResult+"\n", stage, true);
                if (fun_type_flag == 0)
                    if (current_word.lexical_content.equals(";"))
                        Compiler.error_analysis('f', return_line);
                    else {
                        Compiler.error_analysis('i', return_line);
                        Compiler.print_syntactic("<Stmt>");
                        return;
                    }
            } else if (fun_type_flag == 0) {
                Compiler.llvmPrint("ret void\n", stage, true);
            }
            fun_if_return = 1;
            if (!current_word.lexical_content.equals(";") || current_word.lexical_line != return_line)
                Compiler.error_analysis('i', return_line);
            else {
                Compiler.print_word(current_word);
            }
        } else if (current_word.lexical_content.equals("printf")) {
            int temp_exp_num = 0, format_string_line, printf_line, format_string_num;
            String printf_word = null;
            printf_param = new ArrayList<>();
            printf_line = current_word.lexical_line;
            Compiler.print_word(current_word);
            if (!current_word.lexical_content.equals("("))
                ERROR();
            else
                Compiler.print_word(current_word);
            format_string_num = FormatString_num();
            format_string_line = current_word.lexical_line;
            printf_word = current_word.lexical_content;
            FormatString();
            while (current_word.lexical_content.equals(",")) {
                temp_exp_num ++;
                Compiler.print_word(current_word);
                returnValue = Exp();
                printf_param.add(returnValue);
            }
            llvmFormatString(printf_word);
            if (format_string_num == -1) {
                Compiler.error_analysis('a', format_string_line);
            } else if (temp_exp_num != format_string_num) {
                Compiler.error_analysis('l', printf_line);
            }
            temp_line = current_word.lexical_line;
            if (!current_word.lexical_content.equals(")")) {
                temp_line = get_previous_line();
                Compiler.error_analysis('j', temp_line);
            }
            else
                Compiler.print_word(current_word);
            if (!current_word.lexical_content.equals(";") || current_word.lexical_line != temp_line)
                Compiler.error_analysis('i', temp_line);
            else
                Compiler.print_word(current_word);
        } else if (current_word.lexical_type.equals("IDENFR")) {
            Compiler.current_word();
            // 函数调用

            if (current_word.lexical_content.equals("(")) {
                Compiler.previous_word();
                temp_line = current_word.lexical_line;
                Exp();
                if (!current_word.lexical_content.equals(";") || current_word.lexical_line != temp_line)
                    Compiler.error_analysis('i', temp_line);
                else {
                    Compiler.print_word(current_word);
                }
            } else {
                Compiler.previous_word();
                // 判断是否为Exp ";"
                int temp_current = Compiler.current_word;
                if (isExp()) {
                    Compiler.current_word = temp_current - 1;
                    Compiler.current_word();
                    Exp();
                } else {
                    temp_line = current_word.lexical_line;
                    Symbol temp_type = LVal();
                    Compiler.print_word(current_word);
                    if (temp_type != null && temp_type.type.equals("const"))
                        Compiler.error_analysis('h', temp_line);
                    if (current_word.lexical_content.equals("getint")) {
                        if (temp_type != null) {
                            Register newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                            Compiler.llvmPrint("%"+newRegister.registerNumber+
                                    " = call i32 @getint()\n", stage, true);
                            Compiler.llvmPrint("store i32 %"+newRegister.registerNumber+
                                    ", i32* %"+temp_type.register.registerNumber+"\n", stage, true);
                        }
                        Compiler.print_word(current_word);
                        if (!current_word.lexical_content.equals("("))
                            ERROR();
                        else
                            Compiler.print_word(current_word);
                        if (!current_word.lexical_content.equals(")")) {
                            temp_line = get_previous_line();
                            Compiler.error_analysis('j', temp_line);
                        }
                        else
                            Compiler.print_word(current_word);
                    } else {
                        returnValue = Exp();
                        if (temp_type != null)
                            Compiler.llvmPrint("store i32 "+returnValue+
                                ", i32* %"+temp_type.register.registerNumber+"\n", stage, true);
                    }
                }
                temp_line = get_previous_line();
                if (!current_word.lexical_content.equals(";") || current_word.lexical_line != temp_line)
                    Compiler.error_analysis('i', temp_line);
                else {
                    Compiler.print_word(current_word);
                }
            }
        } else {
            if (!current_word.lexical_content.equals(";")) {
                Exp();
                temp_line = get_previous_line();
                if (!current_word.lexical_content.equals(";") || current_word.lexical_line != temp_line)
                    Compiler.error_analysis('i', temp_line);
                else {
                    Compiler.print_word(current_word);
                }
            } else {
                Compiler.print_word(current_word);
            }
        }
        Compiler.print_syntactic("<Stmt>");
    }

    public static boolean isExp() {
        // 不应超过一行
        int temp_current = Compiler.current_word;
        int temp_line = current_word.lexical_line;
        while (!current_word.lexical_content.equals("=") && current_word.lexical_line == temp_line) {
            if (current_word.lexical_content.equals(";")) {
                Compiler.current_word = temp_current - 1;
                Compiler.current_word();
                return true;
            }
            Compiler.current_word();
        }
        Compiler.current_word = temp_current - 1;
        Compiler.current_word();
        return false;
    }

    public static String InitVal() throws IOException {
        String returnValue = null;
        if (current_word.lexical_content.equals("{")) {
            Compiler.print_word(current_word);
            if (!current_word.lexical_content.equals("}")) {
                InitVal();
                while (current_word.lexical_content.equals(",")) {
                    Compiler.print_word(current_word);
                    InitVal();
                }
            }
            if (current_word.lexical_content.equals("}")) {
                Compiler.print_word(current_word);
            } else
                ERROR();
        } else {
            returnValue = Exp();
        }
        Compiler.print_syntactic("<InitVal>");
        return returnValue;
    }

    public static int get_previous_line() {
        Compiler.previous_word();
        int temp_line = current_word.lexical_line;
        Compiler.current_word();
        return temp_line;
    }

    public static String AddExp() throws IOException {
        String returnValue;
        int operator = 0;
        returnValue = MulExp();
        boolean on = stage != 1;
        while (current_word.lexical_content.equals("+") || current_word.lexical_content.equals("-")) {
            switch (current_word.lexical_content) {
                case "+":
                    operator = 1;
                    break;
                case "-":
                    operator = 2;
                    break;
            }
            Compiler.print_syntactic("<AddExp>");
            Compiler.print_word(current_word);
            String temp = MulExp();
            Register newRegister = null;
            int left = 0, right = 0;
            if (!returnValue.contains("%"))
                left = Integer.parseInt(returnValue);
            else
                left = Compiler.currentRegisterTable.map.get(returnValue).value;
            if (!temp.contains("%"))
                right = Integer.parseInt(temp);
            else
                right = Compiler.currentRegisterTable.map.get(temp).value;
            switch (operator) {
                case 1:
                    newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+newRegister.registerNumber+" = add i32 "+returnValue+", "+temp+"\n", stage, on);
                    newRegister.value = left + right;
                    //Compiler.llvmPrint("%"+newRegister.registerNumber+": "+newRegister.value+"\n", stage, on);
                    break;
                case 2:
                    newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+newRegister.registerNumber+" = sub i32 "+returnValue+", "+temp+"\n", stage, on);
                    newRegister.value = left - right;
                    //Compiler.llvmPrint("%"+newRegister.registerNumber+": "+newRegister.value+"\n", stage, on);
                    break;
            }
            returnValue = "%"+newRegister.registerNumber;
        }
        Compiler.print_syntactic("<AddExp>");
        return returnValue;
    }

    public static void Cond() throws IOException {
        LOrExp();
        Compiler.print_syntactic("<Cond>");
    }

    public static String Exp() throws IOException {
        String returnValue = AddExp();
        Compiler.print_syntactic("<Exp>");
        return returnValue;
    }

    public static String MulExp() throws IOException {
        String returnValue;
        int operator = 0;
        returnValue = UnaryExp();
        boolean on = stage != 1;
        while (current_word.lexical_content.equals("*") || current_word.lexical_content.equals("/") ||
        current_word.lexical_content.equals("%")) {
            switch (current_word.lexical_content) {
                case "*":
                    operator = 1;
                    break;
                case "/":
                    operator = 2;
                    break;
                case "%":
                    operator = 3;
                    break;
            }
            Compiler.print_syntactic("<MulExp>");
            Compiler.print_word(current_word);
            String temp = UnaryExp();
            Register newRegister = null;
            int left = 0, right = 0;
            if (!returnValue.contains("%"))
                left = Integer.parseInt(returnValue);
            else
                left = Compiler.currentRegisterTable.map.get(returnValue).value;
            if (!temp.contains("%"))
                right = Integer.parseInt(temp);
            else
                right = Compiler.currentRegisterTable.map.get(temp).value;
            switch (operator) {
                case 1:
                    newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+newRegister.registerNumber+" = mul i32 "+returnValue+", "+temp+"\n", stage, on);
                    newRegister.value = left * right;
                    break;
                case 2:
                    newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+newRegister.registerNumber+" = div i32 "+returnValue+", "+temp+"\n", stage, on);
                    newRegister.value = left / right;
                    break;
                case 3:
                    newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+newRegister.registerNumber+" = mod i32 "+returnValue+", "+temp+"\n", stage, on);
                    newRegister.value = left % right;
                    break;
            }
            returnValue = "%"+newRegister.registerNumber;
        }
        Compiler.print_syntactic("<MulExp>");
        return returnValue;
    }

    public static void LOrExp() throws IOException {
        LAndExp();
        while (current_word.lexical_content.equals("||")) {
            Compiler.print_syntactic("<LOrExp>");
            Compiler.print_word(current_word);
            LAndExp();
        }
        Compiler.print_syntactic("<LOrExp>");
    }

    public static String UnaryExp() throws IOException {
        Symbol temp_func = null;
        String returnValue = null;
        if (current_word.lexical_content.equals("+") ||
        current_word.lexical_content.equals("-") ||
        current_word.lexical_content.equals("!")) {
            UnaryOp();
            UnaryExp();
        } else if (current_word.lexical_type.equals("IDENFR")) {
            int temp = Compiler.current_word;
            int flag = 0;
            Compiler.current_word();
            if (current_word.lexical_content.equals("(")) {
                // 函数调用
                if (func_register != null) {
                    func_stack.add(func_register);
                    flag = 1;
                }
                func_register = new ArrayList<>();
                Compiler.current_word = temp - 1;
                Compiler.current_word();
                Register funcRegister = null;
                // 或许应该在这里进行函数调用？
                if ((Compiler.search_symbol_table(current_word)) == null)
                    Compiler.error_analysis('c', current_word.lexical_line);
                else
                    temp_func = Compiler.search_symbol_table(current_word);
                current_statement = current_word;
                Compiler.print_word(current_word);
                Compiler.print_word(current_word);
                if (!current_word.lexical_content.equals(")")) {
                    if (temp_func != null) {
                        //returnValue = temp_func.word.lexical_content;
                        FuncRParams(temp_func);
                    } else {
                        while (!current_word.lexical_content.equals(")")) {
                            Compiler.current_word();
                        }
                    }
                    if (!current_word.lexical_content.equals(")")) {
                        int temp_line = get_previous_line();
                        Compiler.error_analysis('j', temp_line);
                    }
                    else {
                        Compiler.print_word(current_word);
                    }
                } else {
                    if (temp_func != null)
                        if (temp_func.fun_param.size() != 0) {
                            Compiler.error_analysis('d', Compiler.inputFile.get(temp).lexical_line);
                        }
                    Compiler.print_word(current_word);
                }
                if (temp_func != null) {
                    if (temp_func.var_type.equals("void"))
                        current_param_dimension = -1;
                    else
                        current_param_dimension = 1;
                    if (temp_func.var_type.equals("int")) {
                        funcRegister = Compiler.newTempRegister("%"+
                                Compiler.currentRegisterTable.map.size());
                        Compiler.llvmPrint("%"+funcRegister.registerNumber+
                                " = call i32 @"+temp_func.word.lexical_content+"(", stage, true);
                    } else {
                        Compiler.llvmPrint("call void @"+
                                temp_func.word.lexical_content+"(", stage, true);
                    }
                    for (int i = 0;i < func_register.size();i ++) {
                        if (i != 0) {
                            Compiler.llvmPrint(", ", 1, true);
                        }
                        switch (temp_func.fun_param.get(i).dimension) {
                            case 1:
                                Compiler.llvmPrint("i32 "+func_register.get(i), 1, true);
                                break;
                            case 2:
                                Compiler.llvmPrint("i32* "+func_register.get(i), 1, true);
                                break;
                        }
                    }
                    Compiler.llvmPrint(")\n", 1, true);
                }
                if (funcRegister != null) {
                    returnValue = "%"+funcRegister.registerNumber;
                }
                if (flag == 1) {
                    func_register = func_stack.get(func_stack.size() - 1);
                    func_stack.remove(func_stack.size() - 1);
                } else {
                    func_register = null;
                }
            } else {
                Compiler.previous_word();
                returnValue = PrimaryExp();
            }
        } else if (current_word.lexical_content.equals("(")) {
            returnValue = PrimaryExp();
        } else if (current_word.lexical_type.equals("INTCON")) {
            returnValue = PrimaryExp();
        }
        Compiler.print_syntactic("<UnaryExp>");
        return returnValue;
    }

    public static Symbol LVal() throws IOException {
        Symbol temp_word;
        int temp_dimension = 0;
        if ((temp_word = Compiler.search_symbol_table(current_word)) == null) {
            Compiler.error_analysis('c', current_word.lexical_line);
            temp_dimension = -2;
        }
        else
            temp_dimension = temp_word.dimension;
        Ident();
        while (current_word.lexical_content.equals("[")) {
            if (temp_word != null)
                temp_dimension --;
            Compiler.print_word(current_word);
            Exp();
            if (!current_word.lexical_content.equals("]")) {
                int temp_line = get_previous_line();
                Compiler.error_analysis('k', temp_line);
            }
            else
                Compiler.print_word(current_word);
        }
        current_param_dimension = temp_dimension;
        Compiler.print_syntactic("<LVal>");
        return temp_word;
    }

    public static String PrimaryExp() throws IOException {
        String returnValue = null;
        Register returnRegister = null, tempRegister = null;
        Symbol lvalSymbol = null;
        boolean on = stage != 1;
        switch (current_word.lexical_type) {
            case "LPARENT":
                Compiler.print_word(current_word);
                returnValue = Exp();
                if (!current_word.lexical_content.equals(")")) {
                    int temp_line = get_previous_line();
                    Compiler.error_analysis('j', temp_line);
                }
                else {
                    Compiler.print_word(current_word);
                }
                break;
            case "IDENFR":
                returnRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                lvalSymbol = LVal();
                tempRegister = lvalSymbol.register;
                Compiler.llvmPrint("%"+returnRegister.registerNumber+
                        " = load i32, i32* ", stage, on);
                if (!tempRegister.isGlobal)
                    Compiler.llvmPrint("%"+tempRegister.registerNumber+"\n", 1, on);
                else
                    Compiler.llvmPrint("@"+lvalSymbol.word.lexical_content+"\n", 1, on);
                returnRegister.value = tempRegister.value;
                returnValue = "%"+returnRegister.registerNumber;
                break;
            case "INTCON":
                returnValue = Number_Int();
                break;
        }
        Compiler.print_syntactic("<PrimaryExp>");
        return returnValue;
    }

    public static String Number_Int() throws IOException {
        String returnValue = null;
        if (!current_word.lexical_type.equals("INTCON"))
            ERROR();
        else {
            returnValue = current_word.lexical_content;
            Compiler.print_word(current_word);
        }
        current_param_dimension = 1;
        Compiler.print_syntactic("<Number>");
        return returnValue;
    }

    public static void LAndExp() throws IOException {
        EqExp();
        while (current_word.lexical_content.equals("&&")) {
            Compiler.print_syntactic("<LAndExp>");
            Compiler.print_word(current_word);
            EqExp();
        }
        Compiler.print_syntactic("<LAndExp>");
    }

    public static void EqExp() throws IOException {
        RelExp();
        while (current_word.lexical_content.equals("==") || current_word.lexical_content.equals("!=")) {
            Compiler.print_syntactic("<EqExp>");
            Compiler.print_word(current_word);
            RelExp();
        }
        Compiler.print_syntactic("<EqExp>");
    }

    public static void RelExp() throws IOException {
        AddExp();
        while (current_word.lexical_content.equals("<") ||
                current_word.lexical_content.equals(">") ||
                current_word.lexical_content.equals("<=") ||
                current_word.lexical_content.equals(">=")) {
            Compiler.print_syntactic("<RelExp>");
            Compiler.print_word(current_word);
            AddExp();
        }
        Compiler.print_syntactic("<RelExp>");
    }

    public static void FormatString() throws IOException {
        if (!current_word.lexical_type.equals("STRCON"))
            ERROR();
        else {
            Compiler.print_word(current_word);
        }
    }

    public static int FormatString_num() throws IOException {
        StringBuilder temp = new StringBuilder(current_word.lexical_content);
        int temp_num = 0;
        for (int i = 1;i < temp.length() - 1;i ++) {
            if (temp.charAt(i) == '%') {
                if (temp.charAt(i + 1) == 'd')
                    temp_num ++;
                else
                    return -1;
            } else if (temp.charAt(i) == 92 && temp.charAt(i + 1) != 'n')
                return -1;
            else if (temp.charAt(i) != 32 &&
            temp.charAt(i) != 33 &&
            !(temp.charAt(i) >= 40 && temp.charAt(i) <= 126))
                return -1;
        }
        return temp_num;
    }

    public static int llvmFormatString(String word) throws IOException {
        StringBuilder temp = new StringBuilder(word);
        int temp_num = 0;
        for (int i = 1;i < temp.length() - 1;i ++) {
            if (temp.charAt(i) == '%') {
                if (temp.charAt(i + 1) == 'd') {
                    callPutChar(printf_param.get(temp_num));
                    i ++;
                    continue;
                }
                else
                    return -1;
            } else if (temp.charAt(i) == 92 && temp.charAt(i + 1) != 'n')
                return -1;
            else if (temp.charAt(i) != 32 &&
                    temp.charAt(i) != 33 &&
                    !(temp.charAt(i) >= 40 && temp.charAt(i) <= 126))
                return -1;
            else if (temp.charAt(i) == 92 && temp.charAt(i + 1) == 'n') {
                callPutChar(String.valueOf(10));
                i ++;
                continue;
            }
            callPutChar(String.valueOf((int)temp.charAt(i)));
        }
        return temp_num;
    }

    public static void callPutChar(String word) throws IOException {
        int temp = 0;
        if (!word.contains("%")) {
            temp = Integer.parseInt(word);
        }
        if (!word.contains("%"))
            Compiler.llvmPrint("call void @putch i32 "+temp+"\n", stage, true);
        else
            Compiler.llvmPrint("call void @putint i32 "+word+"\n", stage, true);
    }

    public static void UnaryOp() throws IOException {
        if (!(current_word.lexical_content.equals("+") ||
                current_word.lexical_content.equals("-") ||
                current_word.lexical_content.equals("!")))
            ERROR();
        else
            Compiler.print_word(current_word);
        Compiler.print_syntactic("<UnaryOp>");
    }

    public static void FuncRParams(Symbol temp_func) throws IOException {
        ArrayList<Integer> func_param = new ArrayList<>();
        String returnValue = null;
        int params_num = 0;
        returnValue = Exp();
        if (current_param_dimension != 0) {
            func_param.add(current_param_dimension);
            func_register.add(returnValue);
            params_num ++;
        }
        while (current_word.lexical_content.equals(",")) {
            Compiler.print_word(current_word);
            returnValue = Exp();
            params_num ++;
            if (current_param_dimension != 0) {
                func_param.add(current_param_dimension);
                func_register.add(returnValue);
            }
        }
        if (params_num != temp_func.fun_param.size())
            Compiler.error_analysis('d', current_statement.lexical_line);
        else {
            for (int i = 0;i < params_num;i ++) {
                if (func_param.get(i) != -2 && temp_func.fun_param.get(i).dimension != func_param.get(i))
                    Compiler.error_analysis('e', current_word.lexical_line);
            }
        }
        Compiler.print_syntactic("<FuncRParams>");
    }

    public static void ERROR() {
        System.out.println("\nERROR at line "+current_word.lexical_line+"\n");
    }
}
