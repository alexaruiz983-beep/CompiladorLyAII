package intermediate;

public class SemanticAnalyzer {
    private SymbolTable symbolTable;

    public SemanticAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public void declareVariable(String name, String type) throws SemanticException {
        String typeUpper = type.toUpperCase();
        if (!typeUpper.equals("INTEGER") && !typeUpper.equals("REAL") && !typeUpper.equals("BOOLEAN")) {
            throw new SemanticException("Error Semántico: Tipo de dato no válido '" + type + "'.");
        }
        symbolTable.insert(name, typeUpper);
    }

    public void validateAssignment(String varName, String expressionType) throws SemanticException {
        String varType = symbolTable.getType(varName);

        if (varType.equals(expressionType)) return;
        if (varType.equals("REAL") && expressionType.equals("INTEGER")) return;

        throw new SemanticException("Error Semántico: No se puede asignar una expresión de tipo '" 
                + expressionType + "' a la variable '" + varName + "' de tipo '" + varType + "'.");
    }

    public String checkBinaryOperation(String typeLeft, String op, String typeRight) throws SemanticException {
        typeLeft = typeLeft.toUpperCase();
        typeRight = typeRight.toUpperCase();

        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
            if (typeLeft.equals("BOOLEAN") || typeRight.equals("BOOLEAN")) {
                throw new SemanticException("Error de Tipos: Operador '" + op + "' no aplicable a BOOLEAN.");
            }
            if (typeLeft.equals("REAL") || typeRight.equals("REAL")) return "REAL";
            return "INTEGER";
        }

        if (op.equalsIgnoreCase("AND") || op.equalsIgnoreCase("OR")) {
            if (!typeLeft.equals("BOOLEAN") || !typeRight.equals("BOOLEAN")) {
                throw new SemanticException("Error de Tipos: El operador '" + op + "' requiere operandos BOOLEAN.");
            }
            return "BOOLEAN";
        }

        throw new SemanticException("Error Semántico: Operador no reconocido '" + op + "'.");
    }

    public void validateConditionExpression(String conditionType) throws SemanticException {
        if (!conditionType.equalsIgnoreCase("BOOLEAN")) {
            throw new SemanticException("Error Semántico: La condición debe evaluar a BOOLEAN.");
        }
    }
}
