package optimizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import optimizer.TACInstruction.Kind;

/**
 * Limpieza sobre la lista completa de instrucciones:
 *  - código inalcanzable (después de un goto, hasta la siguiente etiqueta)
 *  - goto a la instrucción siguiente
 *  - etiquetas que nadie usa
 *  - temporales (t1, t2, ...) que se asignan y nunca se leen
 */
public class PeepholeOptimizer {

    public List<TACInstruction> optimize(List<TACInstruction> code) {
        List<TACInstruction> c = new ArrayList<>(code);
        removeUnreachable(c);
        removeJumpToNext(c);
        removeUnusedLabels(c);
        removeDeadTemps(c);
        return c;
    }

    private void removeUnreachable(List<TACInstruction> c) {
        for (int i = 0; i < c.size(); i++)
            if (c.get(i).kind == Kind.GOTO)
                while (i + 1 < c.size() && c.get(i + 1).kind != Kind.LABEL) c.remove(i + 1);
    }

    private void removeJumpToNext(List<TACInstruction> c) {
        for (int i = 0; i + 1 < c.size(); i++) {
            TACInstruction a = c.get(i), b = c.get(i + 1);
            if (a.kind == Kind.GOTO && b.kind == Kind.LABEL && a.result.equals(b.result)) c.remove(i--);
        }
    }

    private void removeUnusedLabels(List<TACInstruction> c) {
        Set<String> used = new HashSet<>();
        for (TACInstruction i : c)
            if (i.kind == Kind.GOTO || i.kind == Kind.IF_GOTO) used.add(i.result);
        c.removeIf(i -> i.kind == Kind.LABEL && !used.contains(i.result));
    }

    private void removeDeadTemps(List<TACInstruction> c) {
        Set<String> read = new HashSet<>();           // todo lo que alguna instrucción lee
        for (TACInstruction i : c) { read.add(i.arg1); read.add(i.arg2); }
        c.removeIf(i -> (i.kind == Kind.ASSIGN || i.kind == Kind.BINARY)
                     && i.result.matches("t\\d+") && !read.contains(i.result));
    }
}
