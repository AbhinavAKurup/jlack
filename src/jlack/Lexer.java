package jlack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jlack.TokenType.*;

public class Lexer {
    private String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    Lexer(String source) {
        this.source = source;
    }

    List<Token> lexTokens() {
        while (!isAtEnd()) {
            start = current;
            lexToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void lexToken() {
        char c = advance();
        switch (c) {
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_CURLY); break;
            case '}': addToken(RIGHT_CURLY); break;
            case '+': addToken(PLUS); break;
            case '-': addToken(MINUS); break;
            case '*': addToken(STAR); break;
            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    while (!((peek() == '*') && (peek(2) == '/')) && !isAtEnd()) {
                        if (peek() == '\n') line++;
                        advance();
                    }
                    if (!isAtEnd()) advance();
                    if (!isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;
            case '.':
                if (isDigit(peek())) {
                    number(c);
                } else {
                    addToken(DOT);
                }
                break;
            case ',': addToken(COMMA); break;
            case ';': addToken(SEMICOLON); break;
            case '!':
                if (match('=')) {
                    addToken(BANG_EQUAL);
                } else {
                    Lack.error(line, "Unexpected character " + c);
                }
                break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
            case '"':
            case '\'':
                string(c);
                break;
            default:
                if (isDigit(c)) {
                    number(c);
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lack.error(line, "Unexpected character " + c);
                    break;
                }
        }
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private boolean isDigit(char c) {
        return (c >= '0' && c <= '9');
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c == '_');
    }

    private boolean isAlphaNum(char c) {
        return (isAlpha(c) || isDigit(c));
    }

    private char advance() {
        return source.charAt(current++);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char peek() {
        return peek(1);
    }

    private char peek(int step) {
        if (current + step-1 >= source.length()) return '\0';
        return source.charAt(current + step-1);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String lexeme = source.substring(start, current);
        tokens.add(new Token(type, lexeme, literal, line));
    }

    private void string(char quote) {
        while (peek() != quote && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Lack.error(line, "Unterminated string");
            return;
        }

        advance();
        String val = source.substring(start+1, current-1);
        addToken(STRING, val);
    }

    private void number(char c) {
        while (isDigit(peek())) advance();
        if (c != '.') {
            if (peek() == '.' && isDigit(peek(2))) {
                advance();
                while (isDigit(peek())) advance();
            }
        }
        addToken(NUMBER, Double.parseDouble('0' + source.substring(start, current)));
    }

    private void identifier() {
        while (isAlphaNum(peek())) advance();
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }

    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("write", WRITE);
        keywords.put("writeln", WRITELN);
        keywords.put("let", LET);
        keywords.put("true", TRUE);
        keywords.put("false", FALSE);
        keywords.put("nil", NIL);
        keywords.put("if", IF);
        keywords.put("else", ELSE);
        keywords.put("not", NOT);
        keywords.put("or", OR);
        keywords.put("and", AND);
        keywords.put("xor", XOR);
        keywords.put("for", FOR);
        keywords.put("while", WHILE);
        keywords.put("fun", FUN);
        keywords.put("return", RETURN);
        keywords.put("class", CLASS);
        keywords.put("this", THIS);
        keywords.put("super", SUPER);
    }
}