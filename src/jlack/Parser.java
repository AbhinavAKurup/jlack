package jlack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static jlack.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Stmt statement() {
        if (match(WRITE)) return writeStatement("");
        if (match(WRITELN)) return writeStatement("\n");
        if (match(READ)) return readStatement();
        if (match(READNUM)) return readNumStatement();
        if (match(LEFT_CURLY)) return new Stmt.Block(block());
        if (match(IF)) return ifStatement();
        if (match(WHILE)) return whileStatement();
        if (match(FOR)) return forStatement();
        if (match(REPEAT)) return repeatStatement();
        if (match(BREAK)) return breakStatement();
        if (match(CONTINUE)) return continueStatement();
        return expressionStatement();
    }

    private Stmt declaration() {
        try {
            if (match(LET)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronise();
            return null;
        }
    }

    private Stmt writeStatement(String end) {
        Expr value = expression();
        consume(SEMICOLON, "Expected ';' after value");
        return new Stmt.Write(value, end);
    }

    private Stmt readStatement() {
        Token token = peek(-1);
        Token name = consume(IDENTIFIER, "Expected variable name");
        consume(SEMICOLON, "Expected ';' after variable name");
        return new Stmt.Read(name, token);
    }

    private Stmt readNumStatement() {
        Token token = peek(-1);
        Token name = consume(IDENTIFIER, "Expected variable name");
        consume(SEMICOLON, "Expected ';' after variable name");
        return new Stmt.ReadNum(name, token);
    }

    private Expr evalExpr() {
        Token token = peek(-1);
        Expr string = primary();
        return new Expr.Eval(string, token);
    }

    private Stmt ifStatement() {
        Expr condition = expression();

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt whileStatement() {
        Expr condition = expression();
        Stmt body = statement();
        return new Stmt.While(condition, body, null);
    }

    private Stmt repeatStatement() {
        Stmt body = statement();
        if (match(UNTIL)) {
            Expr condition = expression();
            return new Stmt.RepeatUntil(condition, body);
        } else if (match(FOR)) {
                Token forToken = peek(-1);
                Expr times = term();
                return new Stmt.RepeatFor(times, body, forToken);
        }
        throw error(peek(), "Expected 'until' or 'for'");
    }

    private Stmt forStatement() {
        Stmt initialiser;
        if (match(SEMICOLON)) initialiser = null;
        else if (match(LET)) {
            initialiser = varDeclaration();
        } else {
            initialiser = expressionStatement();
        }
        
        Expr condition = null;
        if (!check(SEMICOLON)) condition = expression();
        consume(SEMICOLON, "Expected ';' after loop condition");

        Expr increment = null;
        if (!check(SEMICOLON)) {
            increment = expression();
        }
        consume(SEMICOLON, "Expected ';' after loop increment");

        Stmt body = statement();

        // if (increment != null) {
        //     body = new Stmt.Block(Arrays.asList(
        //         body,
        //         new Stmt.Expression(increment)
        //     ));
        // }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body, increment);

        if (initialiser != null) {
            body = new Stmt.Block(Arrays.asList(
                initialiser,
                body
            ));
        }

        return body;
    }

    private Stmt breakStatement() {
        consume(SEMICOLON, "Expected ';' after 'break'");
        return new Stmt.Break(peek(-2));
    }

    private Stmt continueStatement() {
        consume(SEMICOLON, "Expected ';' after 'continue'");
        return new Stmt.Continue(peek(-2));
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expected variable name");
        Expr initialiser = null;
        if (match(EQUAL)) {
            initialiser = expression();
        }
        consume(SEMICOLON, "Expected ';' after variable declaration");
        return new Stmt.Let(name, initialiser);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expected ';' after value");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_CURLY) && !isAtEnd()) statements.add(declaration());
        consume(RIGHT_CURLY, "Expected '}' after block");
        return statements;
    }

    private Expr assignment() {
        Expr expr = or();
        if (match(EQUAL)) {
            Token equals = peek(-1);
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target");
        }
        return expr;
    }

    private Expr or() {
        Expr expr = xor();
        while (match(OR, NOR)) {
            Token operator = peek(-1);
            Expr right = xor();

            if (operator.type == OR) expr = new Expr.Logical(expr, operator, right);
            
            if (operator.type == NOR) {
                Token not = new Token(NOT, "nor", null, operator.line);
                Token or = new Token(OR, "nor", null, operator.line);
                expr = new Expr.Unary(not, new Expr.Logical(expr, or, right));
            }
        }
        return expr;
    }

    private Expr xor() {
        Expr expr = and();
        while (match(XOR, XNOR)) {
            Token operator = peek(-1);
            Expr right = and();

            Token not = new Token(NOT, "xor", null, operator.line);
            Token or = new Token(OR, "xor", null, operator.line);
            Token and = new Token(AND, "xor", null, operator.line);
            expr = new Expr.Logical(
                new Expr.Logical(expr, and, new Expr.Unary(not, right)),
                or,
                new Expr.Logical(new Expr.Unary(not, expr), and, right)
            );

            if (operator.type == XNOR) {
                expr = new Expr.Unary(not, expr);
            }
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();
        while (match(AND, NAND)) {
            Token operator = peek(-1);
            Expr right = equality();

            if (operator.type == AND) expr = new Expr.Logical(expr, operator, right);

            if (operator.type == NAND) {
                Token not = new Token(NOT, "nand", null, operator.line);
                Token and = new Token(AND, "nand", null, operator.line);
                expr = new Expr.Unary(not, new Expr.Logical(expr, and, right));
            }
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = peek(-1);
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = peek(-1);
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(PLUS, MINUS)) {
            Token operator = peek(-1);
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(STAR, SLASH, MODULO)) {
            Token operator = peek(-1);
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        while (match(NOT, MINUS)) {
            Token operator = peek(-1);
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(NIL)) return new Expr.Literal(null);
        if (match(NUMBER, STRING)) return new Expr.Literal(peek(-1).literal);
        if (match(IDENTIFIER)) return new Expr.Variable(peek(-1));
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expected ')' after expression");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expected expression");
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return peek(-1);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String msg) {
        if (check(type)) return advance();
        throw error(peek(), msg);
    }

    private ParseError error(Token token, String msg) {
        Lack.error(token, msg);
        return new ParseError();
    }

    private void synchronise() {
        advance();

        while (!isAtEnd()) {
            if (peek(-1).type == SEMICOLON) return;

            switch (peek().type) {
                case WRITE:
                case WRITELN:
                case READ:
                case READNUM:
                case LET:
                case IF:
                case FOR:
                case WHILE:
                case REPEAT:
                case FUN:
                case RETURN:
                case CLASS:
                    return;
                default: // pass
            }
            advance();
        }
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token peek() {
        return peek(0);
    }

    // previous() = peek(-1)
    private Token peek(int step) {
        return tokens.get(current + step);
    }
}
