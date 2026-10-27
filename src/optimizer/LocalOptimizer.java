package optimizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import optimizer.TACInstruction.Kind;

/**
 * Optimización local: trabaja dentro de cada bloque básico
 * (un bloque empieza en una etiqueta y termina en un salto).
 *
 * Técnicas (cada una se puede activar/desactivar):
 *  - Plegado de constantes          t1 = 2 * 3     ->  t1 = 6
 *  - Simplificación algebraica      x + 0, x * 1, x * 0 ...
 *  - Reducción de fuerza            x * 2          ->  x + x
 *  - Propagación de constantes y copias
 *  - Eliminación de subexpresiones comunes (local)
 *  - Salto condicional con condición constante
 */
public class LocalOptimizer {

    public boolean constantFolding   = true;
    public boolean algebraic         = true;
    public boolean strengthReduction = true;
    public boolean propagation       = true;
    public boolean commonSubexpr     = true;

    // ---------------------------------------------------------------
    public List<TACInstruction> optimize(List<TACInstruction> code) {
        List<TACInstruction> out = new ArrayList<>();
        List<TACInstruction> block = new ArrayList<>();

        for (TACInstruction in : code) {
            if (in.kind == Kind.LABEL) {
                optimizeBlock(block, out);
                block.clear();
                out.add(in.copy());
                continue;
            }
            block.add(in.copy());
            if (in.isJump()) {
                optimizeBlock(block, out);
                block.clear();
            }
        }
        optimizeBlock(block, out);
        return out;
    }

    // ---------------------------------------------------------------
    /** Expresión disponible: holder = a op b */
    private static class Expr {
        final String a, op, b, holder;
        Expr(String a, String op, String b, String holder) {
            this.a = a; this.op = op; this.b = b; this.holder = holder;
        }
    }

    private void optimizeBlock(List<TACInstruction> block, List<TACInstruction> out) {
        Map<String, String> known = new HashMap<>();   // var -> constante o variable equivalente
        List<Expr> available = new ArrayList<>();

        for (TACInstruction ins : block) {

            // 1) propagación: sustituir operandos por su valor conocido
            if (propagation) {
                if (ins.arg1 != null) ins.arg1 = known.getOrDefault(ins.arg1, ins.arg1);
                if (ins.arg2 != null) ins.arg2 = known.getOrDefault(ins.arg2, ins.arg2);
            }

            // 2) simplificar la instrucción
            if (ins.kind == Kind.BINARY) {
                simplifyBinary(ins);
            }
            if (ins.kind == Kind.BINARY && commonSubexpr) {
                for (Expr e : available) {
                    if (e.a.equals(ins.arg1) && e.op.equals(ins.op) && e.b.equals(ins.arg2)) {
                        ins.kind = Kind.ASSIGN;
                        ins.arg1 = e.holder;
                        ins.op = null;
                        ins.arg2 = null;
                        break;
                    }
                }
            }

            // 3) salto condicional con condición constante
            if (ins.kind == Kind.IF_GOTO && isNum(ins.arg1)) {
                if (Double.parseDouble(ins.arg1) != 0) {
                    out.add(TACInstruction.jump(ins.result));
                }
                // si es falso, la instrucción desaparece
                continue;
            }

            // x = x  no hace nada
            if (ins.kind == Kind.ASSIGN && ins.result.equals(ins.arg1)) {
                continue;
            }

            // 4) actualizar el estado según lo que define la instrucción
            String def = ins.defines();
            if (def != null) {
                kill(def, known, available);
                if (ins.kind == Kind.ASSIGN && propagation) {
                    known.put(def, ins.arg1);
                }
                if (ins.kind == Kind.BINARY && commonSubexpr
                        && !def.equals(ins.arg1) && !def.equals(ins.arg2)) {
                    available.add(new Expr(ins.arg1, ins.op, ins.arg2, def));
                }
            }
            out.add(ins);
        }
    }

    /** Invalida todo lo que dependa de 'v' (porque se redefine). */
    private void kill(String v, Map<String, String> known, List<Expr> available) {
        known.remove(v);
        known.values().removeIf(val -> val.equals(v));
        available.removeIf(e -> e.holder.equals(v) || e.a.equals(v) || e.b.equals(v));
    }

