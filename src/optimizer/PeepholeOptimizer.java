package optimizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import optimizer.TACInstruction.Kind;

/**
 * Limpieza sobre la lista completa de instrucciones:
 *  - Eliminación de código inalcanzable (después de un goto, hasta la siguiente etiqueta)
 *  - Eliminación de saltos a la instrucción siguiente
 *  - Eliminación de etiquetas que nadie usa
 *  - Eliminación de código muerto: temporales (t1, t2, ...) que se asignan y nunca se leen
 *
 * Las variables del programa (no temporales) nunca se eliminan.
 */
public class PeepholeOptimizer {

    public boolean unreachableCode = true;
    public boolean jumpToNext      = true;
    public boolean unusedLabels    = true;
    public boolean deadCode        = true;

    public List<TACInstruction> optimize(List<TACInstruction> code) {
        List<TACInstruction> cur = copyAll(code);
        boolean changed = true;
        while (changed) {
            int before = signature(cur);
            if (unreachableCode) cur = removeUnreachable(cur);
            if (jumpToNext)      cur = removeJumpToNext(cur);
            if (unusedLabels)    cur = removeUnusedLabels(cur);
            if (deadCode)        cur = removeDeadTemps(cur);
            changed = signature(cur) != before;
        }
        return cur;
    }

    // ---------------------------------------------------------------
    private List<TACInstruction> removeUnreachable(List<TACInstruction> code) {
        List<TACInstruction> out = new ArrayList<>();
        boolean dead = false;
        for (TACInstruction i : code) {
            if (i.kind == Kind.LABEL) dead = false;
            if (!dead) out.add(i);
            if (i.kind == Kind.GOTO) dead = true;
        }
        return out;
    }

    /** goto L  seguido (directamente, o tras otras etiquetas) de  L:  */
    private List<TACInstruction> removeJumpToNext(List<TACInstruction> code) {
        List<TACInstruction> out = new ArrayList<>();
        for (int k = 0; k < code.size(); k++) {
            TACInstruction i = code.get(k);
            if (i.kind == Kind.GOTO) {
                boolean redundant = false;
                for (int j = k + 1; j < code.size() && code.get(j).kind == Kind.LABEL; j++) {
                    if (code.get(j).result.equals(i.result)) { redundant = true; break; }
                }
                if (redundant) continue;
            }
            out.add(i);
        }
        return out;
    }

    private List<TACInstruction> removeUnusedLabels(List<TACInstruction> code) {
        Set<String> used = new HashSet<>();
        for (TACInstruction i : code) if (i.isJump()) used.add(i.result);
        List<TACInstruction> out = new ArrayList<>();
        for (TACInstruction i : code) {
            if (i.kind == Kind.LABEL && !used.contains(i.result)) continue;
            out.add(i);
        }
        return out;
    }

    /** Quita 't = ...' si 't' es temporal y no se lee en ninguna parte. */
    private List<TACInstruction> removeDeadTemps(List<TACInstruction> code) {
        Map<String, Integer> uses = new HashMap<>();
        for (TACInstruction i : code)
            for (String u : i.uses()) uses.merge(u, 1, Integer::sum);

        List<TACInstruction> out = new ArrayList<>();
        for (TACInstruction i : code) {
            boolean pure = i.kind == Kind.ASSIGN || i.kind == Kind.BINARY;
            if (pure && isTemp(i.result) && !uses.containsKey(i.result)) continue;
            out.add(i);
        }
        return out;
    }

    // ---------------------------------------------------------------
    static boolean isTemp(String name) {
        return name != null && name.matches("t\\d+");
    }

    private static List<TACInstruction> copyAll(List<TACInstruction> code) {
        List<TACInstruction> out = new ArrayList<>();
        for (TACInstruction i : code) out.add(i.copy());
        return out;
    }

    private static int signature(List<TACInstruction> code) {
        return TACInstruction.format(code).hashCode() * 31 + code.size();
    }
}