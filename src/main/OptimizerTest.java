package main;

import java.util.*;

import optimizer.*;
import optimizer.TACInstruction.Kind;

/**
 * Pruebas del optimizador (sin JUnit, se ejecuta con main).
 *
 *   1) Salida esperada: cada técnica produce exactamente el TAC que debe.
 *   2) Equivalencia: se EJECUTA el TAC antes y después de optimizar
 *      con un mini intérprete y se comprueba que escribe lo mismo.
 */
public class OptimizerTest {

    static int ok = 0, fail = 0;

    public static void main(String[] args) {
        System.out.println("== 1. Salida esperada ==");
        expect("plegado de constantes",  "t1 = 2 * 3\nwrite t1",                "t1 = 6\nwrite t1");
        expect("plegado con reales",     "t1 = 1.5 + 2\nwrite t1",              "t1 = 3.5\nwrite t1");
        expect("division siempre real",  "t1 = 10 / 4\nwrite t1",               "t1 = 2.5\nwrite t1");
        expect("no pliega division /0",  "t1 = 4 / 0\nwrite t1",                "t1 = 4 / 0\nwrite t1");
        expect("x + 0",                  "read x\nt1 = x + 0\nwrite t1",        "read x\nt1 = x\nwrite t1");
        expect("0 + x",                  "read x\nt1 = 0 + x\nwrite t1",        "read x\nt1 = x\nwrite t1");
        expect("x - 0",                  "read x\nt1 = x - 0\nwrite t1",        "read x\nt1 = x\nwrite t1");
        expect("x * 1",                  "read x\nt1 = x * 1\nwrite t1",        "read x\nt1 = x\nwrite t1");
        expect("1 * x",                  "read x\nt1 = 1 * x\nwrite t1",        "read x\nt1 = x\nwrite t1");
        expect("x * 0",                  "read x\nt1 = x * 0\nwrite t1",        "read x\nt1 = 0\nwrite t1");
        expect("x / 1",                  "read x\nt1 = x / 1\nwrite t1",        "read x\nt1 = x\nwrite t1");
        expect("no toca x + y",          "read x\nread y\nt1 = x + y\nwrite t1","read x\nread y\nt1 = x + y\nwrite t1");
        expect("temporal sin uso",       "read x\nt1 = x + 1\nwrite x",         "read x\nwrite x");
        expect("variable sin uso se conserva", "read x\ny = x + 1\nwrite x",    "read x\ny = x + 1\nwrite x");
        expect("codigo inalcanzable",    "goto L1\nx = 5\nL1:\nwrite x",        "write x");
        expect("while no se altera",
               "i = 0\nL1:\nt1 = i < 10\nif t1 goto L2\ngoto L3\nL2:\nt2 = i + 1\ni = t2\ngoto L1\nL3:\nwrite i",
               "i = 0\nL1:\nt1 = i < 10\nif t1 goto L2\ngoto L3\nL2:\nt2 = i + 1\ni = t2\ngoto L1\nL3:\nwrite i");

        // banderas
        Optimizer o = new Optimizer();
        o.local.constantFolding = false;
        check("bandera constantFolding=false", o.optimize("t1 = 2 * 3\nwrite t1").trim().equals("t1 = 2 * 3\nwrite t1"));
        o = new Optimizer();
        o.local.algebraic = false;
        check("bandera algebraic=false", o.optimize("read x\nt1 = x + 0\nwrite t1").trim().equals("read x\nt1 = x + 0\nwrite t1"));

        // la lista original no se modifica
        List<TACInstruction> original = TACInstruction.parseAll("t1 = 2 * 3\nwrite t1");
        String antes = TACInstruction.format(original);
        new Optimizer().optimize(original);
        check("no modifica la lista original", TACInstruction.format(original).equals(antes));

        System.out.println("\n== 2. Equivalencia (mismo resultado antes y despues) ==");
        equivalent("suma 1..n (while)",
            "read n\ni = 1\ns = 0\nL1:\nt1 = i <= n\nif t1 goto L2\ngoto L3\nL2:\nt2 = s + i\ns = t2\nt3 = i + 1\ni = t3\ngoto L1\nL3:\nwrite s",
            new double[]{5}, new double[]{0}, new double[]{10});
        equivalent("promedio de 4 calificaciones",
            "read c1\nread c2\nread c3\nread c4\nt1 = c1 + c2\nt2 = t1 + c3\nt3 = t2 + c4\nsuma = t3\nt4 = suma / 4\npromedio = t4\nwrite promedio",
            new double[]{8, 9, 10, 7}, new double[]{0, 0, 0, 0});
        equivalent("if / else con constantes",
            "read x\nt1 = x > 5\nif t1 goto L1\nt2 = x * 1\ny = t2\ngoto L2\nL1:\nt3 = x + 0\nt4 = t3 * 2\ny = t4\nL2:\nwrite y",
            new double[]{3}, new double[]{9});
        equivalent("constantes y algebra mezclados",
            "read x\nt1 = 2 * 3\nt2 = x + 0\nt3 = t1 * t2\nt4 = 0 * x\nt5 = t3 + t4\nwrite t5",
            new double[]{4}, new double[]{-2});

        // programas aleatorios: lineales, con +, -, *, / y constantes
        Random rnd = new Random(42);
        String[] ops = {"+", "-", "*", "/"}, vars = {"a", "b", "c"};
        String[] consts = {"0", "1", "2", "3", "2.5", "0.0", "1.0"};
        int probados = 0;
        for (int n = 0; n < 1000; n++) {
            StringBuilder sb = new StringBuilder("read a\nread b\nc = 1\n");
            int t = 0;
            for (int k = 0; k < 10; k++) {
                String x = operand(rnd, vars, consts, t), y = operand(rnd, vars, consts, t);
                String dst = rnd.nextBoolean() ? "t" + (++t) : vars[rnd.nextInt(3)];
                sb.append(dst).append(" = ").append(x).append(' ').append(ops[rnd.nextInt(4)]).append(' ').append(y).append('\n');
            }
            sb.append("write a\nwrite b\nwrite c\n");
            double[] in = {rnd.nextInt(6), rnd.nextInt(6)};
            List<TACInstruction> prog = TACInstruction.parseAll(sb.toString());
            List<Double> esperado = run(prog, in);
            if (!esperado.stream().allMatch(d -> !d.isNaN() && !d.isInfinite())) continue;   // ignora /0
            probados++;
            List<TACInstruction> opt = new Optimizer().optimize(prog);
            boolean igual = iguales(esperado, run(opt, in));
            boolean idem = TACInstruction.format(new Optimizer().optimize(opt)).equals(TACInstruction.format(opt));
            boolean noCrece = opt.size() <= prog.size();
            if (!(igual && idem && noCrece)) {
                fail++;
                System.out.println("FALLO aleatorio (igual=" + igual + ", idempotente=" + idem + ", noCrece=" + noCrece + "):\n"
                    + sb + "--- optimizado\n" + TACInstruction.format(opt));
                break;
            }
        }
        check("1000 programas aleatorios (" + probados + " sin division por cero)", fail == 0);

        System.out.println("\n" + ok + " pruebas correctas, " + fail + " fallidas");
        if (fail > 0) System.exit(1);
    }

