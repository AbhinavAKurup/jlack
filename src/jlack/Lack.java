package jlack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lack {
    static boolean hadError = false;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            runShell();
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            System.out.println("Usage: jlack <script>");
            System.exit(64);
        }
    }

    public static void runShell() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    public static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError) System.exit(65);
    }

    public static void run(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.lexTokens();

        // for (Token token : tokens) {
        //     System.out.println(token);
        // }

        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();

        if (hadError) return;
        System.out.println(new AstPrinter().print(expression));
    }

    static void error(int line, String msg) {
        report(line, "", msg);
    }

    static void error(Token token, String msg) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", msg);
        } else {
            report(token.line, "at '"+token.lexeme+"'", msg);
        }
    }

    private static void report(int line, String location, String msg) {
        System.err.println(String.format((location == "" ? "<line %d> Error%s: %s" : "<line %d> Error %s: %s"), line, location, msg));
        hadError = true;
    }
}