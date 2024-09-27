package com.craftinginterpreters.jez;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.jez.TokenType.*;

//store source code as string and create list
class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    //place of lexeme being scanned
    private int start = 0;
    private int current = 0;
    private int line = 1;
    //create hashmap for keywords
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("template", TEMPLATE);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("function", FUNCTION);
        keywords.put("if", IF);
        keywords.put("none", NONE);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("variable", VARIABLE);
        keywords.put("while", WHILE);
    }

    Scanner(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            // beginning of the next lexeme.
            start = current;
            scanToken();
        }
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    //scan single char tokens
    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(':
                addToken(LEFT_PAREN);
                break;
            case ')':
                addToken(RIGHT_PAREN);
                break;
            case '{':
                addToken(LEFT_BRACE);
                break;
            case '}':
                addToken(RIGHT_BRACE);
                break;
            case ',':
                addToken(COMMA);
                break;
            case '.':
                addToken(DOT);
                break;
            case '-':
                addToken(MINUS);
                break;
            case '+':
                addToken(PLUS);
                break;
            case ';':
                addToken(SEMICOLON);
                break;
            case '*':
                addToken(STAR);
                break;
            //scan single char tokens that can be double when combined with =
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            //check if / is a comment or division
            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;
            //handle white space
            case ' ':
                break;
            case '\n':
                line++;
                break;
            //create string
            case '"':
                string();
                break;
            //error for tokens that do not exist in jez
            default:
                //check for digit
                if (isDigit(c)) {
                    number();
                }
                //check for letter
                else if (isAlpha(c)) {
                    identifier();
                } else {
                    JEZ.error(line, "Can not use one of these symbols.");
                }
                break;

        }
    }

    //consume letters in strings and check if string matches keyword
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }

    //consume numbers and consume decimal point using lookaheads to confirm number continues
    private void number() {
        while (isDigit(peek())) advance();
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) advance();
        }
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    //consume characters in string until it reads a "
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }
        if (isAtEnd()) {
            JEZ.error(line, "String is not finished, check for missing quote.");
            return;
        }
        advance();
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    //only consume char if expected
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    //lookahead for speed, one char
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    //lookahead for confirmation
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    //confirms letter is valid using boolean operators
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    //checks if c is a letter or number
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    //checks if c is valid number
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    //returns when all chars are consumed
    private boolean isAtEnd() {
        return current >= source.length();
    }

    //consumes next char in source file and returns it
    private char advance() {
        return source.charAt(current++);
    }

    //grabs text of current lexeme and creates new token
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    //handle literal tokens
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}




