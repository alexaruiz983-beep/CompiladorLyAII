package optimizer;

import java.util.List;

/**
 * Punto de entrada del optimizador:  new Optimizer().optimize(tac)
 *
 * Repite LocalOptimizer y PeepholeOptimizer hasta que el código ya no cambie,
 * porque una técnica puede dejar trabajo a la otra.
 * Para mostrar el antes y el después se apagan las banderas:  opt.local.algebraic = false;
 */
public class Optimizer {

    public final LocalOptimizer local = new LocalOptimizer();
    private final PeepholeOptimizer peephole = new PeepholeOptimizer();

    public List<TACInstruction> optimize(List<TACInstruction> code) {
        String before;
        do {
            before = TACInstruction.format(code);
            code = peephole.optimize(local.optimize(code));
        } while (!TACInstruction.format(code).equals(before));
        return code;
    }

    /** Variante de texto a texto, útil para pruebas. */
    public String optimize(String tac) {
        return TACInstruction.format(optimize(TACInstruction.parseAll(tac)));
    }
}
