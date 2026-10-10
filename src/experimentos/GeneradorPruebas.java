package experimentos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import modelo.Imagen;
import modelo.Quadtree;
import servicio.CompresorQuadtree;
import servicio.MetricasImagen;

/**
 * Genera imágenes de prueba reproducibles (sin archivos externos), mide el Quadtree sobre ellas
 * y escribe los resultados en CSV.
 */
public class GeneradorPruebas {
    /** Ejecuciones previas descartadas para que la JVM compile el código antes de medir. */
    public static final int CALENTAMIENTOS = 3;
    /** Ejecuciones medidas; se reporta la mediana de cada tiempo. */
    public static final int REPETICIONES = 7;

    public static final int[] TAMANOS = {64, 128, 256};
    public static final int[] TOLERANCIAS = {0, 20, 50};
    public static final String[] TIPOS_OBLIGATORIOS = {"uniforme", "regiones", "detallada"};
    public static final String[] TIPOS_EXTRA = {"circulo", "ruido"};

    /** Dimensiones no potencia de dos (y no cuadradas) para el experimento adicional. */
    private static final int[][] DIMENSIONES_EXTRA = {{100, 75}, {129, 97}};
    private static final long SEMILLA_RUIDO = 42L;

    // ---------------------------------------------------------------- Imágenes

