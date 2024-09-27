package com.craftinginterpreters.jez;
import java.util.HashMap;
import java.util.Map;

class Environment {
    final Environment enclosing;
    //constructors
    //for global scope
    Environment() {
        enclosing = null;
    }
    //for local scope
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }
    private final Map<String, Object> values = new HashMap<>();
    //look up variable
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        if (enclosing != null) return enclosing.get(name);
        throw new RuntimeError(name,
                "Variable '" + name.lexeme + "' has not been created.");
    }
    //assign values, error if variable doesnt exist already
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }
        //check outer environment for var if not in enclosed one
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }
        throw new RuntimeError(name,
                "Variable '" + name.lexeme + "' has not been created.");
    }
    void define(String name, Object value) {
        values.put(name, value);
    }
    //reach environment w wanted variable
    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }

        return environment;
    }
    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }
    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }
}