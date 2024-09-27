package com.craftinginterpreters.jez;
enum TokenType {
    //single char token
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    //1 or 2 char tokens
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    //literals
    IDENTIFIER, STRING, NUMBER,

    //keywords
    AND, TEMPLATE, ELSE, FALSE, FUNCTION, FOR, IF, NONE, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VARIABLE, WHILE,

    EOF
}

