package com.mycompany.proyecto_automatas;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SimpleParser {
    private SymbolTable symbolTable;
    private SemanticAnalyzer analyzer;
    private IntermediateCodeGenerator codeGenerator;

    public SimpleParser() {
        this.symbolTable = new SymbolTable();
        this.analyzer = new SemanticAnalyzer(this.symbolTable);
        this.codeGenerator = new IntermediateCodeGenerator();
    }

    public void parseFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            boolean inVarSection = false;
            System.out.println("=== LEYENDO ARCHIVO FUENTE: " + filePath + " ===\n");

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    if (line.equalsIgnoreCase("VAR")) {
                        inVarSection = true;
                    } else if (line.equalsIgnoreCase("BEGIN")) {
                        inVarSection = false;
                        this.symbolTable.printTable();
                    } else {
                        if (inVarSection && line.contains(":")) {
                            this.processDeclaration(line);
                        }
                        if (!inVarSection && line.contains(":=")) {
                            this.processAssignment(line);
                        }
                    }
                }
            }

            System.out.println("--------------------------------------------------");
            System.out.println("¡ÉXITO! Análisis semántico y código intermedio completado.\n");
            this.codeGenerator.printCode();

        } catch (IOException e) {
            System.err.println("Error al abrir el archivo: " + e.getMessage());
        } catch (SemanticException e) {
            System.err.println("\n[ERROR SEMÁNTICO DETECTADO]: " + e.getMessage());
        }
    }

    private void processDeclaration(String line) throws SemanticException {
        line = line.replace(";", "").trim();
        String[] parts = line.split(":");
        if (parts.length == 2) {
            String[] vars = parts[0].split(",");
            String type = parts[1].trim();
            for (String v : vars) {
                String varName = v.trim();
                if (!varName.isEmpty()) {
                    this.analyzer.declareVariable(varName, type);
                }
            }
        }
    }

    private void processAssignment(String line) throws SemanticException {
        line = line.replace(";", "").trim();
        String[] parts = line.split(":=");
        if (parts.length < 2) return;
        
        String varName = parts[0].trim();
        String expression = parts[1].trim();
        System.out.println("Analizando asignación: " + varName + " := " + expression);

        ExpressionResult exprRes = this.translateExpression(expression);
        this.analyzer.validateAssignment(varName, exprRes.getType());
        this.codeGenerator.emit(varName + " := " + exprRes.getPlace());
    }

    private ExpressionResult translateExpression(String expr) throws SemanticException {
        expr = expr.trim();
        
        if (expr.equalsIgnoreCase("TRUE") || expr.equalsIgnoreCase("FALSE")) {
            return new ExpressionResult("BOOLEAN", expr.toUpperCase());
        }

        if (!expr.contains("+") && !expr.contains("-") && !expr.contains("*") && !expr.contains("/")) {
            String type;
            if (this.symbolTable.contains(expr)) {
                type = this.symbolTable.getType(expr);
            } else if (expr.matches("\\d+(\\.\\d+)?")) {
                type = expr.contains(".") ? "REAL" : "INTEGER";
            } else {
                throw new SemanticException("Error Semántico: Token o variable no reconocida '" + expr + "'.");
            }
            return new ExpressionResult(type, expr);
        }

        String[] tokens = expr.split("\\s+");
        if (tokens.length >= 3) {
            String leftOp = tokens[0];
            String op = tokens[1];
            
            StringBuilder rightBuilder = new StringBuilder();
            for (int i = 2; i < tokens.length; i++) {
                rightBuilder.append(tokens[i]).append(" ");
            }
            String rightOp = rightBuilder.toString().trim();

            ExpressionResult leftRes = translateExpression(leftOp);
            ExpressionResult rightRes = translateExpression(rightOp);

            String resultType = this.analyzer.checkBinaryOperation(leftRes.getType(), op, rightRes.getType());
            String temp = this.codeGenerator.newTemp();

            this.codeGenerator.emit(temp + " := " + leftRes.getPlace() + " " + op + " " + rightRes.getPlace());

            return new ExpressionResult(resultType, temp);
        }

        throw new SemanticException("Error Semántico en la expresión: '" + expr + "'.");
    }
}
