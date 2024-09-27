package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        //create class and fields for syntax tree
        defineAst(outputDir, "Expr", Arrays.asList(
                "Block      : List<Stmt> statements",
                "Template      : Token name, Expr.Variable superclass," +
                            " List<Stmt.Function> methods",
                "Assign   : Token name, Expr value",
                "Binary   : Expr left, Token operator, Expr right",
                "Call     : Expr callee, Token paren, List<Expr> arguments",
                "Get      : Expr object, Token name",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Logical  : Expr left, Token operator, Expr right",
                "Set      : Expr object, Token name, Expr value",
                "Super    : Token keyword, Token method",
                "This     : Token keyword",
                "Unary    : Token operator, Expr right",
                "Variable : Token name"
        ));
        //file for expression and print statements
        defineAst(outputDir, "Stmt", Arrays.asList(
                "Expression : Expr expression",
                "Function   : Token name, List<Token> params," +
                        " List<Stmt> body",
                "If         : Expr condition, Stmt thenBranch," +
                        " Stmt elseBranch",
                "Print      : Expr expression",
                "Return     : Token keyword, Expr value",
                "Var        : Token name, Expr initializer",
                "While      : Expr condition, Stmt body"
        ));
    }
}

//output expr class
private static void defineAst(
        String outputDir, String baseName, List<String> types)
        throws IOException {
    String path = outputDir + "/" + baseName + ".java";
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    writer.println("package com.craftinginterpreters.lox;");
    writer.println();
    writer.println("import java.util.List;");
    writer.println();
    writer.println("abstract class " + baseName + " {");
    //visitor interface
    defineVisitor(writer, baseName, types);
    //base accept() method
    writer.println();
    writer.println("  abstract <R> R accept(Visitor<R> visitor);");
    //define subclasses
    for (String type : types) {
        String className = type.split(":")[0].trim();
        String fields = type.split(":")[1].trim();
        defineType(writer, baseName, className, fields);
    }
    writer.println("}");
    writer.close();
}

//iterate through all subclasses and declare visit method for each
private static void defineVisitor(
        PrintWriter writer, String baseName, List<String> types) {
    writer.println("  interface Visitor<R> {");
    for (String type : types) {
        String typeName = type.split(":")[0].trim();
        writer.println("  R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
    }

    writer.println("  }");
}

//define constructor
private static void defineType(
        PrintWriter writer, String baseName,
        String className, String fieldList) {
    writer.println("  static class " + className + " extends " + baseName + " {");
    //constructor
    writer.println("    " + className + "(" + fieldList + ") {");
    //store parameters
    String[] fields = fieldList.split(", ");
    for (String field : fields) {
        String name = field.split(" ")[1];
        writer.println("      this." + name + " = " + name + ";");
    }
    writer.println("    }");
    //call visit method for type
    writer.println();
    writer.println("    @Override");
    writer.println("    <R> R accept(Visitor<R> visitor) {");
    writer.println("      return visitor.visit" +
            className + baseName + "(this);");
    writer.println("    }");
    //fields
    writer.println();
    for (String field : fields) {
        writer.println("    final " + field + ";");
    }
    writer.println("  }");
}