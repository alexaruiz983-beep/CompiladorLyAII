package optimizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Una instrucción de código de tres direcciones. Formato de texto:
 *   t1 = a + b     BINARY      x = t1        ASSIGN
 *   L1:            LABEL       goto L1       GOTO
 *   if t1 goto L2  IF_GOTO     read x        READ
 *   write x        WRITE
 */
public class TACInstruction {

    public enum Kind { ASSIGN, BINARY, LABEL, GOTO, IF_GOTO, READ, WRITE }

    public Kind kind;
    public String result;   // destino, o nombre de la etiqueta en LABEL / GOTO / IF_GOTO
    public String arg1, op, arg2;

    public TACInstruction(Kind kind, String result, String arg1, String op, String arg2) {
        this.kind = kind;
        this.result = result;
        this.arg1 = arg1;
        this.op = op;
        this.arg2 = arg2;
    }

    public TACInstruction copy() {
        return new TACInstruction(kind, result, arg1, op, arg2);
    }

    /**
     * Convierte una línea de texto en instrucción (los tokens van separados por espacios).
     * Acepta también el formato del generador del equipo:  "3: t1 := a + b"
     * (quita el número de línea "3:" y cambia ":=" por "=").
     */
    public static TACInstruction parse(String line) {
        String[] t = line.trim().replaceFirst("^\\d+:\\s*", "").replace(":=", "=").split("\\s+");
        if (t[0].endsWith(":")) return new TACInstruction(Kind.LABEL, t[0].substring(0, t[0].length() - 1), null, null, null);
        switch (t[0]) {
            case "goto":  return new TACInstruction(Kind.GOTO, t[1], null, null, null);
            case "read":  return new TACInstruction(Kind.READ, t[1], null, null, null);
            case "write": return new TACInstruction(Kind.WRITE, null, t[1], null, null);
            case "if":    return new TACInstruction(Kind.IF_GOTO, t[3], t[1], null, null);   // if cond goto L
        }
        // t[0] = t[2]     o     t[0] = t[2] op t[4]
        return t.length == 3 ? new TACInstruction(Kind.ASSIGN, t[0], t[2], null, null)
                             : new TACInstruction(Kind.BINARY, t[0], t[2], t[3], t[4]);
    }

    public static List<TACInstruction> parseAll(String text) {
        List<TACInstruction> out = new ArrayList<>();
        for (String line : text.split("\\R"))
            if (!line.trim().isEmpty() && !line.startsWith("=")) out.add(parse(line));   // salta encabezado "=== ... ==="
        return out;
    }

    public static String format(List<TACInstruction> code) {
        StringBuilder sb = new StringBuilder();
        for (TACInstruction i : code) sb.append(i).append('\n');
        return sb.toString();
    }

    @Override
    public String toString() {
        switch (kind) {
            case LABEL:   return result + ":";
            case GOTO:    return "goto " + result;
            case IF_GOTO: return "if " + arg1 + " goto " + result;
            case READ:    return "read " + result;
            case WRITE:   return "write " + arg1;
            case ASSIGN:  return result + " = " + arg1;
            default:      return result + " = " + arg1 + " " + op + " " + arg2;
        }
    }
}