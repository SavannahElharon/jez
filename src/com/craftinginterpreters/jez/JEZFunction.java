package com.craftinginterpreters.jez;
import java.util.List;
//allows function calls
class JEZFunction implements JEZCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean isInitializer;

    JEZFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.isInitializer = isInitializer;
        this.closure = closure;
        this.declaration = declaration;
    }
    JEZFunction bind(JEZInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new JEZFunction(declaration, environment, isInitializer);
    }
    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
    //checks arity
    @Override
    public int arity() {
        return declaration.parameters.size();
    }

    //each function gets its own environment
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.parameters.size(); i++) {
            environment.define(declaration.parameters.get(i).lexeme, arguments.get(i));
        }
        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }
        if (isInitializer) return closure.getAt(0, "this");
        return null;
    }
}