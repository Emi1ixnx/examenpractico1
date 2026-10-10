package aplicacion;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import experimentos.GeneradorPruebas;
import experimentos.Resultado;
import modelo.Imagen;

/**
 * Ejecuta todo el flujo reproducible:
 *  1. Evidencia: original, reconstruida y métricas de cada caso en resultados/ (fotos de imagenes/
 *     si las hay, más imágenes generadas).
 *  2. resultados/experimentos.csv       -> matriz obligatoria (27 filas).
 *  3. resultados/experimentos_extra.csv -> dimensiones impares/no cuadradas y 2 tipos más.
 */
public class Main {
    private static final int CALENTAMIENTOS_EVIDENCIA = 1;
    private static final int REPETICIONES_EVIDENCIA = 3;

    public static void main(String[] args) {
        try {
            System.out.println("=== Compresión por Quadtree ===");

            new File("resultados").mkdirs();
            File carpeta = new File("imagenes");
            carpeta.mkdirs();

            // 1. Fotos propias (carpeta imagenes/), en orden alfabético para que sea reproducible.
            File[] fotos = carpeta.listFiles((dir, nombre) -> {
                String n = nombre.toLowerCase();
                return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                        || n.endsWith(".bmp") || n.endsWith(".gif");
            });

            if (fotos == null || fotos.length == 0) {
                System.out.println("\n(No hay fotos en la carpeta 'imagenes/'. Solo se usarán imágenes generadas.)");
            } else {
                Arrays.sort(fotos);
                for (File foto : fotos) {
                    try {
                        Imagen imagen = Imagen.desdeArchivo(foto.getPath());
                        String base = foto.getName().replaceFirst("\\.[^.]+$", "");
                        for (int tol : GeneradorPruebas.TOLERANCIAS) {
                            ejecutarCaso(base + "_tol" + tol, imagen, tol);
                        }
                    } catch (IOException e) {
                        System.out.println("\nSe omite '" + foto.getName() + "': " + e.getMessage());
                    }
                }
            }

            // 2. Imágenes generadas: tres ejemplos de evidencia y uno con dimensiones impares.
            ejecutarCaso("uniforme", GeneradorPruebas.crearPorTipo("uniforme", 128), 0);
            ejecutarCaso("regiones", GeneradorPruebas.crearPorTipo("regiones", 128), 20);
            ejecutarCaso("detallada", GeneradorPruebas.crearPorTipo("detallada", 128), 50);
            ejecutarCaso("circulo_129x97", GeneradorPruebas.crearPorTipo("circulo", 129, 97), 20);

            // 3. Experimentos completos.
            System.out.println("\nEjecutando la matriz obligatoria (27 experimentos)...");
            GeneradorPruebas.guardarCSV("resultados/experimentos.csv", GeneradorPruebas.ejecutarMatrizObligatoria());
            System.out.println("Ejecutando el experimento adicional (dimensiones impares)...");
            GeneradorPruebas.guardarCSV("resultados/experimentos_extra.csv", GeneradorPruebas.ejecutarMatrizExtra());

            System.out.println("\nCSV generados en: resultados/experimentos.csv y resultados/experimentos_extra.csv");
            System.out.println("Evidencia (imágenes y métricas) en: resultados/");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void ejecutarCaso(String nombre, Imagen original, int tolerancia) throws IOException {
        Resultado r = GeneradorPruebas.medir(nombre, original, tolerancia,
                CALENTAMIENTOS_EVIDENCIA, REPETICIONES_EVIDENCIA);

        System.out.println();
        System.out.print(r.aTexto());

        original.guardar("resultados/" + nombre + "_original.png");
        r.reconstruida.guardar("resultados/" + nombre + "_reconstruida.png");
        Files.write(Paths.get("resultados/" + nombre + "_metricas.txt"),
                r.aTexto().getBytes(StandardCharsets.UTF_8));
    }
}
