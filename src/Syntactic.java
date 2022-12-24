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
    public static int func_param_define;
    public static int if_stack = 0;
    public static ArrayList<String> constValue = null;
    public static boolean isIf = false;
    public static boolean isParamDefine = false;
    public static ArrayList<Boolean> expStack = new ArrayList<>();
    public static void CompUnit() throws IOException {
        expStack.add(false);
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
        if (!current_word.lexical_content.equals(")")) {
            isParamDefine = true;
            FuncFParams();
            isParamDefine = false;
        }
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
                        Compiler.llvmPrint("["+i.fun_dimension.get(k)+" x ", 1, true);
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
        } else
            fun_type_flag = 0;
        fun_if_return = 0;
        last_sentence = null;
        Compiler.llvmPrint(" {\n", 1, true);
        if_block_start = 1;
        Compiler.newLabelRegister();
        if_block_start = 0;
        for (int i = 0;i < current_func.fun_param.size();i ++) {
            if (current_func.fun_param.get(i).dimension != 1)
                continue;
            Register paramRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
            Compiler.llvmPrint("%"+paramRegister.registerNumber+" = alloca i32\n", stage, true);
            Compiler.llvmPrint("store i32 %"+
                    current_func.fun_param.get(i).register.registerNumber+
                    ", i32* %"+paramRegister.registerNumber+"\n", stage, true);
            paramRegister.isParam = true;
            current_func.fun_param.get(i).register = paramRegister;
        }
        Block();
        if ((fun_if_return == 0 || !last_sentence.lexical_content.equals("return")) && fun_type_flag == 0) {
            Compiler.newLabelRegister();
            Compiler.llvmPrint("ret void\n", stage, true);
        }
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
        Compiler.newLabelRegister();
        if_block_start = 0;
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
        String tempValue;
        if (Compiler.currentSymbolTable.map.get(current_word.lexical_content) != null)
            Compiler.error_analysis('b', current_word.lexical_line);
        else {
            Compiler.add_word_to_symbol_table(current_word, "const", "int");
            const_var = Compiler.search_symbol_table(current_word);
        }
        Ident();
        while (!current_word.lexical_content.equals("=")) {
            assert const_var != null;
            if (!current_word.lexical_content.equals("["))
                ERROR();
            else {
                Compiler.print_word(current_word);
                temp_dimension ++;
            }
            expStack.add(true);
            tempValue = ConstExp();
            expStack.remove(expStack.size() - 1);
            if (tempValue.contains("%")) {
                const_var.dimensionValue.add(Compiler.currentRegisterTable.map.get(tempValue).value.get(0));
            } else {
                const_var.dimensionValue.add(Integer.parseInt(tempValue));
            }
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
                // 全局常量
                if (temp_dimension == 1)
                    Compiler.llvmPrint("@"+const_var.word.lexical_content+" = dso_local constant i32 ", stage, true);
                else {
                    Compiler.llvmPrint("@" + const_var.word.lexical_content + " = dso_local constant ", stage, true);
                    for (Integer integer : const_var.dimensionValue) {
                        Compiler.llvmPrint("[" + integer + " x ", 1, true);
                    }
                    Compiler.llvmPrint("i32", 1, true);
                    for (int i = const_var.dimensionValue.size() - 1; i >= 0; i--)
                        Compiler.llvmPrint("]", 1, true);
                    Compiler.llvmPrint(" ", stage, true);
                }
            } else {
                // 非全局常量
                if (temp_dimension == 1)
                    Compiler.llvmPrint("%"+Compiler.currentRegisterTable.map.size()+" = alloca i32\n", stage, true);
                else if (temp_dimension == 2) {
                    Compiler.llvmPrint("%" + Compiler.currentRegisterTable.map.size() + " = alloca ", stage, true);
                    for (Integer integer : const_var.dimensionValue) {
                        Compiler.llvmPrint("[" + integer + " x ", 1, true);
                    }
                    Compiler.llvmPrint("i32", 1, true);
                    for (int i = const_var.dimensionValue.size() - 1; i >= 0; i--)
                        Compiler.llvmPrint("]\n", 1, true);
                    //Compiler.llvmPrint();
                } else {
                    Compiler.llvmPrint("%" + Compiler.currentRegisterTable.map.size() + " = alloca ", stage, true);
                    for (Integer integer : const_var.dimensionValue) {
                        Compiler.llvmPrint("[" + integer + " x ", 1, true);
                    }
                    Compiler.llvmPrint("i32", 1, true);
                    for (int i = const_var.dimensionValue.size() - 1; i >= 0; i--)
                        Compiler.llvmPrint("]", 1, true);
                    Compiler.llvmPrint("\n", 1, true);
                }
            }
            const_var.register = Compiler.newRegister(const_var);
        }
        constValue = new ArrayList<>();
        String returnValue = ConstInitVal();
        if (const_var != null) {
            if (stage == 1) {
                if (temp_dimension == 1) {
                    if (!returnValue.contains("%")) {
                        Compiler.llvmPrint(returnValue + "\n", stage, true);
                        const_var.register.value.add(Integer.parseInt(returnValue));
                    } else {
                        Register tempRegister = Compiler.currentRegisterTable.map.get(returnValue);
                        Compiler.llvmPrint(tempRegister.value.get(0) + "\n", stage, true);
                        const_var.register.value = tempRegister.value;
                    }
                } else if (temp_dimension == 2){
                    Compiler.llvmPrint("[", stage, true);
                    ArrayList<Integer> transferConstValue = new ArrayList<>();
                    for (String i : constValue) {
                        if (i.contains("%")) {
                            transferConstValue.add(Compiler.currentRegisterTable.map.get(i).value.get(0));
                        } else {
                            transferConstValue.add(Integer.parseInt(i));
                        }
                    }
                    for (int i = 0;i < constValue.size();i ++) {
                        if (i != 0) {
                            Compiler.llvmPrint(", ", stage, true);
                        }
                        Compiler.llvmPrint("i32 "+transferConstValue.get(i), stage, true);
                    }
                    Compiler.llvmPrint("]\n", stage, true);
                    const_var.register.value = transferConstValue;
                } else {
                    Compiler.llvmPrint("[", stage, true);
                    int b = const_var.dimensionValue.get(1);
                    ArrayList<Integer> transferConstValue = new ArrayList<>();
                    for (String i : constValue) {
                        if (i.contains("%")) {
                            transferConstValue.add(Compiler.currentRegisterTable.map.get(i).value.get(0));
                        } else {
                            transferConstValue.add(Integer.parseInt(i));
                        }
                    }
                    for (int i = 0;i < const_var.dimensionValue.get(0);i ++) {
                        if (i != 0) {
                            Compiler.llvmPrint(", ", stage, true);
                        }
                        Compiler.llvmPrint("["+b+" x i32] [", stage, true);
                        for (int j = 0;j < b;j ++) {
                            if (j != 0) {
                                Compiler.llvmPrint(", ", stage, true);
                            }
                            Compiler.llvmPrint("i32 "+transferConstValue.get(i * b + j), stage, true);
                        }
                        Compiler.llvmPrint("]", stage, true);
                    }
                    Compiler.llvmPrint("]\n", stage, true);
                    const_var.register.value = transferConstValue;
                }
            } else {
                if (temp_dimension == 1) {
                    if (!returnValue.contains("%")) {
                        Compiler.llvmPrint("store i32 " + returnValue + ", i32* %" + const_var.register.registerNumber + "\n", stage, true);
                        const_var.register.value.add(Integer.parseInt(returnValue));
                    } else {
                        Register tempRegister = Compiler.currentRegisterTable.map.get(returnValue);
                        Compiler.llvmPrint("store i32 " + returnValue + ", i32* %" + const_var.register.registerNumber + "\n", stage, true);
                        const_var.register.value = tempRegister.value;
                    }
                } else if (temp_dimension == 2) {
                    Register arrayStart = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+arrayStart.registerNumber+" = getelementptr ["+const_var.dimensionValue.get(0)+" x i32], [" +
                            const_var.dimensionValue.get(0)+" x i32]* %"+const_var.register.registerNumber, stage, true);
                    for (int i = 0;i < temp_dimension;i ++) {
                        Compiler.llvmPrint(", i32 0", 1, true);
                    }
                    Compiler.llvmPrint("\n", 1, true);
                    Compiler.llvmPrint("store i32 "+constValue.get(0)+", i32* %"+arrayStart.registerNumber+"\n", stage, true);
                    for (int i = 1;i < constValue.size();i ++) {
                        Register arrayTemp = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                        Compiler.llvmPrint("%"+arrayTemp.registerNumber+" = getelementptr i32, i32* %"+arrayStart.registerNumber
                                +", i32 "+i+"\n", stage, true);
                        Compiler.llvmPrint("store i32 "+constValue.get(i)+", i32* %"+arrayTemp.registerNumber+"\n", stage, true);
                    }
                } else {
                    Register arrayStart = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+arrayStart.registerNumber+" = getelementptr ["+
                            const_var.dimensionValue.get(0)+" x "+"["+const_var.dimensionValue.get(1)+" x i32]], ["+
                            const_var.dimensionValue.get(0)+" x "+"["+const_var.dimensionValue.get(1)+" x i32]]* %"+
                            const_var.register.registerNumber, stage, true);
                    for (int i = 0;i < temp_dimension;i ++) {
                        Compiler.llvmPrint(", i32 0", 1, true);
                    }
                    Compiler.llvmPrint("\n", 1, true);
                    Compiler.llvmPrint("store i32 "+constValue.get(0)+", i32* %"+arrayStart.registerNumber+"\n", stage, true);
                    for (int i = 1;i < constValue.size();i ++) {
                        Register arrayTemp = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                        Compiler.llvmPrint("%"+arrayTemp.registerNumber+" = getelementptr i32, i32* %"+arrayStart.registerNumber
                                +", i32 "+i+"\n", stage, true);
                        Compiler.llvmPrint("store i32 "+constValue.get(i)+", i32* %"+arrayTemp.registerNumber+"\n", stage, true);
                    }
                }
            }
        }
        if (stage == 1) {
            const_var.register.isGlobal = true;
        }
        Compiler.print_syntactic("<ConstDef>");
    }

    public static void VarDef() throws IOException {
        Symbol var = null;
        int temp_dimension = 1;
        String tempValue;
        if (Compiler.currentSymbolTable.map.get(current_word.lexical_content) != null)
            Compiler.error_analysis('b', current_word.lexical_line);
        else {
            Compiler.add_word_to_symbol_table(current_word, "var", "int");
            var = Compiler.search_symbol_table(current_word);
        }
        Ident();
        while (current_word.lexical_content.equals("[")) {
            assert var != null;
            temp_dimension ++;
            Compiler.print_word(current_word);
            expStack.add(true);
            tempValue = ConstExp();
            expStack.remove(expStack.size() - 1);
            if (tempValue.contains("%")) {
                var.dimensionValue.add(Compiler.currentRegisterTable.map.get(tempValue).value.get(0));
            } else {
                var.dimensionValue.add(Integer.parseInt(tempValue));
            }
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
                } else {
                    Compiler.llvmPrint("@"+var.word.lexical_content+" = dso_local global ", stage, true);
                    for (Integer integer : var.dimensionValue) {
                        Compiler.llvmPrint("[" + integer + " x ", 1, true);
                    }
                    Compiler.llvmPrint("i32", 1, true);
                    for (int i = var.dimensionValue.size() - 1;i >= 0;i --)
                        Compiler.llvmPrint("]", 1, true);
                    Compiler.llvmPrint(" ", 1, true);
                }
            } else {
                if (temp_dimension == 1) {
                    Compiler.llvmPrint("%"+Compiler.currentRegisterTable.map.size()+" = alloca i32\n", stage, true);
                } else {
                    Compiler.llvmPrint("%"+Compiler.currentRegisterTable.map.size()+" = alloca ", stage, true);
                    for (Integer integer : var.dimensionValue) {
                        Compiler.llvmPrint("[" + integer + " x ", 1, true);
                    }
                    Compiler.llvmPrint("i32", 1, true);
                    for (int i = var.dimensionValue.size() - 1;i >= 0;i --)
                        Compiler.llvmPrint("]", 1, true);
                    Compiler.llvmPrint("\n", 1, true);
                }
            }
            var.register = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
        }
        if (!current_word.lexical_content.equals("=")) {
            if (stage == 1) {
                if (temp_dimension == 1)
                    Compiler.llvmPrint("0\n", stage, true);
                else
                    Compiler.llvmPrint(" zeroinitializer\n", stage, true);
            }
            Compiler.print_syntactic("<VarDef>");
            return;
        } else {
            Compiler.print_word(current_word);
        }
        constValue = new ArrayList<>();
        int buffer = Compiler.bufferFlag - 1;
        String temp = InitVal();
        if (Compiler.bufferFlag - 1 > buffer && stage == 1) {
            Compiler.buffer.subList(buffer + 1, Compiler.bufferFlag).clear();
            Compiler.bufferFlag = buffer + 1;
        }
        if (stage == 1) {
            //目前函数体内声明常量主要问题：如何对表达式的值进行计算
            if (temp_dimension == 1) {
                if (!temp.contains("%")) {
                    Compiler.llvmPrint(temp + "\n", stage, true);
                    assert var != null;
                    var.register.value.add(Integer.parseInt(temp));
                } else {
                    Register returnRegister = Compiler.currentRegisterTable.map.get(temp);
                    Compiler.llvmPrint(returnRegister.value.get(0) + "\n", stage, true);
                    assert var != null;
                    var.register.value = returnRegister.value;
                }
            } else if (temp_dimension == 2) {
                Compiler.llvmPrint("[", stage, true);
                ArrayList<Integer> transferConstValue = new ArrayList<>();
                for (String i : constValue) {
                    if (i.contains("%")) {
                        transferConstValue.add(Compiler.currentRegisterTable.map.get(i).value.get(0));
                    } else {
                        transferConstValue.add(Integer.parseInt(i));
                    }
                }
                for (int i = 0;i < constValue.size();i ++) {
                    if (i != 0) {
                        Compiler.llvmPrint(", ", stage, true);
                    }
                    Compiler.llvmPrint("i32 "+transferConstValue.get(i), stage, true);
                }
                Compiler.llvmPrint("]\n", stage, true);
                var.register.value = transferConstValue;
            } else {
                Compiler.llvmPrint("[", stage, true);
                int b = var.dimensionValue.get(1);
                ArrayList<Integer> transferConstValue = new ArrayList<>();
                for (String i : constValue) {
                    if (i.contains("%")) {
                        transferConstValue.add(Compiler.currentRegisterTable.map.get(i).value.get(0));
                    } else {
                        transferConstValue.add(Integer.parseInt(i));
                    }
                }
                for (int i = 0;i < var.dimensionValue.get(0);i ++) {
                    if (i != 0) {
                        Compiler.llvmPrint(", ", stage, true);
                    }
                    Compiler.llvmPrint("["+b+" x i32] [", stage, true);
                    for (int j = 0;j < b;j ++) {
                        if (j != 0) {
                            Compiler.llvmPrint(", ", stage, true);
                        }
                        Compiler.llvmPrint("i32 "+transferConstValue.get(i * b + j), stage, true);
                    }
                    Compiler.llvmPrint("]", stage, true);
                }
                Compiler.llvmPrint("]\n", stage, true);
                var.register.value = transferConstValue;
            }
        } else {
            if (temp_dimension == 1) {
                assert var != null;
                Compiler.llvmPrint("store i32 "+temp+", i32* %"+var.register.registerNumber+"\n", stage, true);
                if (!temp.contains("%")) {
                    var.register.value.add(Integer.parseInt(temp));
                } else {
                    Register tempRegister = Compiler.currentRegisterTable.map.get(temp);
                    var.register.value = tempRegister.value;
                    var.register.isParam = tempRegister.isParam;
                }
            } else {
                Register arrayStart = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                if (temp_dimension == 2) {
                    Compiler.llvmPrint("%" + arrayStart.registerNumber + " = getelementptr [" + var.dimensionValue.get(0) + " x i32], [" +
                            var.dimensionValue.get(0) + " x i32]* %" + var.register.registerNumber, stage, true);
                } else {
                    Compiler.llvmPrint("%" + arrayStart.registerNumber + " = getelementptr [" + var.dimensionValue.get(0) + " x ["+var.dimensionValue.get(1)+" x i32]], [" +
                            var.dimensionValue.get(0) + " x ["+var.dimensionValue.get(1)+" x i32]]* %" + var.register.registerNumber, stage, true);
                }
                for (int i = 0;i < temp_dimension;i ++) {
                    Compiler.llvmPrint(", i32 0", 1, true);
                }
                Compiler.llvmPrint("\n", 1, true);
                Compiler.llvmPrint("store i32 "+constValue.get(0)+", i32* %"+arrayStart.registerNumber+"\n", stage, true);
                for (int i = 1;i < constValue.size();i ++) {
                    Register arrayTemp = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+arrayTemp.registerNumber+" = getelementptr i32, i32* %"+arrayStart.registerNumber
                    +", i32 "+i+"\n", stage, true);
                    Compiler.llvmPrint("store i32 "+constValue.get(i)+", i32* %"+arrayTemp.registerNumber+"\n", stage, true);
                }
            }
        }
        if (stage == 1) {
            var.register.isGlobal = true;
        }
        Compiler.print_syntactic("<VarDef>");
    }

    public static void FuncFParam() throws IOException {
        int temp_dimension = 1, temp_line;
        String returnValue = null;
        Symbol param = null;
        ArrayList<Integer> dimensionValue = new ArrayList<>();
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
            //dimensionValue.add()
            while (current_word.lexical_content.equals("[")) {
                temp_dimension ++;
                Compiler.print_word(current_word);
                /*
                if (current_word.lexical_type.equals("INTCON") && param != null) {
                    param.fun_dimension.add(Integer.valueOf(current_word.lexical_content));
                    param.dimensionValue.add(Integer.valueOf(current_word.lexical_content));
                }
                 */
                expStack.add(true);
                returnValue = ConstExp();
                expStack.remove(expStack.size() - 1);
                if (param != null) {
                    if (!returnValue.contains("%")) {
                        param.fun_dimension.add(Integer.parseInt(returnValue));
                        param.dimensionValue.add(Integer.parseInt(returnValue));
                    } else {
                        param.fun_dimension.add(Compiler.currentRegisterTable.map.get(returnValue).value.get(0));
                        param.dimensionValue.add(Compiler.currentRegisterTable.map.get(returnValue).value.get(0));
                    }
                }
                if (!current_word.lexical_content.equals("]")) {
                    temp_line = get_previous_line();
                    Compiler.error_analysis('k', temp_line);
                }
                else
                    Compiler.print_word(current_word);
            }
        }
        for (String i : Compiler.currentRegisterTable.map.keySet()) {
            if (i.contains("%"))
                Compiler.currentRegisterTable.map.remove(i);
        }
        if (param != null) {
            param.dimension = temp_dimension;
            param.register = Compiler.newRegister(param);
            param.register.isParam = true;
            param.register.isDirectParam = true;
        }
        current_func.fun_param.add(param);
        Compiler.print_syntactic("<FuncFParam>");
    }

    public static void BlockItem() throws IOException {
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
                returnValue = ConstInitVal();
                if (returnValue.contains("%")) {
                    constValue.add(returnValue);
                } else if (!returnValue.equals("array")) {
                    constValue.add(returnValue);
                }
                while (current_word.lexical_content.equals(",")) {
                    Compiler.print_word(current_word);
                    returnValue = ConstInitVal();
                    if (returnValue.contains("%")) {
                        constValue.add(returnValue);
                    } else if (!returnValue.equals("array")) {
                        constValue.add(returnValue);
                    }
                }
                if (!current_word.lexical_content.equals("}"))
                    ERROR();
                else {
                    Compiler.print_word(current_word);
                    returnValue = "array";
                }
            } else {
                Compiler.print_word(current_word);
            }
        } else {
            expStack.add(true);
            returnValue = ConstExp();
            expStack.remove(expStack.size() - 1);
        }
        Compiler.print_syntactic("<ConstInitVal>");
        return returnValue;
    }

    public static void Stmt() throws IOException {
        int temp_line;
        String returnValue;
        if (current_word.lexical_content.equals("if")) {
            isIf = true;
            if_stack ++;
            int ifLabel, elseLabel, ifBr, elseBr = 0, elseFlag = 0;
            Compiler.print_word(current_word);
            if (!current_word.lexical_content.equals("("))
                ERROR();
            else
                Compiler.print_word(current_word);
            returnValue = Cond();
            Compiler.llvmPrint("br i1 "+returnValue+", label <if"+if_stack+">, label <else"+if_stack+">\n", stage, true);
            if (!current_word.lexical_content.equals(")")) {
                temp_line = get_previous_line();
                Compiler.error_analysis('j', temp_line);
            }
            else
                Compiler.print_word(current_word);
            Register tempLabel = Compiler.newLabelRegister();
            ifLabel = tempLabel.registerNumber;// if 主体
            for (int i = 0;i < Compiler.bufferFlag;i ++) {
                if (Compiler.buffer.get(i).contains("<if"+if_stack+">"))
                    Compiler.buffer.set(i,
                            Compiler.buffer.get(i).replace("<if"+if_stack+">", "%"+ifLabel));
            }
            Compiler.llvmPrint("\n; <label>:"+ifLabel+":\n", 1, true);
            Stmt();
            //ifBr = Compiler.bufferFlag;
            Compiler.llvmPrint("br label <main"+if_stack+">\n", stage, true);
            if (current_word.lexical_content.equals("else")) {
                elseFlag = 1;
                Compiler.print_word(current_word);
                tempLabel = Compiler.newLabelRegister();
                elseLabel = tempLabel.registerNumber;
                for (int i = 0;i < Compiler.bufferFlag;i ++) {
                    if (Compiler.buffer.get(i).contains("<else"+if_stack+">"))
                        Compiler.buffer.set(i,
                                Compiler.buffer.get(i).replace("<else"+if_stack+">", "%"+elseLabel));
                }
                Compiler.llvmPrint("\n; <label>:"+tempLabel.registerNumber+":\n", 1, true);
                Stmt();
                elseBr = Compiler.bufferFlag;
                Compiler.llvmPrint("br label <main"+if_stack+">\n", stage, true);
            }
            tempLabel = Compiler.newLabelRegister();
            int mainFlag = tempLabel.registerNumber;
            Compiler.llvmPrint("\n; <label>:"+mainFlag+":\n", 1, true);
            // 补全对应label
            for (int i = 0;i < Compiler.bufferFlag;i ++) {
                if (Compiler.buffer.get(i).contains("<main"+if_stack+">"))
                    Compiler.buffer.set(i,
                            Compiler.buffer.get(i).replace("<main"+if_stack+">", "%"+mainFlag));
            }
            //Compiler.buffer.set(ifBr, Compiler.buffer.get(ifBr)+tempLabel.registerNumber+"\n");
            if (elseFlag != 1)
                for (int i = 0;i < Compiler.bufferFlag;i ++) {
                    if (Compiler.buffer.get(i).contains("<else"+if_stack+">"))
                        Compiler.buffer.set(i,
                                Compiler.buffer.get(i).replace("<else"+if_stack+">", "%"+mainFlag));
                }
            if_stack --;
        } else if (current_word.lexical_content.equals("{")) {
            Compiler.new_symbol_table();
            Block();
            Compiler.currentSymbolTable = Compiler.currentSymbolTable.parent;
        } else if (current_word.lexical_content.equals("while")) {
            isIf = false;
            Compiler.print_word(current_word);
            if (!current_word.lexical_content.equals("("))
                ERROR();
            else
                Compiler.print_word(current_word);
            Register whileLabel = Compiler.newLabelRegister();
            Compiler.llvmPrint("br label %"+whileLabel.registerNumber+"\n", stage, true);
            Compiler.llvmPrint("\n; <label>:"+whileLabel.registerNumber+":\n", stage, true);
            returnValue = Cond();
            Compiler.llvmPrint("br i1 "+returnValue+", label <while"+while_block_flag+">, label <while_else"+while_block_flag+">\n", stage, true);
            if (!current_word.lexical_content.equals(")")) {
                temp_line = get_previous_line();
                Compiler.error_analysis('j', temp_line);
            }
            else
                Compiler.print_word(current_word);
            while_block_flag ++;
            Register while_main_register = Compiler.newLabelRegister();
            Compiler.llvmPrint("\n; <label>:"+while_main_register.registerNumber+":\n", stage, true);
            Stmt();
            Compiler.llvmPrint("br label <while_return"+while_block_flag+">\n", stage, true);
            Register return_register = Compiler.newLabelRegister();
            for (int i = 0;i < Compiler.bufferFlag;i ++) {
                if (Compiler.buffer.get(i).contains("<while_return"+while_block_flag+">"))
                    Compiler.buffer.set(i,
                            Compiler.buffer.get(i).replace("<while_return"+while_block_flag+">", "%"+return_register.registerNumber));
            }
            Compiler.llvmPrint("\n; <label>:"+return_register.registerNumber+":\n", stage, true);
            Compiler.llvmPrint("br label %"+whileLabel.registerNumber+"\n", stage, true);
            while_block_flag --;
            Register main_register = Compiler.newLabelRegister();
            Compiler.llvmPrint("\n; <label>:"+main_register.registerNumber+":\n", stage, true);

            for (int i = 0;i < Compiler.bufferFlag;i ++) {
                if (Compiler.buffer.get(i).contains("<while"+while_block_flag+">"))
                    Compiler.buffer.set(i,
                            Compiler.buffer.get(i).replace("<while"+while_block_flag+">", "%"+while_main_register.registerNumber));
            }
            for (int i = 0;i < Compiler.bufferFlag;i ++) {
                if (Compiler.buffer.get(i).contains("<while_cond"+while_block_flag+">"))
                    Compiler.buffer.set(i,
                            Compiler.buffer.get(i).replace("<while_cond"+while_block_flag+">", "%"+whileLabel.registerNumber));
            }
            for (int i = 0;i < Compiler.bufferFlag;i ++) {
                if (Compiler.buffer.get(i).contains("<while_else"+while_block_flag+">"))
                    Compiler.buffer.set(i,
                            Compiler.buffer.get(i).replace("<while_else"+while_block_flag+">", "%"+main_register.registerNumber));
            }
        } else if (current_word.lexical_content.equals("break") || current_word.lexical_content.equals("continue")) {
            if (current_word.lexical_content.equals("break")) {
                Register tempLabel = Compiler.newLabelRegister();
                Compiler.llvmPrint("br label <while_else"+(while_block_flag - 1)+">\n", stage, true);
                Compiler.llvmPrint("\n; <label>:"+tempLabel.registerNumber+":\n", stage, true);
            }
            else {
                Register tempLabel = Compiler.newLabelRegister();
                Compiler.llvmPrint("br label <while_cond" + (while_block_flag - 1) + ">\n", stage, true);
                Compiler.llvmPrint("\n; <label>:"+tempLabel.registerNumber+":\n", stage, true);
            }
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
            String expResult;
            Compiler.print_word(current_word);
            if (!current_word.lexical_content.equals(";")) {
                expStack.add(false);
                expResult = Exp();
                expStack.remove(expStack.size() - 1);
                Compiler.newLabelRegister();
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
                Compiler.newLabelRegister();
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
            String printf_word;
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
                expStack.add(false);
                returnValue = Exp();
                expStack.remove(expStack.size() - 1);
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
                expStack.add(false);
                Exp();
                expStack.remove(expStack.size() - 1);
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
                    expStack.add(false);
                    Exp();
                    expStack.remove(expStack.size() - 1);
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
                            if (!temp_type.register.isGlobal)
                                Compiler.llvmPrint("store i32 %"+newRegister.registerNumber+
                                    ", i32* %"+temp_type.register.registerNumber+"\n", stage, true);
                            else
                                Compiler.llvmPrint("store i32 %"+newRegister.registerNumber+
                                        ", i32* @"+temp_type.word.lexical_content+"\n", stage, true);
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
                        expStack.add(false);
                        returnValue = Exp();
                        expStack.remove(expStack.size() - 1);
                        if (temp_type != null)
                            if (temp_type.register.isGlobal && !temp_type.ifArrayUsed) {
                                Compiler.llvmPrint("store i32 "+returnValue+
                                        ", i32* @"+temp_type.word.lexical_content+"\n", stage, true);
                            }
                            else {
                                Compiler.llvmPrint("store i32 "+returnValue+
                                        ", i32* %"+temp_type.register.registerNumber+"\n", stage, true);
                            }
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
                expStack.add(false);
                Exp();
                expStack.remove(expStack.size() - 1);
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
                returnValue = InitVal();
                while (current_word.lexical_content.equals(",")) {
                    Compiler.print_word(current_word);
                    returnValue = InitVal();
                }
            }
            if (current_word.lexical_content.equals("}")) {
                Compiler.print_word(current_word);
            } else
                ERROR();
        } else {
            expStack.add(false);
            returnValue = Exp();
            expStack.remove(expStack.size() - 1);
            if (returnValue.contains("%")) {
                constValue.add(returnValue);
            } else {
                constValue.add(returnValue);
            }
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
            boolean leftParam = false, rightParam = false;
            if (!returnValue.contains("%"))
                left = Integer.parseInt(returnValue);
            else if (Compiler.currentRegisterTable.map.get(returnValue).isParam) {
                leftParam = true;
            } else if (Compiler.currentRegisterTable.map.get(returnValue).value.size() != 0) {
                left = Compiler.currentRegisterTable.map.get(returnValue).value.get(0);
            }
            if (!temp.contains("%"))
                right = Integer.parseInt(temp);
            else if (Compiler.currentRegisterTable.map.get(temp).isParam) {
                rightParam = true;
            } else if (Compiler.currentRegisterTable.map.get(temp).value.size() != 0) {
                right = Compiler.currentRegisterTable.map.get(temp).value.get(0);
            }
            boolean ifJoinParam = leftParam | rightParam;
            switch (operator) {
                case 1:
                    newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    if (stage != 1 && !isParamDefine)
                        Compiler.llvmPrint("%"+newRegister.registerNumber+" = add i32 "+returnValue+", "+temp+"\n", stage, on);
                    if (ifJoinParam)
                        newRegister.isParam = true;
                    else
                        newRegister.value.add(left + right);
                    //Compiler.llvmPrint("%"+newRegister.registerNumber+": "+newRegister.value+"\n", stage, on);
                    break;
                case 2:
                    newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    if (stage != 1 && !isParamDefine)
                        Compiler.llvmPrint("%"+newRegister.registerNumber+" = sub i32 "+returnValue+", "+temp+"\n", stage, on);
                    if (ifJoinParam)
                        newRegister.isParam = true;
                    else
                        newRegister.value.add(left - right);
                    //Compiler.llvmPrint("%"+newRegister.registerNumber+": "+newRegister.value+"\n", stage, on);
                    break;
            }
            returnValue = "%"+newRegister.registerNumber;
        }
        Compiler.print_syntactic("<AddExp>");
        return returnValue;
    }

    public static String Cond() throws IOException {
        String returnValue;
        returnValue = LOrExp();
        Compiler.print_syntactic("<Cond>");
        return returnValue;
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
            boolean leftParam = false, rightParam = false;
            if (!returnValue.contains("%"))
                left = Integer.parseInt(returnValue);
            else if (Compiler.currentRegisterTable.map.get(returnValue).isParam) {
                leftParam = true;
            } else if (Compiler.currentRegisterTable.map.get(returnValue).value.size() != 0) {
                left = Compiler.currentRegisterTable.map.get(returnValue).value.get(0);
            }
            if (!temp.contains("%"))
                right = Integer.parseInt(temp);
            else if (Compiler.currentRegisterTable.map.get(temp).isParam) {
                rightParam = true;
            } else if (Compiler.currentRegisterTable.map.get(temp).value.size() != 0) {
                right = Compiler.currentRegisterTable.map.get(temp).value.get(0);
            }
            boolean ifJoinParam = leftParam | rightParam;
            switch (operator) {
                case 1:
                    newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    if (stage != 1 && !isParamDefine)
                        Compiler.llvmPrint("%"+newRegister.registerNumber+" = mul i32 "+returnValue+", "+temp+"\n", stage, on);
                    if (ifJoinParam)
                        newRegister.isParam = true;
                    else
                        newRegister.value.add(left * right);
                    break;
                case 2:
                    newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    if (stage != 1 && !isParamDefine)
                        Compiler.llvmPrint("%"+newRegister.registerNumber+" = sdiv i32 "+returnValue+", "+temp+"\n", stage, on);
                    if (ifJoinParam)
                        newRegister.isParam = true;
                    else if (right != 0)
                        newRegister.value.add(left / right);
                    break;
                case 3:
                    newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    if (stage != 1 && !isParamDefine)
                        Compiler.llvmPrint("%"+newRegister.registerNumber+" = srem i32 "+returnValue+", "+temp+"\n", stage, on);
                    if (ifJoinParam)
                        newRegister.isParam = true;
                    else if (right != 0)
                        newRegister.value.add(left % right);
                    break;
            }
            returnValue = "%"+newRegister.registerNumber;
        }
        Compiler.print_syntactic("<MulExp>");
        return returnValue;
    }

    public static String LOrExp() throws IOException {
        String returnValue = null, tempValue = null;
        Register tempLabel = null, tempRegister = null;
        boolean on = stage != 1;
        returnValue = LAndExp();
        tempValue = returnValue;
        while (current_word.lexical_content.equals("||")) {
            if (!Compiler.buffer.get(Compiler.bufferFlag - 1).contains("icmp")) {
                tempRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                Compiler.llvmPrint("%"+tempRegister.registerNumber+" = icmp ne i32 "+tempValue+", 0\n", stage, on);
            }
            if (isIf)
                Compiler.llvmPrint("br i1 "+tempValue+", label <if"+if_stack+">, label <next lor>\n", stage, on);
            else
                Compiler.llvmPrint("br i1 "+tempValue+", label <while"+while_block_flag+">, label <next lor>\n", stage, on);
            Compiler.print_syntactic("<LOrExp>");
            Compiler.print_word(current_word);
            tempLabel = Compiler.newLabelRegister();
            for (int i = 0;i < Compiler.bufferFlag;i ++) {
                if (Compiler.buffer.get(i).contains("<next lor>"))
                    Compiler.buffer.set(i,
                            Compiler.buffer.get(i).replace("<next lor>", "%"+tempLabel.registerNumber));
            }
            //Compiler.buffer.set(Compiler.bufferFlag - 1,
            //        Compiler.buffer.get(Compiler.bufferFlag - 1).replace("<next lor>", "%"+tempLabel.registerNumber));
            Compiler.llvmPrint("\n; <label>:"+tempLabel.registerNumber+":\n", stage, on);
            tempValue = LAndExp();
            if (!Compiler.buffer.get(Compiler.bufferFlag - 1).contains("icmp")) {
                tempRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                Compiler.llvmPrint("%"+tempRegister.registerNumber+" = icmp ne i32 "+tempValue+", 0\n", stage, on);
                tempValue = "%"+tempRegister.registerNumber;
            }
        }
        for (int i = 0;i < Compiler.bufferFlag;i ++) {
            if (Compiler.buffer.get(i).contains("<next lor>"))
                Compiler.buffer.set(i,
                        Compiler.buffer.get(i).replace("<next lor>", "<else"+if_stack+">\n"));
        }
        Compiler.print_syntactic("<LOrExp>");
        return tempValue;
    }

    public static String UnaryExp() throws IOException {
        Symbol temp_func = null;
        String returnValue = null, operator = null;
        boolean on = stage != 1;
        if (current_word.lexical_content.equals("+") ||
        current_word.lexical_content.equals("-") ||
        current_word.lexical_content.equals("!")) {
            operator = UnaryOp();
            returnValue = UnaryExp();
            if ("-".equals(operator)) {
                if (returnValue.contains("%")) {
                    Register tempRegister = Compiler.newTempRegister("%" + Compiler.currentRegisterTable.map.size());
                    if (stage == 1)
                        tempRegister.value.add(-Compiler.currentRegisterTable.map.get(returnValue).value.get(0));
                    if (stage != 1 && !isParamDefine)
                        Compiler.llvmPrint("%"+tempRegister.registerNumber+
                            " = sub i32 0, "+returnValue+"\n", stage, on);
                    returnValue = "%"+tempRegister.registerNumber;
                } else {
                    returnValue = String.valueOf(-Integer.parseInt(returnValue));
                }
            } else if ("!".equals(operator)) {
                if (returnValue.contains("%")) {
                    Register tempRegister = Compiler.newTempRegister("%" + Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+tempRegister.registerNumber+
                            " = icmp eq i32 "+returnValue+", 0\n", stage, on);
                    Register zeroRegister = Compiler.newTempRegister("%" + Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+zeroRegister.registerNumber+
                            " = zext i1 %"+tempRegister.registerNumber+" to i32\n", stage, on);
                    returnValue = "%"+zeroRegister.registerNumber;
                } else {
                    if (Integer.parseInt(returnValue) == 0) {
                        returnValue = "1";
                    } else {
                        returnValue = "0";
                    }
                }
            }
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
                            case 3:
                                Compiler.llvmPrint("["+ temp_func.fun_param.get(i).fun_dimension.get(0)+
                                        " x i32]* "+func_register.get(i), 1, true);
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
        Symbol temp_word, finalWord = null;
        int temp_dimension;
        String tempValue;
        Register tempRegister = null, valueRegister = null, baseRegister = null;
        if ((temp_word = Compiler.search_symbol_table(current_word)) == null) {
            Compiler.error_analysis('c', current_word.lexical_line);
            temp_dimension = -2;
        }
        else {
            finalWord = new Symbol(temp_word);
            finalWord.ifArrayUsed = false;
            temp_dimension = temp_word.dimension;
            baseRegister = temp_word.register;
        }
        Ident();
        while (current_word.lexical_content.equals("[")) {
            assert finalWord != null;
            if (temp_word != null) {
                temp_dimension --;
                finalWord.dimension --;
            }
            Compiler.print_word(current_word);
            expStack.add(false);
            tempValue = Exp();
            expStack.remove(expStack.size() - 1);
            if (tempValue.contains("%")) {
                valueRegister = Compiler.currentRegisterTable.map.get(tempValue);
            } else {
                valueRegister = new Register();
                valueRegister.value.add(Integer.parseInt(tempValue));
            }
            tempRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
            if (finalWord.register.isParam) {
                tempRegister.isParam = true;
            } else {
                if (temp_dimension == 2 && (stage == 1 || expStack.get(expStack.size() - 1))) {
                    int k = temp_word.dimensionValue.get(1);
                    int bound = valueRegister.value.get(0);
                    for (int j = 0; j < k; j++) {
                        tempRegister.value.add(finalWord.register.value.get(bound * k + j));
                    }
                } else if (temp_dimension == 1 && (stage == 1 || expStack.get(expStack.size() - 1))) {
                    tempRegister.value.add(finalWord.register.value.get(valueRegister.value.get(valueRegister.value.size() - 1)));
                }
            }
            if (stage != 1) {
                Compiler.llvmPrint("%" + tempRegister.registerNumber + " = getelementptr ", stage, true);
                for (int i = 0; i < temp_dimension; i++) {
                    if (finalWord.register.isParam && i >= temp_dimension - 1)
                        break;
                    Compiler.llvmPrint("[" + finalWord.dimensionValue.get(i) + " x ", 1, true);
                }
                Compiler.llvmPrint("i32", 1, true);
                for (int i = 0; i < temp_dimension; i++) {
                    if (finalWord.register.isParam && i >= temp_dimension - 1)
                        break;
                    Compiler.llvmPrint("]", 1, true);
                }
                Compiler.llvmPrint(", ", 1, true);
                for (int i = 0; i < temp_dimension; i++) {
                    if (finalWord.register.isParam && i >= temp_dimension - 1)
                        break;
                    Compiler.llvmPrint("[" + finalWord.dimensionValue.get(i) + " x ", 1, true);
                }
                Compiler.llvmPrint("i32", 1, true);
                for (int i = 0; i < temp_dimension; i++) {
                    if (finalWord.register.isParam && i >= temp_dimension - 1)
                        break;
                    Compiler.llvmPrint("]", 1, true);
                }
                assert baseRegister != null;
                if (finalWord.register.isParam)
                    if (temp_dimension == 1)
                        if (valueRegister.value.size() == 0 || tempValue.contains("%"))
                            Compiler.llvmPrint("* %" + baseRegister.registerNumber + ", i32 %" + valueRegister.registerNumber + "\n", 1, true);
                        else
                            Compiler.llvmPrint("* %" + baseRegister.registerNumber + ", i32 " + valueRegister.value.get(0) + "\n", 1, true);
                    else if (valueRegister.value.size() == 0 || tempValue.contains("%"))
                        Compiler.llvmPrint("* %" + baseRegister.registerNumber + ", i32 %" + valueRegister.registerNumber + ", i32 0\n", 1, true);
                    else
                        Compiler.llvmPrint("* %" + baseRegister.registerNumber + ", i32 " + valueRegister.value.get(0) + ", i32 0\n", 1, true);
                else if (finalWord.register.isGlobal && !finalWord.ifArrayUsed) {
                    if (valueRegister.value.size() == 0 || tempValue.contains("%"))
                        Compiler.llvmPrint("* @" + finalWord.word.lexical_content + ", i32 0, i32 %" + valueRegister.registerNumber + "\n", 1, true);
                    else
                        Compiler.llvmPrint("* @" + finalWord.word.lexical_content + ", i32 0, i32 " + valueRegister.value.get(0) + "\n", 1, true);
                } else {
                    if (valueRegister.value.size() == 0 || tempValue.contains("%"))
                        Compiler.llvmPrint("* %" + baseRegister.registerNumber + ", i32 0, i32 %" + valueRegister.registerNumber + "\n", 1, true);
                    else
                        Compiler.llvmPrint("* %" + baseRegister.registerNumber + ", i32 0, i32 " + valueRegister.value.get(0) + "\n", 1, true);
                }
            }
            if (!current_word.lexical_content.equals("]")) {
                int temp_line = get_previous_line();
                Compiler.error_analysis('k', temp_line);
            }
            else
                Compiler.print_word(current_word);
            baseRegister = tempRegister;
            finalWord.register = tempRegister;
            if (!finalWord.register.isParam && temp_dimension != 1)
                finalWord.dimensionValue.remove(0);
            finalWord.ifArrayUsed = true;
        }
        current_param_dimension = temp_dimension;
        Compiler.print_syntactic("<LVal>");
        return finalWord;
    }

    public static String PrimaryExp() throws IOException {
        String returnValue = null;
        Register returnRegister = null, tempRegister = null;
        Symbol lValSymbol = null;
        boolean on = stage != 1;
        switch (current_word.lexical_type) {
            case "LPARENT":
                Compiler.print_word(current_word);
                expStack.add(false);
                returnValue = Exp();
                expStack.remove(expStack.size() - 1);
                if (!current_word.lexical_content.equals(")")) {
                    int temp_line = get_previous_line();
                    Compiler.error_analysis('j', temp_line);
                } else {
                    Compiler.print_word(current_word);
                }
                break;
            case "IDENFR":
                lValSymbol = LVal();
                tempRegister = lValSymbol.register;
                // 应当对LVal返回为数组或其他类型进行判断是否需要load语句
                if (lValSymbol.dimension == 1) {
                    returnRegister = Compiler.newTempRegister("%" + Compiler.currentRegisterTable.map.size());
                    if (lValSymbol.register.isParam) {
                        returnRegister.isParam = true;
                    }
                    if (!isParamDefine) {
                        Compiler.llvmPrint("%" + returnRegister.registerNumber +
                                " = load i32, i32* ", stage, on);
                        if (!tempRegister.isGlobal)
                            Compiler.llvmPrint("%" + tempRegister.registerNumber + "\n", 1, on);
                        else
                            Compiler.llvmPrint("@" + lValSymbol.word.lexical_content + "\n", 1, on);
                    }
                    returnRegister.value = tempRegister.value;
                    returnValue = "%" + returnRegister.registerNumber;
                } else {
                    if (tempRegister.isParam) {
                        returnValue = "%"+tempRegister.registerNumber;
                    } else {
                        returnRegister = Compiler.newTempRegister("%" + Compiler.currentRegisterTable.map.size());
                        if (lValSymbol.register.isParam) {
                            returnRegister.isParam = true;
                        }
                        Compiler.llvmPrint("%" + returnRegister.registerNumber + " = getelementptr ", stage, true);
                        for (int i = 0; i < lValSymbol.dimension - 1; i++) {
                            Compiler.llvmPrint("[" + lValSymbol.dimensionValue.get(i) + " x ", 1, true);
                        }
                        Compiler.llvmPrint("i32", 1, true);
                        for (int i = 0; i < lValSymbol.dimension - 1; i++) {
                            Compiler.llvmPrint("]", 1, true);
                        }
                        Compiler.llvmPrint(", ", 1, true);
                        for (int i = 0; i < lValSymbol.dimension - 1; i++) {
                            Compiler.llvmPrint("[" + lValSymbol.dimensionValue.get(i) + " x ", 1, true);
                        }
                        Compiler.llvmPrint("i32", 1, true);
                        for (int i = 0; i < lValSymbol.dimension - 1; i++) {
                            Compiler.llvmPrint("]", 1, true);
                        }
                        if (tempRegister.isGlobal && !lValSymbol.ifArrayUsed)
                            Compiler.llvmPrint("* @" + lValSymbol.word.lexical_content + ", i32 0, i32 0" + "\n", 1, true);
                        else
                            Compiler.llvmPrint("* %" + tempRegister.registerNumber + ", i32 0, i32 0" + "\n", 1, true);
                        returnValue = "%" + returnRegister.registerNumber;
                    }
                }
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

    public static String LAndExp() throws IOException {
        String returnValue ,tempValue;
        Register tempLabel = null, tempRegister = null;
        boolean on = stage != 1;
        returnValue = EqExp();
        tempValue = returnValue;
        if (!Compiler.buffer.get(Compiler.bufferFlag - 1).contains("icmp")) {
            tempRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
            Compiler.llvmPrint("%"+tempRegister.registerNumber+" = icmp ne i32 "+tempValue+", 0\n", stage, true);
            returnValue = "%"+tempRegister.registerNumber;
            tempValue = returnValue;
        }
        while (current_word.lexical_content.equals("&&")) {
            if (isIf)
                Compiler.llvmPrint("br i1 "+tempValue+", label <next add>, label <next lor>\n", stage, true);
            else
                Compiler.llvmPrint("br i1 "+tempValue+", label <next add>, label <while_else"+while_block_flag+">\n", stage, true);
            Compiler.print_syntactic("<LAndExp>");
            Compiler.print_word(current_word);
            tempLabel = Compiler.newLabelRegister();
            Compiler.buffer.set(Compiler.bufferFlag - 1,
                    Compiler.buffer.get(Compiler.bufferFlag - 1).replace("<next add>", "%"+tempLabel.registerNumber));
            Compiler.llvmPrint("\n; <label>:"+tempLabel.registerNumber+":\n", stage, true);
            tempValue = EqExp();
            // 好像条件表达式不需要计算具体值
            if (!Compiler.buffer.get(Compiler.bufferFlag - 1).contains("icmp")) {
                tempRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                Compiler.llvmPrint("%"+tempRegister.registerNumber+" = icmp ne i32 "+tempValue+", 0\n", stage, true);
                tempValue = "%"+tempRegister.registerNumber;
            }
        }
        Compiler.print_syntactic("<LAndExp>");
        return tempValue;
    }

    public static String EqExp() throws IOException {
        String returnValue, tempValue;
        int operator = 0;
        boolean on = stage != 1;
        Register tempRegister = null;
        returnValue = RelExp();
        tempValue = returnValue;
        if (Compiler.buffer.get(Compiler.bufferFlag - 1).contains("icmp")) {
            tempRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
            Compiler.llvmPrint("%"+tempRegister.registerNumber+" = zext i1 "+tempValue+" to i32\n", stage, true);
            returnValue = "%"+tempRegister.registerNumber;
        }
        while (current_word.lexical_content.equals("==") ||
                current_word.lexical_content.equals("!=")) {
            switch (current_word.lexical_content) {
                case "==":
                    operator = 1;
                    break;
                case "!=":
                    operator = 2;
                    break;
            }
            Compiler.print_syntactic("<EqExp>");
            Compiler.print_word(current_word);
            tempValue = RelExp();
            Register newRegister = null;
            switch (operator) {
                case 1:
                    newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+newRegister.registerNumber+" = icmp eq i32 "+returnValue+", "+tempValue+"\n", stage, on);
                    tempRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+tempRegister.registerNumber+" = zext i1 %"+newRegister.registerNumber+" to i32\n", stage, true);
                    break;
                case 2:
                    newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+newRegister.registerNumber+" = icmp ne i32 "+returnValue+", "+tempValue+"\n", stage, on);
                    tempRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+tempRegister.registerNumber+" = zext i1 %"+newRegister.registerNumber+" to i32\n", stage, true);
                    break;
            }
            returnValue = "%"+tempRegister.registerNumber;
            //RelExp();
        }
        Compiler.print_syntactic("<EqExp>");
        return returnValue;
    }

    public static String RelExp() throws IOException {
        String returnValue;
        int operator = 0;
        returnValue = AddExp();
        boolean on = stage != 1;
        while (current_word.lexical_content.equals("<") ||
                current_word.lexical_content.equals(">") ||
                current_word.lexical_content.equals("<=") ||
                current_word.lexical_content.equals(">=")) {
            switch (current_word.lexical_content) {
                case "<":
                    operator = 1;
                    break;
                case ">":
                    operator = 2;
                    break;
                case "<=":
                    operator = 3;
                    break;
                case ">=":
                    operator = 4;
                    break;
            }
            Compiler.print_syntactic("<RelExp>");
            Compiler.print_word(current_word);
            String tempValue = AddExp();
            Register newRegister = null, tempRegister = null;
            switch (operator) {
                case 1:
                    newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+newRegister.registerNumber+" = icmp slt i32 "+returnValue+", "+tempValue+"\n", stage, on);
                    tempRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+tempRegister.registerNumber+" = zext i1 %"+newRegister.registerNumber+" to i32\n", stage, true);
                    break;
                case 2:
                    newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+newRegister.registerNumber+" = icmp sgt i32 "+returnValue+", "+tempValue+"\n", stage, on);
                    tempRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+tempRegister.registerNumber+" = zext i1 %"+newRegister.registerNumber+" to i32\n", stage, true);
                    break;
                case 3:
                    newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+newRegister.registerNumber+" = icmp sle i32 "+returnValue+", "+tempValue+"\n", stage, on);
                    tempRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+tempRegister.registerNumber+" = zext i1 %"+newRegister.registerNumber+" to i32\n", stage, true);
                    break;
                case 4:
                    newRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+newRegister.registerNumber+" = icmp sge i32 "+returnValue+", "+tempValue+"\n", stage, on);
                    tempRegister = Compiler.newTempRegister("%"+Compiler.currentRegisterTable.map.size());
                    Compiler.llvmPrint("%"+tempRegister.registerNumber+" = zext i1 %"+newRegister.registerNumber+" to i32\n", stage, true);
                    break;
            }
            returnValue = "%"+tempRegister.registerNumber;
        }
        Compiler.print_syntactic("<RelExp>");
        return returnValue;
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
                    callPutChar(printf_param.get(temp_num ++), 1);
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
                callPutChar(String.valueOf(10), 2);
                i ++;
                continue;
            }
            callPutChar(String.valueOf((int)temp.charAt(i)), 2);
        }
        return temp_num;
    }

    public static void callPutChar(String word, int type) throws IOException {
        int temp = 0;
        if (!word.contains("%")) {
            temp = Integer.parseInt(word);
        }
        if (type == 2)
            Compiler.llvmPrint("call void @putch(i32 "+temp+")\n", stage, true);
        else
            Compiler.llvmPrint("call void @putint(i32 "+word+")\n", stage, true);
    }

    public static String UnaryOp() throws IOException {
        String operator = null;
        if (!(current_word.lexical_content.equals("+") ||
                current_word.lexical_content.equals("-") ||
                current_word.lexical_content.equals("!")))
            ERROR();
        else {
            operator = current_word.lexical_content;
            Compiler.print_word(current_word);
        }
        Compiler.print_syntactic("<UnaryOp>");
        return operator;
    }

    public static void FuncRParams(Symbol temp_func) throws IOException {
        ArrayList<Integer> func_param = new ArrayList<>();
        String returnValue = null;
        int params_num = 0;
        expStack.add(false);
        returnValue = Exp();
        expStack.remove(expStack.size() - 1);
        if (current_param_dimension != 0) {
            func_param.add(current_param_dimension);
            func_register.add(returnValue);
            params_num ++;
        }
        while (current_word.lexical_content.equals(",")) {
            Compiler.print_word(current_word);
            expStack.add(false);
            returnValue = Exp();
            expStack.remove(expStack.size() - 1);
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
