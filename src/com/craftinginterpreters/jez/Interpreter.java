package com.craftinginterpreters.jez;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();
    //create clock
    Interpreter() {
        globals.define("clock", new JEZCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }
        });
    }
            @Override
            public String toString() { return "<native fn>"; }

    //public API
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            JEZ.runtimeError(error);
        }
    }
    @Override
    //get runtime value
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }
    //evaluate left operand first
    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }
    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof JEZInstance)) {throw new RuntimeError(expr.name, "Only objects have fields.");
        }
        Object value = evaluate(expr.value);
        ((JEZInstance)object).set(expr.name, value);
        return value;
    }
    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        int distance = locals.get(expr);
        JEZClass superclass = (JEZClass)environment.getAt(distance, "super");
        JEZInstance object = (JEZInstance)environment.getAt(distance - 1, "this");
        JEZFunction method = superclass.findMethod(expr.method.lexeme);
        if (method == null) {
            throw new RuntimeError(expr.method, "Did not create property '" + expr.method.lexeme + "'.");
        }
        return method.bind(object);
    }
    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }
    //evaluate operand expression
    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            //allow ! to be true
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }
        return null;
    }
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }
    //checks if var is within distance
    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Everything in equation must be a number.");
    }
    //validator
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Everything in equation must be a number.");
    }
    //false and null are false, everything else is truthy
    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }
    //handle equality
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }
    //takes in a syntax tree as string and evaluates
    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }
    //evaluate grouping
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }
    //send expression to visitor implementation
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }
    //helper
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }
    //resolver gives # of envir. between current and enclosing where interp. can find var value
    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }
    //environment for blocks scope
    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }
    //interpret the class declaration
    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null);
        Object superclass = null;
        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass);
            if (!(superclass instanceof JEZClass)) {
                throw new RuntimeError(stmt.name, "Super must be a template.");
            }
        }
        Map<String, JEZFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            JEZFunction function = new JEZFunction(method, environment, method.name.lexeme.equals("initialize"));
            methods.put(method.name.lexeme, function);
        };
        JEZClass klass = new JEZClass(stmt.name.lexeme, (JEZClass) superclass, methods);
        if (stmt.superclass != null) {
            environment = new Environment(environment);
            environment.define("super", superclass);
        }
        if (superclass != null) {
            environment = environment.enclosing;
        }
        environment.assign(stmt.name, klass);
        return null;
    }

    //visit statements
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }
    //visit function statement
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        JEZFunction function = new JEZFunction(stmt, environment, false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }
    //interpret if and else statements
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }
    //allow for using var by assigning it
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        return value;
    }
    //return binary expressions answers
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            //addition or concatenate two strings
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator,
                        "Addition must be between two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
        }
        return null;
    }
    //visit function call
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }
        //runtime error
        if (!(callee instanceof JEZCallable)) {
            throw new RuntimeError(expr.paren,
                    "Can only call functions and templates.");
        }

        JEZCallable function = (JEZCallable)callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "There needs to be " +
                    function.arity() + " arguments but you gave " +
                    arguments.size() + ".");
        }
        return function.call(this, arguments);
    }
    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof JEZInstance) {
            return ((JEZInstance) object).get(expr.name);
        }

        throw new RuntimeError(expr.name, "Only objects have properties.");
    }
}