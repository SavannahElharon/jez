package com.craftinginterpreters.jez;
import java.util.List;

interface JEZCallable {
    int arity();
    //call interpreter to return value that call expression produces
    Object call(Interpreter interpreter, List<Object> arguments);
}
