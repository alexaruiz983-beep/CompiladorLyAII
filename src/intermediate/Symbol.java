package com.mycompany.proyecto_automatas;

public class Symbol {
    private String name;
    private String type;

    public Symbol(String name, String type) {
        this.name = name;
        this.type = type.toUpperCase();
    }

    public String getName() { return name; }
    public String getType() { return type; }

    @Override
    public String toString() {
        return "Symbol{name='" + name + "', type='" + type + "'}";
    }
}
