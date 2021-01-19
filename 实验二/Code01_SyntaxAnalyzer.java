import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 实验要求： (@ 代表 ε)
 * 1. 用户在一个TXT文件中输入一个二型文法
 *      E::= BA    A::= @|aA    B::= bB|a
 *      L(E) = {b*aa*}
 * 2. 系统读取该TXT文件，展示出每条文法右部符号串的First集
 *   （重复的只需展示1次）以及每个非终结符的Follow集。
 *   First(E)={b}  First(A)={/@,a} First(aA)={a}
 *   First(BA)={b}  First(@)={/@} First(aA)={a} First(bB)={b} First(b)={b}
 *   Follow(E)={#}  Follow(A)={#}  Follow(B)={a,#}
 *
 * 3. 展示出该文法所对应的LL(1)分析表
 * 4. 用户输入一个由终结符组成的字符串，
 *    系统展示出分析过程，并给出分析结论（该串“是”或“不是”文法的句子）
 */
public class Code01_SyntaxAnalyzer {
    private String src = "3_2.txt"; // 源TXT文件名称
    private ArrayList<String[]> rules = new ArrayList<>(); // 左规则和右规则
    private HashSet<String> Vt = new HashSet<>(); // 终结符集合
    private HashSet<String> Vn = new HashSet<>(); // 非终结符集合
    private HashMap<String, HashSet<String>> val_fir = new HashMap<>();
    private HashMap<String, HashSet<String>> val_fol = new HashMap<>();
    private Map<String, String> first = new HashMap<>(); // FIRST集合
    private Map<String, String> follow = new HashMap<>(); // FOLLOW集合
    private String[][] table; // LL(1)表

    public Code01_SyntaxAnalyzer() { }

    /**
     * X是否是终结符，如果是返回true
     * @param X
     * @return
     */
    public boolean isVt(String X) {
        Pattern patt1 = Pattern.compile("[^A-Z]");
        Matcher matcher = patt1.matcher(X);
        return matcher.matches();
    }

    /**
     * X是否是非终结符，如果是返回true
     * @param X
     * @return
     */
    public boolean isVn(String X) {
        Pattern patt = Pattern.compile("[A-Z]");
        Matcher matcher = patt.matcher(X);
        return matcher.matches();
    }

    /**
     * 把ch添加到Vt或者Vn中
     * @param ch
     */
    public void makeVtOrVn(String ch) {
        if (isVn(ch)) {
            Vn.add(ch);
        } else {
            Vt.add(ch);
        }
    }

    public void process() {
        // 在rules集合中遍历，找到X作为规则左部的规则
        String X;
        System.out.println("------------------------");
        System.out.println("First集和Follow集：");
        for (int i = 0; i < rules.size(); i++) {
            X = rules.get(i)[1];
            // 把ch添加到Vt或者Vn中
            for (int j = 0; j < X.length(); j++) {
                makeVtOrVn(String.valueOf(X.charAt(j)));
            }
            if (Vt.contains("@")) {
                Vt.remove("@");
                Vt.add("#");
            }
            addY(X,getFirst(X), true);
            X = rules.get(i)[0];
            getFollow(X);
        }
        printFirst();
        System.out.println();
        printFollow();
        System.out.println("\n------------------------");
        System.out.println("LL(1)分析表：");
        getLL();
        System.out.println("\n------------------------");
        Scanner sc = new Scanner(System.in);
        System.out.println("请输入一个由终结符组成的字符串：");
        String str = sc.nextLine();
        analyze(str);
    }

    /**
     * 分析str是否是该文法的句子
     * @param str
     */
    public void analyze(String str) {
        String[] tb = new String[3];
        Formatter formatter = new Formatter(System.out);
        formatter.format("%15s%15s%5s\n","分析栈", "余留输入串", "产生式");
        Stack<Character> list1 = new Stack<>(); // 分析栈
        Stack<Character> list2 = new Stack<>(); // 余留输入串
        list2.add("#".charAt(0));
        for (int j = str.length() - 1; j > 0; j--) {
            list2.add(str.charAt(j));
        }
        list1.push("#".charAt(0));
        list1.push(rules.get(0)[0].charAt(0));
        boolean success = true;
        while (!removeBrackets(list1.peek().toString()).equals("#")) {
            success = analyzeProcess(tb, list1, list2);
            if (!success) {
                break;
            }
        }
        if (success) {
            System.out.println("是该文法的句子。");
        }
    }

    public boolean analyzeProcess(String[] tb, Stack<Character> list1, Stack<Character> list2) {
        String peek1 = removeBrackets(list1.peek().toString());
        String peek2 = removeBrackets(list2.peek().toString());
        if (isVn(peek1)) { // 如果分析栈栈顶是非终结符
            // 分析栈和余留串栈的栈顶不能在LL表中找到对应的产生式
            if (!isContainFormula(peek1, peek2)) {
                System.out.println("不是该文法的句子。");
                return false;
            }
            int[] coor = findPosition(peek1, peek2);
            tb[2] = table[coor[0]][coor[1]]; // 所用产生式
            list1.pop(); // 弹出分析栈中匹配到的符号
            // 把产生式的右侧压入分析栈
            int index = table[coor[0]][coor[1]].lastIndexOf(">"); // 返回"->"的索引
            int j = table[coor[0]][coor[1]].length() - 1; // 最后一个字符的索引
            if (!String.valueOf(table[coor[0]][coor[1]].charAt(j)).equals("@")) {
                while (j > index) { // 倒着压入分析栈
                    list1.push(table[coor[0]][coor[1]].charAt(j--));
                }
            }
        } else { // 如果分析栈栈顶是终结符
            list1.pop();
            list2.pop();
        }
        tb[0] = TransToStr(list1, true); // 分析栈赋值
        tb[1] = TransToStr(list2, false); // 余留串栈赋值
        Formatter formatter = new Formatter(System.out);
        formatter.format("%15s%15s%15s\n",tb[0], tb[1], tb[2]);
        return true;
    }

    /**
     * 将栈中的字符转换为字符串
     * @param list
     */
    public String TransToStr(Stack<Character> list, boolean islist1) {
        StringTokenizer st = new StringTokenizer(removeBrackets(list.toString()), ",");
        StringBuffer sb = new StringBuffer();
        while (st.hasMoreTokens()) {
            sb.append(st.nextToken());
        }
        if (!islist1) { // 如果是余留串栈
            sb = sb.reverse();
        }

        return sb.toString().replace(" ","");
    }


    /**
     * LL表的(x,y)处是否包含产生式
     * @return
     */
    public boolean isContainFormula(String X, String Y) {
        int[] coor = findPosition(X, Y);
        if (table[coor[0]][coor[1]].equals("")) {
            return false;
        }
        return true;
    }

    /**
     * 创造LL(1)分析表
     */
    public void getLL() {
        int len = Vt.size() + 1;
        int wid = Vn.size() + 1;
        table = new String[wid][len];
        // 初始化填充为""
        for(int i = 0; i < table.length; i++){
            Arrays.fill(table[i],"");
        }

        // Vt, Vn 填表
        int i = 1;
        for (String t : Vt) {
            table[0][i++] = t;
        }

        i = 1;
        for (String n : Vn) {
            table[i++][0] = n;
        }

        // 根据first集填表
        // First集:first(aA)=a  rules:A->aA
        for (int j = 0; j < rules.size(); j++) {
            // 根据follow集填表
            if (rules.get(j)[1].equals("@")) { // 第j条规则右侧为"@"
                fillLLOnFollow(rules.get(j)[0]);
            }
            for (String k : first.keySet()) {
                if ((rules.get(j)[1]).equals(k)) {
                    fillLLOnFirst(rules.get(j)[0], k);
                }
            }
        }
        printLL(table);
    }

    /**
     * 根据follow集填表
     * @param X 右侧为"@"的规则的左部
     */
    public void fillLLOnFollow(String X) {
        // 遍历follow(X),将X->@填表
        StringTokenizer st1 = new StringTokenizer(follow.get(X), ",");
        while (st1.hasMoreTokens()) {
            String ch = st1.nextToken();
            int[] coor = findPosition(X, ch);
            table[coor[0]][coor[1]] = X + "->@";
        }
    }

    /**
     * 根据first集填表
     * @param X
     * @param firX
     */
    public void fillLLOnFirst(String X, String firX) {
        // 遍历first(firX),将X->Y填表
        StringTokenizer st1 = new StringTokenizer(first.get(firX), ",");
        while (st1.hasMoreTokens()) {
            String ch = st1.nextToken();
            int[] coor = findPosition(X, ch);
            table[coor[0]][coor[1]] = X + "->" + firX;
        }

    }

    /**
     * 查找规则应该被写到的位置 x,y分别对应表的table[i][0]和table[0][i]
     * @param x
     * @param y
     * @return
     */
    public int[] findPosition(String x, String y) {
        int[] coor = new int[2];
        // 去空格
        String y1 = y.replaceAll(" ","");
        if (y1.equals("@")) {
            y1 = "#";
        }
        // 查找应该写在第几行
        for (int i = 1; i <= Vn.size(); i++) {
            if (table[i][0].equals(x)){
                coor[0] = i;
                break;
            }
        }
        // 查找应该写在第几列
        for (int i = 1; i <= Vt.size(); i++) {
            if (table[0][i].equals(y1)){
                coor[1] = i;
                break;
            }
        }
        return coor;
    }

    /**
     * 获得Follow(B)
     * @param X
     * @return
     */
    public String getFollow(String X) {
        String res = "";
        // 1. 对于文法的开始符号E，令#∈FOLLOW(E)
        if (X.equals(rules.get(0)[0])) {
            res = addY(X, "#", false);
        }
        for (int j = 0; j < rules.size(); j++) {
            if (rules.get(j)[1].contains(X)) {
                getFollowY(rules.get(j)[0], rules.get(j)[1], X);
            }
        }
        return res;
    }

    public String getFollowY(String A, String B, String X) {
        int i = B.indexOf(X);
        String res = "";
        String y2 = "";
        // 若文法中有形如A::=αBβ的规则，且β≠ε，将FIRST(β)中一切非ε符号加进FOLLOW(B)中
        if (++i < B.length()) {
            y2 = String.valueOf(B.charAt(i));
            String tmp = getFirst(y2);
            if (tmp.contains("@")) {
                tmp = removeNull(tmp);
                res = addY(X, tmp, false);
            }
        }
        // 若文法中有形如Ａ::=αB 或 A::=αBβ的规则，且β=>*ε，则FOLLOW(A)中全部终结符均属于FOLLOW(B)
        if (i == B.length() || first.get(y2).contains("@")) {
            if (!A.equals(X)) {
                res = addY(X, getFollow(A), false);
            } else {
                return res;
            }
        }
        return res;
    }

    /**
     * 获得First(X) 头终结符号集合
     * @param X
     * @return
     */
    public String getFirst(String X) {
        Matcher matcher ;
        String res = "";
        // 1. X是终结符，则FIRST(X)={X}
        if (isVt(X)) { // 是终结符
            res = addY(X, X, true);
        }
        // X不是终结符
        else {
            if (!isVn(X)) { // X不是非终结符
                res = getFirstY(X);
                addY(X, res, true);
            } else {
                // 2. X是非终结符,且有Ｘ::=aβ规则 (a∈VT)，或Ｘ::=ε的规则
                // 把a或（和）ε加入FIRST(X)中
                ArrayList<String> Y = getRightRule(X); // 获得X作为左侧产生式的右侧
                // 有X::=Y1Y2…Yk
                Pattern patt2 = Pattern.compile("@|^[^A-Z].*$");
                for (String y : Y) {
                    matcher = patt2.matcher(String.valueOf(y.charAt(0)));
                    // 且有Ｘ::=aβ规则 (a∈VT)，或Ｘ::=ε的规则
                    if (matcher.matches()) {
                        // 把a或（和）ε加入FIRST(X)中
                        res = addY(X, matcher.group(), true);
                    } else {
                        // 3. X::=Y1Y2…Yk，且若Y1是非终结符
                        res = getFirstY(y);
                    }
                }
            }
        }
        return res;
    }

    /**
     * 获得规则右侧y的First集
     * @param y
     * @return
     */
    public String getFirstY(String y) {
        int i =0;
        String ch = String.valueOf(y.charAt(i));
        String res = getFirst(ch);
        while (isVn(ch)) { // ch是非终结符
            // 1)将FIRST(Y1)中一切非ε符号加进FIRST(X)中
            res = getFirst(ch);
            addY(y, res, true);
            if (first.get(ch).contains("@")) {
                first.put(ch, removeNull(first.get(ch)));
                // 2)发现Y1=>*ε,将FIRST(Y2)中一切非ε符号加进FIRST(X)中
                i++;
            } else { // 3)否则,计算过程结束
                break;
            }
        }
        return res;
    }

    /**
     * 把Y加入FIRST(X)或者FOLLOW(X)中
     * @param X
     * @param Y
     * @return
     */
    public String addY(String X, String Y, boolean isFIR) {
        String res = "";
        HashSet<String> tmp = new HashSet<>();
        if (isFIR) {
            if (val_fir.containsKey(X)) { // 不是第一次往first（X）中增加元素
                tmp = val_fir.get(X);
            }
            tmp.add(Y);
            val_fir.put(X, tmp);
            res = removeBrackets(tmp.toString());
            first.put(X, res);

        } else {
            if (val_fol.containsKey(X)) { // 不是第一次往first（X）中增加元素
                tmp = val_fol.get(X);
            }
            tmp.add(Y);
            val_fol.put(X, tmp);
            res = removeBrackets(tmp.toString());
            follow.put(X, res);
        }
        return res;
    }

    /**
     * 获得X作为左侧产生式的右侧串
     * @param X
     * @return
     */
    public ArrayList<String> getRightRule(String X) {
        // 在rules集合中遍历，找到X作为规则左部的规则
        ArrayList<String> Y = new ArrayList<>();
        for (int j = 0; j < rules.size(); j++) {
            if (X.equals(rules.get(j)[0])) {
                Y.add(rules.get(j)[1]);
            }
        }
        return Y;
    }

    /**
     * 去除ε符号
     * @param str
     * @return
     */
    public String removeNull(String str) {
        Pattern patt = Pattern.compile("^@,|,@");
        Matcher matcher = patt.matcher(str);
        str = matcher.replaceAll("");
        return str;
    }

    /**
     * 去掉方括号
     * @param str
     * @return
     */
    public String removeBrackets(String str) {
        return StringUtils.strip(str, "[]");
    }
    /**
     * 提取文法，保存到rules中
     * @param str
     */
    public void divideGrammars(String str) {
        String[] rule = new String[2];
        str.replaceAll(" ", ""); // 去除空格
        // 提出规则左部
        StringTokenizer st1 = new StringTokenizer(str, "::=");
        String left = st1.nextToken();
        Vn.add(left);
        String right = st1.nextToken();
        rule[0] = left;
        // 提取规则右部 合并存入rules
        if (str.indexOf("|") == -1) {
            rule[1] = right;
            rules.add(rule);
        } else { // 有多条规则
            StringTokenizer st2 = new StringTokenizer(right, "|");
            while (st2.hasMoreTokens()) {
                rule = new String[2];
                rule[0] = left;
                rule[1] = st2.nextToken();
                rules.add(rule);
            }
        }
    }

    /**
     * 读取文本
     */
    public void preTxt() {
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(src);
            BufferedReader br = new BufferedReader(fileReader);
            String read = null;
            while ((read = br.readLine()) != null) {
                divideGrammars(read);

                System.out.println(read);
            }
        } catch (FileNotFoundException e) {
            System.out.println("源文件未找到!");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("读写文件出现异常!");
            e.printStackTrace();
        }
    }

    /**
     * 打印文法
     */
    public void printRules() {
        for (int i = 0; i < rules.size(); i++) {
            System.out.println(rules.get(i)[0] + "," + rules.get(i)[1]);
        }
    }

    /**
     * 打印First集
     */
    public void printFirst() {
        for (int i = 0; i < rules.size(); i++) {
            System.out.print("First(" + rules.get(i)[1] + ")={" + first.get(rules.get(i)[1]) + "}\t");
        }
    }

    /**
     * 打印Follow集
     */
    public void printFollow() {
        for (String k : follow.keySet()) {
            System.out.print("Follow(" + k + ")={" + follow.get(k) + "}\t");
        }
    }

    /**
     * 打印LL(1)分析表
     * @param table
     */
    public void printLL(String[][] table) {
        for (int i = 0; i <= Vt.size(); i++) {
            System.out.print("\t" + table[0][i] + "\t");
        }
        System.out.println();
        for (int i = 1; i <= Vn.size(); i++) {
            for (int j = 0; j <= Vt.size(); j++) {
                System.out.print(table[i][j] + "\t\t");
            }
            System.out.println();
        }
    }
    public static void main(String[] args) {
        Code01_SyntaxAnalyzer alz = new Code01_SyntaxAnalyzer();
        alz.preTxt();
//        alz.printRules();
        alz.process();
    }
}
