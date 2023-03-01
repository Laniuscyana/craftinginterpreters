package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Lox是解释器的主体，在整个解释器中，所有的输入代码的工作将交给Lox类来完成。
 * Lox类在读入被输入代码后，到scanner类里完成扫描工作
 * 在lox类中，定义main函数，完成jlox的主体功能
 **/
public class Lox {

    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    /**
     * main函数的输入参数，这里的 args是输入框中输入的指令，它将被视为一个字符串数组
     * Lox的main函数允许两种输入的形式，第一种是第二个if分支，此时的输入参数是一个文件地址，
     * 在本机上运行时，它通常会是一个本地文件，以lox作为后缀，如 “C:\testforlox.lox” .
     * 注意到在这时，输入的文件地址被视为一个长度为1的字符串数组，也对应着该分支的触发条件 .
     * 第二种是最后一个if分支，此时没有输入参数，即输入参数为空，Lox的main函数将直接在IDE
     * 的命令行窗口运行。根据Lox函数的设置，这种运行只能逐行读入，并立刻返回结果，因此想要输
     * 入如：
     * func add(a,b) {
     *     print a+b;
     * }
     * 时，正确的输入方式有两种，第一种是在任意路径打开记事本将以上代码输入到里面保存，并将后
     * 缀名改为lox，然后根据路径，将Lox类的运行参数设置为文件地址；第二种方法是在命令行种输入
     * func add(a,b) { print a+b; }
     * 这是因为Lox在命令行模式读入时只能逐行读入。
     **/

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            // 这表示输入参数存在问题，Lox只允许从文件路径读取和命令行输入指令两种读取方式
            System.out.println("Usage : jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    /**
     * 这是lox从文件读取的方法实现
     **/
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code.
        if (hadError) {
            System.exit(65);
        }
        if (hadRuntimeError) {
            System.exit(70);
        }
    }

    /**
     * 这是lox从命令行读取的方法实现
     *
     * If you want a more intimate conversation with your interpreter,
     * you can also run it interactively. Fire up jlox without any arguments,
     * and it drops you into a prompt where you can enter and execute code
     * one line at a time.
     * **/
    private static void runPrompt() throws IOException{
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            /*
              The readLine() function, as the name so helpfully implies,
              reads a line of input from the user on the command line
              and returns the result.
             */
            String line = reader.readLine();
            if (line == null) break;

            run(line);
        }
    }

    /**
     * 以上两种方法都基于以下的运行方法，它也是Lox解释器的核心。
     * 在终端输入参数后，lox需要理解这些参数的意思，并执行它们。
     * 那么第一步自然就是获取这些参数，把它们进行“扫描” （Scanner）
     * 在Scanner的过程中，为了能够让程序理解输入的代码，必须设置一些
     * 关键词让它理解，这就是Token
     * Token指的是用约定的lox语言中一些关键的词语，包括但不限于类、函数、打印
     * 继承等，加减乘除也是必要的，以及标点符号
     * 在识别了token之后，就可以继续进行下一步了。
     * 注意，下一步的操作是根据上一步的token来的
     *
     * Both the prompt and the file runner are thin wrappers around this core
     * function, run(String source).
     **/
    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        for (Token token : tokens) {
            System.out.println(token);
        }

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();
        // Expr expression = parser.parse();

        if (hadError) return;

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        if (hadError) return;


        interpreter.interpret(statements);

        //System.out.println(new AstPrinter().print(expression));


    }

    static void error(int line, String message) {
        report(line,"",message);
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, "at end", message);
        } else {
            report(token.line, " at '"+ token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line" + error.token.line + "]");
        hadRuntimeError = true;
    }

    private static void report(int line, String where, String message) {
        System.err.println(
                "[line" + line + "]Error " + where + ": " + message
        );
        hadError = true;
    }
}
