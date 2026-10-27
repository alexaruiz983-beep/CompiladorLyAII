package optimizer;

import java.util.List;

/**
 * Punto de entrada del optimizador.
 *
 *   List<TACInstruction> opt = new Optimizer().optimize(tac);
 *
 * Aplica LocalOptimizer y PeepholeOptimizer repetidamente hasta que el código
 * ya no cambie (punto fijo), porque una técnica suele habilitar a otra
 * (p. ej. la propagación deja temporales sin uso que el código muerto elimina).
 *
 * Cada técnica se activa/desactivando sus banderas, para poder mostrar
 * el código antes y después:
 *
 *   Optimizer o = new Optimizer();
 *   o.local.constantFolding = false;
 */
public class Optimizer {

    public final LocalOptimizer local = new LocalOptimizer();
    public final PeepholeOptimizer peephole = new PeepholeOptimizer();

    /** Máximo de vueltas, por seguridad. */
    private static final int MAX_PASSES = 50;

    /** Recibe TAC y devuelve TAC equivalente. No modifica la lista original. */
    public List<TACInstruction> optimize(List<TACInstruction> code) {
        List<TACInstruction> cur = code;
        String previous = TACInstruction.format(cur);

        for (int pass = 0; pass < MAX_PASSES; pass++) {
            cur = local.optimize(cur);
            cur = peephole.optimize(cur);

            String now = TACInstruction.format(cur);
            if (now.equals(previous)) break;
            previous = now;
        }
        return cur;
    }

    /** Variante de texto a texto: útil para pruebas y para el contrato por archivos. */
    public String optimize(String tacText) {
        return TACInstruction.format(optimize(TACInstruction.parseAll(tacText)));
    }
}