    // ---------------------------------------------------------------
    static void expect(String name, String tac, String esperado) {
        String real = new Optimizer().optimize(tac).trim();
        if (real.equals(esperado)) { ok++; System.out.println("  OK     " + name); }
        else { fail++; System.out.println("  FALLO  " + name + "\n    esperado:\n" + indent(esperado) + "\n    obtenido:\n" + indent(real)); }
    }

    static void equivalent(String name, String tac, double[]... entradas) {
        List<TACInstruction> prog = TACInstruction.parseAll(tac);
        List<TACInstruction> opt = new Optimizer().optimize(prog);
        boolean todo = true;
        for (double[] in : entradas) todo &= iguales(run(prog, in), run(opt, in));
        check(name + "  (" + prog.size() + " -> " + opt.size() + " instrucciones)", todo);
    }

    static void check(String name, boolean cond) {
        if (cond) { ok++; System.out.println("  OK     " + name); }
        else { fail++; System.out.println("  FALLO  " + name); }
    }

    static String indent(String s) { return "      " + s.replace("\n", "\n      "); }

    static String operand(Random r, String[] vars, String[] consts, int temps) {
        int k = r.nextInt(3);
        if (k == 0) return vars[r.nextInt(vars.length)];
        if (k == 1 || temps == 0) return consts[r.nextInt(consts.length)];
        return "t" + (1 + r.nextInt(temps));
    }

    static boolean iguales(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++)
            if (Math.abs(a.get(i) - b.get(i)) > 1e-9) return false;
        return true;
    }

    // ---------------- mini intérprete de TAC ----------------
    static List<Double> run(List<TACInstruction> code, double[] in) {
        Map<String, Double> v = new HashMap<>();
        Map<String, Integer> label = new HashMap<>();
        for (int i = 0; i < code.size(); i++)
            if (code.get(i).kind == Kind.LABEL) label.put(code.get(i).result, i);

        List<Double> out = new ArrayList<>();
        int next = 0;
        for (int pc = 0, steps = 0; pc < code.size() && steps++ < 100000; pc++) {
            TACInstruction i = code.get(pc);
            switch (i.kind) {
                case READ:    v.put(i.result, in[next++]); break;
                case WRITE:   out.add(val(v, i.arg1)); break;
                case ASSIGN:  v.put(i.result, val(v, i.arg1)); break;
                case BINARY:  v.put(i.result, calc(val(v, i.arg1), i.op, val(v, i.arg2))); break;
                case GOTO:    pc = label.get(i.result); break;
                case IF_GOTO: if (val(v, i.arg1) != 0) pc = label.get(i.result); break;
                default:      break;
            }
        }
        return out;
    }

    static double val(Map<String, Double> v, String s) {
        return s.matches("-?\\d+(\\.\\d+)?") ? Double.parseDouble(s) : v.getOrDefault(s, 0.0);
    }

    static double calc(double a, String op, double b) {
        switch (op) {
            case "+": return a + b;   case "-": return a - b;
            case "*": return a * b;   case "/": return a / b;
            case "=": return a == b ? 1 : 0;   case "<>": return a != b ? 1 : 0;
            case "<": return a < b ? 1 : 0;    case "<=": return a <= b ? 1 : 0;
            case ">": return a > b ? 1 : 0;    case ">=": return a >= b ? 1 : 0;
            case "AND": return (a != 0 && b != 0) ? 1 : 0;
            case "OR":  return (a != 0 || b != 0) ? 1 : 0;
            default: throw new IllegalArgumentException("operador " + op);
        }
    }
}