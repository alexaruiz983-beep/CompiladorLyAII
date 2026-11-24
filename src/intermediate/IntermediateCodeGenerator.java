package intermediate;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IntermediateCodeGenerator {
    private int tempCount = 0;
    private int labelCount = 0;
    private List<String> instructions;

    public IntermediateCodeGenerator() {
        this.instructions = new ArrayList<>();
    }

    public String newTemp() {
        tempCount++;
        return "t" + tempCount;
    }

    public String newLabel() {
        labelCount++;
        return "L" + labelCount;
    }

    public void emit(String instruction) {
        instructions.add(instruction);
    }

    // Muestra el código en pantalla (Consola)
    public void printCode() {
        System.out.println("=== CÓDIGO INTERMEDIO (TAC) ===");
        for (int i = 0; i < instructions.size(); i++) {
            System.out.println((i + 1) + ": " + instructions.get(i));
        }
        System.out.println("================================\n");
        
        // Opcional: Lo guarda automáticamente en un archivo por si el profesor lo pide
        saveToFile("codigo_intermedio.txt");
    }

    // Guarda el código intermedio en un archivo de texto (Cumple con el requerimiento)
    public void saveToFile(String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("=== CÓDIGO INTERMEDIO (TAC) ===\n");
            for (int i = 0; i < instructions.size(); i++) {
                writer.write((i + 1) + ": " + instructions.get(i) + "\n");
            }
            writer.write("================================\n");
            System.out.println("[INFO]: Código intermedio guardado exitosamente en '" + fileName + "'.\n");
        } catch (IOException e) {
            System.err.println("Error al guardar el archivo de código intermedio: " + e.getMessage());
        }
    }
}
