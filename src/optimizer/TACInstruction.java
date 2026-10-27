package optimizer;

/**
 * Instrucción de código de tres direcciones.
 *
 * Formato de texto:
 *   t1 = a + b      BINARY
 *   x = t1          ASSIGN
 *   L1:             LABEL
 *   goto L1         GOTO
 *   if t1 goto L2   IF_GOTO
 *   read x          READ
 *   write x         WRITE
 *
 * NOTA: si el equipo de código intermedio (P2) ya tiene su propia clase,
 * basta con adaptar los campos o escribir un conversor hacia/desde esta.
 */
public class TACInstruction {

    public enum Kind { ASSIGN, BINARY, LABEL, GOTO, IF_GOTO, READ, WRITE }

    public Kind kind;
    public String result;  // destino, o nombre de etiqueta en LABEL / GOTO / IF_GOTO
    public String arg1;
    public String op;
    public String arg2;

    public TACInstruction(Kind kind, String result, String arg1, String op, String arg2) {
        this.kind = kind;
        this.result = result;
        this.arg1 = arg1;
        this.op = op;
        this.arg2 = arg2;
    }

    // ---- constructores de conveniencia ----
    public static TACInstruction assign(String r, String a)              { return new TACInstruction(Kind.ASSIGN, r, a, null, null); }
    public static TACInstruction binary(String r, String a, String op, String b) { return new TACInstruction(Kind.BINARY, r, a, op, b); }
    public static TACInstruction label(String l)                         { return new TACInstruction(Kind.LABEL, l, null, null, null); }
    public static TACInstruction jump(String l)                          { return new TACInstruction(Kind.GOTO, l, null, null, null); }
    public static TACInstruction ifGoto(String cond, String l)           { return new TACInstruction(Kind.IF_GOTO, l, cond, null, null); }
    public static TACInstruction read(String v)                          { return new TACInstruction(Kind.READ, v, null, null, null); }
    public static TACInstruction write(String v)                         { return new TACInstruction(Kind.WRITE, null, v, null, null); }

    public TACInstruction copy() {
        return new TACInstruction(kind, result, arg1, op, arg2);
    }

    /** Variable que esta instrucción define (escribe), o null. */
    public String defines() {
        switch (kind) {
            case ASSIGN: case BINARY: case READ: return result;
            default: return null;
        }
    }

    /** Operandos que esta instrucción lee. */
    public java.util.List<String> uses() {
        java.util.List<String> u = new java.util.ArrayList<>();
        switch (kind) {
            case ASSIGN: case IF_GOTO: case WRITE: u.add(arg1); break;
            case BINARY: u.add(arg1); u.add(arg2); break;
            default: break;
        }
        return u;
    }

    public boolean isJump() { return kind == Kind.GOTO || kind == Kind.IF_GOTO; }

    // ---- texto <-> objeto ----
    public static TACInstruction parse(String line) {
        line = line.trim();
        if (line.endsWith(":"))             return label(line.substring(0, line.length() - 1).trim());
        if (line.startsWith("goto "))       return jump(line.substring(5).trim());
        if (line.startsWith("read "))       return read(line.substring(5).trim());
        if (line.startsWith("write "))      return write(line.substring(6).trim());
        if (line.startsWith("if ")) {
            String[] p = line.substring(3).trim().split("\\s+goto\\s+");
            return ifGoto(p[0].trim(), p[1].trim());
        }
        String[] sides = line.split("\\s*=\\s*", 2);
        if (sides.length != 2) throw new IllegalArgumentException("TAC inválido: " + line);
        String[] rhs = sides[1].trim().split("\\s+");
        if (rhs.length == 1) return assign(sides[0].trim(), rhs[0]);
        if (rhs.length == 3) return binary(sides[0].trim(), rhs[0], rhs[1], rhs[2]);
        throw new IllegalArgumentException("TAC inválido: " + line);
    }

    public static java.util.List<TACInstruction> parseAll(String text) {
        java.util.List<TACInstruction> out = new java.util.ArrayList<>();
        for (String l : text.split("\\R")) {
            if (!l.trim().isEmpty()) out.add(parse(l));
        }
        return out;
    }

    public static String format(java.util.List<TACInstruction> code) {
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