    // ---------------------------------------------------------------
    private void simplifyBinary(TACInstruction ins) {
        // plegado de constantes
        if (constantFolding && isNum(ins.arg1) && isNum(ins.arg2)) {
            String v = fold(ins.arg1, ins.op, ins.arg2);
            if (v != null) { toAssign(ins, v); return; }
        }
        if (algebraic && simplifyAlgebraic(ins)) return;
        if (strengthReduction) reduceStrength(ins);
    }

    private boolean simplifyAlgebraic(TACInstruction ins) {
        String a = ins.arg1, b = ins.arg2, op = ins.op;
        boolean aNum = isNum(a), bNum = isNum(b);

        switch (op) {
            case "+":
                if (bNum && val(b) == 0) { toAssign(ins, a); return true; }
                if (aNum && val(a) == 0) { toAssign(ins, b); return true; }
                break;
            case "-":
                if (bNum && val(b) == 0) { toAssign(ins, a); return true; }
                if (a.equals(b))         { toAssign(ins, "0"); return true; }
                break;
            case "*":
                if (bNum && val(b) == 1) { toAssign(ins, a); return true; }
                if (aNum && val(a) == 1) { toAssign(ins, b); return true; }
                if (bNum && val(b) == 0) { toAssign(ins, b); return true; }
                if (aNum && val(a) == 0) { toAssign(ins, a); return true; }
                break;
            case "/":
                if (bNum && val(b) == 1) { toAssign(ins, a); return true; }
                break;
            default:
                break;
        }
        // AND / OR con constante (true = distinto de 0)
        if (op.equalsIgnoreCase("AND")) {
            if (bNum) { toAssign(ins, val(b) == 0 ? "0" : a); return true; }
            if (aNum) { toAssign(ins, val(a) == 0 ? "0" : b); return true; }
        }
        if (op.equalsIgnoreCase("OR")) {
            if (bNum) { toAssign(ins, val(b) != 0 ? "1" : a); return true; }
            if (aNum) { toAssign(ins, val(a) != 0 ? "1" : b); return true; }
        }
        return false;
    }

    /** x * 2  ->  x + x   (2 * x también) */
    private void reduceStrength(TACInstruction ins) {
        if (!ins.op.equals("*")) return;
        if (isNum(ins.arg2) && val(ins.arg2) == 2 && !isNum(ins.arg1)) {
            ins.op = "+"; ins.arg2 = ins.arg1;
        } else if (isNum(ins.arg1) && val(ins.arg1) == 2 && !isNum(ins.arg2)) {
            ins.op = "+"; ins.arg1 = ins.arg2;
        }
    }

    private void toAssign(TACInstruction ins, String value) {
        ins.kind = Kind.ASSIGN;
        ins.arg1 = value;
        ins.op = null;
        ins.arg2 = null;
    }

    // ---------------------------------------------------------------
    // Utilidades sobre constantes numéricas
    // ---------------------------------------------------------------
    static boolean isNum(String s) {
        return s != null && s.matches("-?\\d+(\\.\\d+)?");
    }

    static boolean isReal(String s) {
        return s.contains(".");
    }

    static double val(String s) {
        return Double.parseDouble(s);
    }

    /** Evalúa 'a op b' con constantes. Devuelve null si no se puede plegar. */
    static String fold(String a, String op, String b) {
        double x = val(a), y = val(b);
        boolean real = isReal(a) || isReal(b);

        switch (op.toUpperCase()) {
            case "+": return num(x + y, real);
            case "-": return num(x - y, real);
            case "*": return num(x * y, real);
            case "/": return y == 0 ? null : num(x / y, true);   // '/' siempre da REAL
            case "=":  return bool(x == y);
            case "<>": return bool(x != y);
            case "<":  return bool(x < y);
            case "<=": return bool(x <= y);
            case ">":  return bool(x > y);
            case ">=": return bool(x >= y);
            case "AND": return bool(x != 0 && y != 0);
            case "OR":  return bool(x != 0 || y != 0);
            default:    return null;
        }
    }

    private static String num(double d, boolean real) {
        if (real) return String.valueOf(d);
        return String.valueOf((long) d);
    }

    private static String bool(boolean b) {
        return b ? "1" : "0";
    }
}