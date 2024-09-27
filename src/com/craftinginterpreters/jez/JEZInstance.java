package com.craftinginterpreters.jez;
import java.util.HashMap;
import java.util.Map;

class JEZInstance {
    private JEZClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    JEZInstance(JEZClass klass) {
        this.klass = klass;
    }

    //to look up a property on an instance
    Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }
        JEZFunction method = klass.findMethod(name.lexeme);
        if (method != null) return method.bind(this);
        throw new RuntimeError(name, "You did not define '" + name.lexeme + "'.");
    }
    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }
    @Override
    public String toString() {
        return klass.name + " instance";
    }
}
