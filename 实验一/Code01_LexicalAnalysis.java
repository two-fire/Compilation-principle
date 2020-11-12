import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Code01_LexicalAnalysis {
    private String keyWords[] = {"break","boolean","if","else","static","while",
            "final","int","double","cout"};// 关键字数组
    private String operators[] = { "+","-", "*", "/", "=", "&", "<<", ">>"}; // 运算符数组
    private char separators[] = { ',', ';', '{', '}', '(', ')', '[', ']', '_',
            ':', '、', '.', '"' }; // 分隔符数组
    private String filePath; // 源程序名
    private StringBuffer buffer = new StringBuffer(); // 缓冲区
    private char ch; // 字符变量，存放最新读进的源程序字符
    private static int i = 0;
    private static int keyType ;
    private String strToken; // 存放构成单词符号的字符串

    private int id = 0; // 单词序号

    File writeName = new File("output2.txt");

    public Code01_LexicalAnalysis(String filePath) {
        this.filePath = filePath;
    }

    /**
     * 输入字符读到ch中
     */
    public void getChar() {
        ch = buffer.charAt(i);
        i++;
    }

    /**
     * 检查ch中的字符是否为空白，若是则调用getChar() 直至ch中进入一个非空白字符
     */
    public void getBc() {
        while (Character.isSpaceChar(ch)) {
            getChar();
        }
    }

    /**
     * 将ch连接到strToken之后
     */
    public void concat() {
        strToken += ch;
    }

    /**
     * 将搜索指示器回调一个字符位置，将ch置空
     */
    public void retract() {
        i--;
        ch = ' ';
    }

    /**
     * 判断字符是否为字母
     */
    boolean isLetter() {
        return Character.isLetter(ch);
    }

    /**
     * 判断字符是否为数字
     */
    boolean isDigit() {
        return Character.isDigit(ch);
    }

    /**
     * 判断单词是否为关键字
     * [1,50]
     */
    public int isKeyWord() {
        keyType = -1;
        for (int i = 0; i < keyWords.length; i++) {
            if (keyWords[i].equals(strToken)) keyType = i+1 ;
        }
        return keyType;
    }

    /**
     * 判断是否为运算符
     * [51,100)
     */
    public int isOperator() {
        keyType = -1;
        for (int i = 0; i < operators.length; i++) {
            if (operators[i].equals(strToken)) {
                keyType = i + 51;
            }
        }
        return keyType;
    }

    /**
     * 判断是否为分隔符
     * [101,150)
     */
    public int isSeparators() {
        keyType = -1;
        for (int i = 0; i < separators.length; i++) {
            if (ch == separators[i]) keyType = i+101;
        }
        return keyType;
    }

    /**
     * 将strToken插入到关键字数组中
     * @param strToken
     */
    public void insertKeyWords(String strToken) {
        id++;
        try {
            writeName.createNewFile();
            FileWriter writer = new FileWriter(writeName,true);
            writer.write(id +"\t" + keyType+"\t"+strToken+"\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将strToken插入到符号表
     */
    public void insertId(String strToken) {
        id++;
        //System.out.print("标识符，种别200");
        try {
            writeName.createNewFile();
            FileWriter writer = new FileWriter(writeName,true);
            writer.write(id +"\t"+ 200+"\t"+strToken+"\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将strToken中的常数插入到常数表中
     */
    public void insertConst(String strToken) {
        int num = Integer.parseInt(strToken);
        id++;
        //System.out.print("常数，种别0");
        try {
            writeName.createNewFile();
            FileWriter writer = new FileWriter(writeName,true);
            writer.write(id +"\t"+ 0+"\t"+strToken+"\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将strToken插入到运算符表中
     */
    public void insertOperators(String strToken) {
        //System.out.print("运算符，种别 [51,100)");
        id++;
        try {
            writeName.createNewFile();
            FileWriter writer = new FileWriter(writeName,true);
            writer.write(id +"\t"+ keyType+"\t"+strToken+"\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将ch插入到分隔符表
     */
    public void insertSeparators() {
        //System.out.print("分隔符，种别 [101,150)");
        id++;
        try {
            writeName.createNewFile();
            FileWriter writer = new FileWriter(writeName,true);
            writer.write(id +"\t"+ keyType+"\t"+ch+"\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * txt文件读取到缓冲区
     * @param
     */
    public void readTxt() {
        try {
            FileReader file = new FileReader(filePath);
            BufferedReader br = new BufferedReader(file);
            String temp = null;
            while ((temp = br.readLine()) != null) {
                buffer.append(temp);
                System.out.println(temp);
            }
        }catch (FileNotFoundException e) {
            System.out.println("源文件未找到!");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("读写文件出现异常!");
            e.printStackTrace();
        }
    }

    /**
     * 如果文件存在，进行文件预处理并写入新的文件output.txt
     */
    public void pretreatment() throws IOException {
        File file = new File(filePath);
        InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "utf-8");
        BufferedReader br = new BufferedReader(isr);
        String str = null;
        String REGEX = "\\s+|\\/\\/.*|/\\*.*|\\\\/\\\\*(\\\\s|.)*?\\\\*\\\\/";	//空格、制表符正则表达式,\s匹配任何空白字符，包括空格、制表符、换页符等
        try {
            File writeName = new File("output.txt"); // 相对路径，如果没有则要建立一个新的output.txt文件
            writeName.createNewFile(); // 创建新文件,有同名的文件的话直接覆盖
            try (FileWriter writer = new FileWriter(writeName)
            ) {
                Pattern patt = Pattern.compile(REGEX);	//创建Pattern对象，处理正则表达式
                    while ((str = br.readLine()) != null) {
                        Matcher mat = patt.matcher(str);	//先处理每一行的空白字符
                        str = mat.replaceAll(" ");
                        writer.write(str);
                    }
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 词法分析
     */
    public void analyse(){
        strToken = "";

        while (i < buffer.length()) {
            getChar();
            getBc();
            if (isLetter()) { // 如果ch为字母
                while (isLetter() || isDigit()) { // 如果是字母或数字
                    concat();
                    getChar();
                }
                retract(); // 回调
                if (isKeyWord() > 0) { // 如果是关键字，则插入到保留字表中
                    insertKeyWords(strToken);
                } else { // 否则插入到符号表中
                    insertId(strToken);
                }
                strToken = "";
            } else if (isDigit()) { // 如果ch为数字
                while (isDigit()) {
                    concat();
                    getChar();
                }
                if (isLetter()) {
                    System.out.println("第"+(id+1)+"个单词出错");
                    while (isLetter()) {
                        concat();
                        getChar();
                    }
                    System.out.println("出错的单词为："+strToken);
                } else {
                    insertConst(strToken); // 是常数，插入到常数表中
                }
                retract(); // 回调
                strToken = "";

            }  else if (isSeparators()>0) { // 如果是分隔符，插入到分隔符表中
                insertSeparators();
            } else { // 如果是运算符，则插入到运算符表
                while (isOperator() < 0) {
                    concat();
                    getChar();
                }
                retract(); // 回调
                insertOperators(strToken);
                strToken = "";
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Code01_LexicalAnalysis als = new Code01_LexicalAnalysis("3_1.txt");
        als.readTxt();
        als.pretreatment();
        Code01_LexicalAnalysis als2 = new Code01_LexicalAnalysis("output.txt");
        System.err.println("常数，种别0");
        System.err.println("关键字，种别[1,50]");
        System.err.println("运算符，种别 [51,100)");
        System.err.println("分隔符，种别 [101,150)");
        System.err.println("标识符，种别200");
        als2.readTxt();
        als2.analyse();

    }
}