    public static Imagen crearImagenUniforme(int ancho, int alto, int r, int g, int b) {
        int[][][] datos = new int[alto][ancho][3];
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                datos[y][x][0] = r;
                datos[y][x][1] = g;
                datos[y][x][2] = b;
            }
        }
        return new Imagen(datos);
    }

    /** Cuatro cuadrantes de colores planos, alineados con la partición del Quadtree (mejor caso). */
    public static Imagen crearImagenConRegiones(int ancho, int alto) {
        int[][][] datos = new int[alto][ancho][3];
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                if (x < ancho / 2 && y < alto / 2) {
                    datos[y][x] = new int[] {255, 0, 0};
                } else if (x >= ancho / 2 && y < alto / 2) {
                    datos[y][x] = new int[] {0, 255, 0};
                } else if (x < ancho / 2 && y >= alto / 2) {
                    datos[y][x] = new int[] {0, 0, 255};
                } else {
                    datos[y][x] = new int[] {255, 255, 0};
                }
            }
        }
        return new Imagen(datos);
    }

    /** Ondas sinusoidales suaves en los tres canales: variaciones continuas de color. */
    public static Imagen crearImagenDetallada(int ancho, int alto) {
        int[][][] datos = new int[alto][ancho][3];
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int r = (int) (128 + 127 * Math.sin(x * 0.2 + y * 0.3));
                int g = (int) (128 + 127 * Math.cos(x * 0.15 - y * 0.25));
                int b = (int) (128 + 127 * Math.sin(x * 0.25 + y * 0.2));
                datos[y][x][0] = clamp(r);
                datos[y][x][1] = clamp(g);
                datos[y][x][2] = clamp(b);
            }
        }
        return new Imagen(datos);
    }

    /**
     * Disco amarillo-naranja sobre un fondo azul con degradado horizontal suave. El disco está descentrado:
     * sus bordes NO coinciden con la partición del Quadtree, y los degradados hacen que la tolerancia importe.
     */
    public static Imagen crearImagenCirculo(int ancho, int alto) {
        int[][][] datos = new int[alto][ancho][3];
        double cx = ancho * 0.37;
        double cy = alto * 0.43;
        double radio = Math.min(ancho, alto) * 0.28;
        int maxX = Math.max(1, ancho - 1);
        int maxY = Math.max(1, alto - 1);
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                double dx = x - cx;
                double dy = y - cy;
                boolean dentro = dx * dx + dy * dy <= radio * radio;
                if (dentro) {
                    datos[y][x][0] = 240;
                    datos[y][x][1] = 140 + (100 * y) / maxY;
                    datos[y][x][2] = 60;
                } else {
                    datos[y][x][0] = 30 + (60 * x) / maxX;
                    datos[y][x][1] = 60 + (60 * x) / maxX;
                    datos[y][x][2] = 120 + (60 * x) / maxX;
                }
            }
        }
        return new Imagen(datos);
    }

    /** Ruido uniforme de +-40 alrededor de 128 en cada canal, con semilla fija (reproducible). */
    public static Imagen crearImagenRuido(int ancho, int alto) {
        Random rnd = new Random(SEMILLA_RUIDO);
        int[][][] datos = new int[alto][ancho][3];
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                for (int c = 0; c < 3; c++) {
                    datos[y][x][c] = 128 + rnd.nextInt(81) - 40;
                }
            }
        }
        return new Imagen(datos);
    }

    public static Imagen crearPorTipo(String tipo, int ancho, int alto) {
        switch (tipo) {
            case "uniforme":
                return crearImagenUniforme(ancho, alto, 80, 120, 180);
            case "regiones":
                return crearImagenConRegiones(ancho, alto);
            case "detallada":
                return crearImagenDetallada(ancho, alto);
            case "circulo":
                return crearImagenCirculo(ancho, alto);
            case "ruido":
                return crearImagenRuido(ancho, alto);
            default:
                throw new IllegalArgumentException("Tipo de imagen desconocido: " + tipo);
        }
    }

    public static Imagen crearPorTipo(String tipo, int tam) {
        return crearPorTipo(tipo, tam, tam);
    }

    private static int clamp(int valor) {
        return Math.max(0, Math.min(255, valor));
    }

    // ---------------------------------------------------------------- Medición

    /**
     * Construye, recorre y reconstruye el Quadtree varias veces y devuelve las métricas.
     * Los tiempos son la mediana de las repeticiones; las demás métricas no dependen de ellas.
     */
    public static Resultado medir(String nombre, Imagen imagen, int tolerancia,
                                  int calentamientos, int repeticiones) {
        if (repeticiones < 1) {
            throw new IllegalArgumentException("Se necesita al menos una repetición medida.");
        }

        for (int i = 0; i < calentamientos; i++) {
            Quadtree q = CompresorQuadtree.construir(imagen, tolerancia);
            q.getRaiz().contarNodos();
            q.reconstruir();
        }

        long[] tConstruccion = new long[repeticiones];
        long[] tRecorrido = new long[repeticiones];
        long[] tReconstruccion = new long[repeticiones];
        Quadtree quadtree = null;
        Imagen reconstruida = null;
        int nodos = 0;
        int hojas = 0;
        int profundidad = 0;

        for (int i = 0; i < repeticiones; i++) {
            long t0 = System.nanoTime();
            quadtree = CompresorQuadtree.construir(imagen, tolerancia);
            tConstruccion[i] = System.nanoTime() - t0;

            t0 = System.nanoTime();
            nodos = quadtree.getRaiz().contarNodos();
            hojas = quadtree.getRaiz().contarHojas();
            profundidad = quadtree.getRaiz().profundidadMaxima();
            tRecorrido[i] = System.nanoTime() - t0;

            t0 = System.nanoTime();
            reconstruida = quadtree.reconstruir();
            tReconstruccion[i] = System.nanoTime() - t0;
        }

        double mse = MetricasImagen.calcularMSE(imagen, reconstruida);
        double psnr = MetricasImagen.calcularPSNR(imagen, reconstruida);
        long bytesOriginal = imagen.getTotalPixeles() * 3L;
        long bytesAlmacenados = CompresorQuadtree.estimarBytesArbol(quadtree.getRaiz());
        double ratio = MetricasImagen.calcularRatioCompresion(bytesOriginal, bytesAlmacenados);

        return new Resultado(nombre, imagen, reconstruida, tolerancia, nodos, hojas, profundidad,
                bytesOriginal, bytesAlmacenados, ratio, mse, psnr,
                medianaMs(tConstruccion), medianaMs(tRecorrido), medianaMs(tReconstruccion));
    }

    private static double medianaMs(long[] nanos) {
        long[] copia = nanos.clone();
        Arrays.sort(copia);
        int n = copia.length;
        double mediana = (n % 2 == 1) ? copia[n / 2] : (copia[n / 2 - 1] + copia[n / 2]) / 2.0;
        return mediana / 1_000_000.0;
    }

    // ---------------------------------------------------------------- Experimentos

    /** Matriz exigida: 3 tamaños x 3 tolerancias (incluye 0 = uniformidad exacta) x 3 tipos = 27 filas. */
    public static List<Resultado> ejecutarMatrizObligatoria() {
        List<Resultado> filas = new ArrayList<>();
        for (int tam : TAMANOS) {
            for (int tol : TOLERANCIAS) {
                for (String tipo : TIPOS_OBLIGATORIOS) {
                    filas.add(medir(tipo, crearPorTipo(tipo, tam), tol, CALENTAMIENTOS, REPETICIONES));
                }
            }
        }
        return filas;
    }

    /** Experimento adicional: dimensiones impares y no cuadradas, con los 5 tipos de imagen. */
    public static List<Resultado> ejecutarMatrizExtra() {
        List<String> tipos = new ArrayList<>(Arrays.asList(TIPOS_OBLIGATORIOS));
        tipos.addAll(Arrays.asList(TIPOS_EXTRA));

        List<Resultado> filas = new ArrayList<>();
        for (int[] d : DIMENSIONES_EXTRA) {
            for (int tol : TOLERANCIAS) {
                for (String tipo : tipos) {
                    filas.add(medir(tipo, crearPorTipo(tipo, d[0], d[1]), tol, CALENTAMIENTOS, REPETICIONES));
                }
            }
        }
        return filas;
    }

    public static void guardarCSV(String rutaSalida, List<Resultado> filas) throws IOException {
        File archivo = new File(rutaSalida);
        File directorio = archivo.getParentFile();
        if (directorio != null && !directorio.exists()) {
            directorio.mkdirs();
        }

        try (Writer writer = new FileWriter(archivo)) {
            writer.write(Resultado.ENCABEZADO_CSV + System.lineSeparator());
            for (Resultado fila : filas) {
                writer.write(fila.aCSV() + System.lineSeparator());
            }
        }
    }
}
