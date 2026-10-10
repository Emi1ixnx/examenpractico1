package experimentos;

import java.util.Locale;

import modelo.Imagen;

/** Métricas de un experimento (una imagen, una tolerancia) y su formato en CSV y en texto. */
public class Resultado {
    public static final String ENCABEZADO_CSV =
            "imagen,ancho,alto,tolerancia,pixeles,nodos,hojas,profundidad,"
            + "bytesOriginal,bytesAlmacenados,razonCompresion,mse,psnr,"
            + "tConstruccionMs,tRecorridoMs,tReconstruccionMs";

    public final String imagen;
    public final int ancho;
    public final int alto;
    public final int tolerancia;
    public final long pixeles;
    public final int nodos;
    public final int hojas;
    public final int profundidad;
    public final long bytesOriginal;
    public final long bytesAlmacenados;
    public final double razonCompresion;
    public final double mse;
    public final double psnr;
    public final double tConstruccionMs;
    /** Tiempo de las tres consultas sobre el árbol: contar nodos, contar hojas y profundidad. */
    public final double tRecorridoMs;
    public final double tReconstruccionMs;

    /** Imágenes del último ciclo medido (para guardarlas como evidencia). No van al CSV. */
    public final Imagen original;
    public final Imagen reconstruida;

    public Resultado(String imagen, Imagen original, Imagen reconstruida, int tolerancia,
                     int nodos, int hojas, int profundidad,
                     long bytesOriginal, long bytesAlmacenados, double razonCompresion,
                     double mse, double psnr,
                     double tConstruccionMs, double tRecorridoMs, double tReconstruccionMs) {
        this.imagen = imagen;
        this.original = original;
        this.reconstruida = reconstruida;
        this.ancho = original.getAncho();
        this.alto = original.getAlto();
        this.pixeles = original.getTotalPixeles();
        this.tolerancia = tolerancia;
        this.nodos = nodos;
        this.hojas = hojas;
        this.profundidad = profundidad;
        this.bytesOriginal = bytesOriginal;
        this.bytesAlmacenados = bytesAlmacenados;
        this.razonCompresion = razonCompresion;
        this.mse = mse;
        this.psnr = psnr;
        this.tConstruccionMs = tConstruccionMs;
        this.tRecorridoMs = tRecorridoMs;
        this.tReconstruccionMs = tReconstruccionMs;
    }

    /** Una fila del CSV. Si el PSNR es infinito (MSE = 0) la celda queda vacía. */
    public String aCSV() {
        String psnrTexto = Double.isInfinite(psnr) ? "" : String.format(Locale.US, "%.4f", psnr);
        return String.format(Locale.US,
                "%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.6f,%.6f,%s,%.4f,%.4f,%.4f",
                imagen, ancho, alto, tolerancia, pixeles, nodos, hojas, profundidad,
                bytesOriginal, bytesAlmacenados, razonCompresion, mse, psnrTexto,
                tConstruccionMs, tRecorridoMs, tReconstruccionMs);
    }

    /** Descripción legible (consola y archivos de evidencia). */
    public String aTexto() {
        String psnrTexto = Double.isInfinite(psnr)
                ? "infinito (reconstrucción exacta)"
                : String.format(Locale.US, "%.4f dB", psnr);
        StringBuilder sb = new StringBuilder();
        sb.append("--- Caso: ").append(imagen).append(" ---\n");
        sb.append("Dimensiones: ").append(ancho).append("x").append(alto)
          .append(" (").append(pixeles).append(" píxeles)\n");
        sb.append("Tolerancia: ").append(tolerancia).append('\n');
        sb.append("Nodos: ").append(nodos).append('\n');
        sb.append("Hojas: ").append(hojas).append('\n');
        sb.append("Profundidad (niveles, raíz = 1): ").append(profundidad).append('\n');
        sb.append("Bytes originales (RGB sin comprimir): ").append(bytesOriginal).append('\n');
        sb.append("Bytes almacenados (formato Quadtree): ").append(bytesAlmacenados).append('\n');
        sb.append(String.format(Locale.US, "Razón de compresión: %.4f%s%n", razonCompresion,
                razonCompresion < 1.0 ? "  (NO comprime: el árbol ocupa más que la imagen)" : ""));
        sb.append(String.format(Locale.US, "MSE: %.6f%n", mse));
        sb.append("PSNR: ").append(psnrTexto).append('\n');
        sb.append(String.format(Locale.US, "Tiempo de construcción (ms, mediana): %.4f%n", tConstruccionMs));
        sb.append(String.format(Locale.US, "Tiempo de recorrido del árbol (ms, mediana): %.4f%n", tRecorridoMs));
        sb.append(String.format(Locale.US, "Tiempo de reconstrucción (ms, mediana): %.4f%n", tReconstruccionMs));
        return sb.toString();
    }
}
