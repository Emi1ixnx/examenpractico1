package servicio;

import modelo.Imagen;

/** Métricas de calidad (MSE, PSNR) y de compresión. */
public class MetricasImagen {

    /**
     * Error Cuadrático Medio sobre los tres canales:
     * MSE = (1 / (3 * N)) * suma de (original - reconstruida)^2, con N = ancho x alto.
     * Las dos imágenes deben tener las mismas dimensiones.
     */
    public static double calcularMSE(Imagen original, Imagen reconstruida) {
        if (original.getAncho() != reconstruida.getAncho() || original.getAlto() != reconstruida.getAlto()) {
            throw new IllegalArgumentException("Las imágenes deben tener las mismas dimensiones: "
                    + original.getAncho() + "x" + original.getAlto() + " vs "
                    + reconstruida.getAncho() + "x" + reconstruida.getAlto());
        }

        int[][][] a = original.getPixeles();
        int[][][] b = reconstruida.getPixeles();
        double suma = 0.0;
        for (int y = 0; y < original.getAlto(); y++) {
            for (int x = 0; x < original.getAncho(); x++) {
                int dr = a[y][x][0] - b[y][x][0];
                int dg = a[y][x][1] - b[y][x][1];
                int db = a[y][x][2] - b[y][x][2];
                suma += (dr * dr) + (dg * dg) + (db * db);
            }
        }
        return suma / (3.0 * original.getTotalPixeles());
    }

    /** PSNR en dB con valor máximo 255. Si MSE = 0 (reconstrucción exacta) devuelve +infinito. */
    public static double calcularPSNR(Imagen original, Imagen reconstruida) {
        double mse = calcularMSE(original, reconstruida);
        if (mse <= 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        double maxValor = 255.0;
        return 10.0 * Math.log10((maxValor * maxValor) / mse);
    }

    /** Razón de compresión = bytes originales / bytes almacenados. Menor que 1 significa que NO comprime. */
    public static double calcularRatioCompresion(long bytesOriginal, long bytesComprimidos) {
        if (bytesComprimidos <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        return (double) bytesOriginal / bytesComprimidos;
    }
}
