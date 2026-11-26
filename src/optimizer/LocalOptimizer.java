package optimizer;

import java.util.ArrayList;
import java.util.List;

import optimizer.TACInstruction.Kind;

/**
 * Optimización local con dos técnicas:
 *  - Plegado de constantes:       t1 = 2 * 3   ->  t1 = 6
 *  - Simplificación algebraica:   t2 = x + 0   ->  t2 = x
 */
public class LocalOptimizer {

    public boolean constantFolding = true;
    public boolean algebraic       = true;

    public List<TACInstruction> optimize(List<TACInstruction> code) {
        List<TACInstruction> out = new ArrayList<>();
        for (TACInstruction in : code) {
            TACInstruction i = in.copy();
            if (i.kind == Kind.BINARY) simplify(i);
            out.add(i);
        }
        return out;
    }

    /** Si la operación se puede simplificar, la convierte en una asignación simple. */
    private void simplify(TACInstruction i) {
        String r = null;
        if (constantFolding && isNum(i.arg1) && isNum(i.arg2)) r = fold(i.arg1, i.op, i.arg2);
        if (r == null && algebraic) r = algebraic(i.arg1, i.op, i.arg2);

        if (r != null) {
            i.kind = Kind.ASSIGN;
            i.arg1 = r;
            i.op = null;
            i.arg2 = null;
        }
    }

    /** Calcula 'x op y' cuando ambos son números. Devuelve null si no se puede. */
    private static String fold(String x, String op, String y) {
        double a = Double.parseDouble(x), b = Double.parseDouble(y);
        boolean real = x.contains(".") || y.contains(".") || op.equals("/");  // '/' siempre da REAL
        double r;
        switch (op) {
            case "+": r = a + b; break;
            case "-": r = a - b; break;
            case "*": r = a * b; break;
            case "/": if (b == 0) return null; r = a / b; break;
            default:  return null;
        }
        return real ? String.valueOf(r) : String.valueOf((long) r);
    }

    /** Identidades: x+0, 0+x, x-0, x*1, 1*x, x*0, 0*x, x/1. Devuelve el resultado o null. */
    private static String algebraic(String a, String op, String b) {
        switch (op) {
            case "+": return is(b, 0) ? a : is(a, 0) ? b : null;
            case "-": return is(b, 0) ? a : null;
            case "*": return is(b, 1) ? a : is(a, 1) ? b : (is(a, 0) || is(b, 0)) ? "0" : null;
            case "/": return is(b, 1) ? a : null;
            default:  return null;
        }
    }

    private static boolean isNum(String s) {
        return s != null && s.matches("-?\\d+(\\.\\d+)?");
    }

    /** ¿s es la constante v? */
    private static boolean is(String s, double v) {
        return isNum(s) && Double.parseDouble(s) == v;
    }
}
