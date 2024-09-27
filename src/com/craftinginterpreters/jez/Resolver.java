package com.craftinginterpreters.jez;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }
    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD
    }
    private enum ClassType {
        NONE,
        TEMPLATE,
        SUB
    }

    private ClassType currentClass = ClassType.NONE;
    //helper
    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }
    //create scope for block
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }
    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.TEMPLATE;
        declare(stmt.name);
        define(stmt.name);
        if (stmt.superclass != null) {
            currentClass = ClassType.SUB;
            resolve(stmt.superclass);
            beginScope();
            scopes.peek().put("super", true);
        }
        beginScope();
        scopes.peek().put("this", true);
        for (Stmt.Function method : stmt.methods) {
            FunctionType declaration = FunctionType.METHOD;
            if (method.name.lexeme.equals("initialize")) {
                declaration = FunctionType.INITIALIZER;
            }
            resolveFunction(method, declaration);
        }
        endScope();
        if (stmt.superclass != null) endScope();
        currentClass = enclosingClass;
        return null;
    }
    //traverse expression
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }
    //create scope for functions
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }
    //resolve both branches of if
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            JEZ.error(stmt.keyword, "Can not return from this code.");
        }
        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                JEZ.error(stmt.keyword, "Can't return a value from an initializer.");
            }
            resolve(stmt.value);
        }

        return null;
    }
    //var declaration
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }
    //resolve body of while statement
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }
    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }
    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }
    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);

        for (Expr argument : expr.arguments) {
            resolve(argument);
        }

        return null;
    }
    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }
    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }
    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {

        return null;
    }
    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }
    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }
    @Override
    public Void visitSuperExpr(Expr.Super expr) {
        if (currentClass == ClassType.NONE) {
            JEZ.error(expr.keyword,
                    "Can't use 'super' outside of a template.");
        } else if (currentClass != ClassType.SUB) {
            JEZ.error(expr.keyword,
                    "Can't use 'super' in a template with no super.");
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }
    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (currentClass == ClassType.NONE) {
            JEZ.error(expr.keyword, "Can't use 'this' outside of a template.");
            return null;
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }
    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }
    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            JEZ.error(expr.name, "Can not read most recent variable (local) in its own initializer.");
        }
        resolveLocal(expr, expr.name);
        return null;
    }
    //walk and resolve list of statements
    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }
    //resolving expression
    private void resolve(Expr expr) {
        expr.accept(this);
    }
    //resolve function body(new scope for body then bind var for function parameters)
    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;
        beginScope();
        for (Token param : function.parameters) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction;
    }
    //new block scope
    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }
    private void endScope() {
        scopes.pop();
    }
    //add var to inner scope so it shadows outer ones
    private void declare(Token name) {
        if (scopes.isEmpty()) return;
        Map<String, Boolean> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            JEZ.error(name, "There is already a variable with this name in this scope.");}
        scope.put(name.lexeme, false);
    }
    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }
    //helper for variable resolution
    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }
}
