package proyecto_lya2;

public class SemanticAnalyzer {
    private SymbolTable symbolTable;

    public SemanticAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    // -------------------------------------------------------------
    // 1. VALIDACIÓN DE DECLARACIONES (declaration)
    // -------------------------------------------------------------
    public void declareVariable(String name, String type) throws SemanticException {
        // Valida que el tipo sea uno de los 3 permitidos por la gramática
        String typeUpper = type.toUpperCase();
        if (!typeUpper.equals("INTEGER") && !typeUpper.equals("REAL") && !typeUpper.equals("BOOLEAN")) {
            throw new SemanticException("Error Semántico: Tipo de dato no válido '" + type + "'.");
        }
        symbolTable.insert(name, typeUpper);
    }

    // -------------------------------------------------------------
    // 2. VALIDACIÓN DE ASIGNACIONES (assignment -> identifier := expression)
    // -------------------------------------------------------------
    public void validateAssignment(String varName, String expressionType) throws SemanticException {
        String varType = symbolTable.getType(varName); // Lanza error si varName no está declarada

        // Reglas de compatibilidad de asignación
        if (varType.equals(expressionType)) {
            return; // Tipos idénticos: VÁLIDO
        }

        // Permite asignar un INTEGER a una variable REAL (promoción implícita)
        if (varType.equals("REAL") && expressionType.equals("INTEGER")) {
            return; // VÁLIDO
        }

        throw new SemanticException("Error Semántico: No se puede asignar una expresión de tipo '" 
                + expressionType + "' a la variable '" + varName + "' de tipo '" + varType + "'.");
    }

    // -------------------------------------------------------------
    // 3. OPERACIONES ARITMÉTICAS (addop: +, - | mulop: *, /)
    // -------------------------------------------------------------
    public String checkBinaryOperation(String typeLeft, String op, String typeRight) throws SemanticException {
        typeLeft = typeLeft.toUpperCase();
        typeRight = typeRight.toUpperCase();

        // Operaciones aritméticas simples (+, -, *, /)
        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
            if (typeLeft.equals("BOOLEAN") || typeRight.equals("BOOLEAN")) {
                throw new SemanticException("Error de Tipos: Operador '" + op + "' no aplicable a BOOLEAN.");
            }
            // Si alguno de los dos operandos es REAL, el resultado es REAL
            if (typeLeft.equals("REAL") || typeRight.equals("REAL")) {
                return "REAL";
            }
            return "INTEGER";
        }

        // Operadores Lógicos (AND, OR)
        if (op.equalsIgnoreCase("AND") || op.equalsIgnoreCase("OR")) {
            if (!typeLeft.equals("BOOLEAN") || !typeRight.equals("BOOLEAN")) {
                throw new SemanticException("Error de Tipos: El operador '" + op + "' requiere operandos BOOLEAN.");
            }
            return "BOOLEAN";
        }

        throw new SemanticException("Error Semántico: Operador no reconocido '" + op + "'.");
    }

    // -------------------------------------------------------------
    // 4. OPERACIONES RELACIONALES (relop: =, <>, <, <=, >, >=)
    // -------------------------------------------------------------
    public String checkRelationalOperation(String typeLeft, String relop, String typeRight) throws SemanticException {
        typeLeft = typeLeft.toUpperCase();
        typeRight = typeRight.toUpperCase();

        // No se permite comparar BOOLEAN con números usando <, <=, >, >=
        if ((typeLeft.equals("BOOLEAN") || typeRight.equals("BOOLEAN")) && !relop.equals("=") && !relop.equals("<>")) {
            throw new SemanticException("Error de Tipos: No se puede usar '" + relop + "' con tipos BOOLEAN.");
        }

        // Si ambos son números o ambos son booleanos (para = y <>), el resultado SIEMPRE es BOOLEAN
        return "BOOLEAN";
    }

    // -------------------------------------------------------------
    // 5. ESTRUCTURAS DE CONTROL (IF / WHILE)
    // -------------------------------------------------------------
    public void validateConditionExpression(String conditionType) throws SemanticException {
        if (!conditionType.equalsIgnoreCase("BOOLEAN")) {
            throw new SemanticException("Error Semántico: La condición en IF/WHILE debe evaluar a BOOLEAN, pero se dio '" + conditionType + "'.");
        }
    }
}
