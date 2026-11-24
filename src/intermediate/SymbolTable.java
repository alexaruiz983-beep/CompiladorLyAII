package com.mycompany.proyecto_automatas;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private Map<String, Symbol> table;

    public SymbolTable() {
        this.table = new HashMap<>();
    }

    public void insert(String name, String type) throws SemanticException {
        String key = name.toLowerCase();
        if (table.containsKey(key)) {
            throw new SemanticException("Error Semántico: La variable '" + name + "' ya fue declarada.");
        }
        table.put(key, new Symbol(name, type));
    }

    public Symbol lookup(String name) {
        return table.get(name.toLowerCase());
    }

    public boolean contains(String name) {
        return table.containsKey(name.toLowerCase());
    }

    public String getType(String name) throws SemanticException {
        Symbol symbol = lookup(name);
        if (symbol == null) {
            throw new SemanticException("Error Semántico: La variable '" + name + "' no ha sido declarada.");
        }
        return symbol.getType();
    }

    public void printTable() {
        System.out.println("=== TABLA DE SÍMBOLOS ===");
        for (Symbol s : table.values()) {
            System.out.println("Variable: " + s.getName() + " | Tipo: " + s.getType());
        }
        System.out.println("=========================\n");
    }
}
