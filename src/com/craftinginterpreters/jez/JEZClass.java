package com.craftinginterpreters.jez;
import java.util.List;
import java.util.Map;

class JEZClass implements JEZCallable {
    final String name;
    final JEZClass superclass;
    private final Map<String, JEZFunction> methods;
    JEZClass(String name, JEZClass superclass, Map<String, JEZFunction> methods) {
        this.superclass = superclass;
        this.name = name;
        this.methods = methods;
    }
    JEZFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }
        if (superclass != null) {
            return superclass.findMethod(name);
        }
        return null;
    }
    @Override
    public String toString() {
        return name;
    }
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        JEZInstance instance = new JEZInstance(this);
        JEZFunction initializer = findMethod("initialize");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }
        return instance;
    }

    @Override
    public int arity() {
        JEZFunction initializer = findMethod("initialize");
        if (initializer == null) return 0;
        return initializer.arity();
    }
}