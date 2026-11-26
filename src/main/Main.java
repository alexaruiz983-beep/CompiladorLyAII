package proyecto_lya2;

public class MainTestIntegrante1 {
    public static void main(String[] args) {
        // Crear el parser que abre programa.txt y ejecuta el análisis semántico
        SimpleParser parser = new SimpleParser();

        // Ejecutar el proceso sobre el archivo
        parser.parseFile("programa.txt");
    }
}
