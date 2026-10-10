package modelo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Imagen RGB representada como matriz de píxeles: pixeles[y][x][canal],
 * con canal 0 = rojo, 1 = verde, 2 = azul y valores enteros en [0, 255].
 * y es la fila (0 = arriba) y x es la columna (0 = izquierda).
 */
public class Imagen {
    private final int ancho;
    private final int alto;
    private final int[][][] pixeles;

    /** Imagen negra de ancho x alto. */
    public Imagen(int ancho, int alto) {
        if (ancho <= 0 || alto <= 0) {
            throw new IllegalArgumentException("Las dimensiones deben ser positivas: " + ancho + "x" + alto);
        }
        this.ancho = ancho;
        this.alto = alto;
        this.pixeles = new int[alto][ancho][3];
    }

    /** Envuelve una matriz [alto][ancho][3] (no la copia). Debe ser rectangular y con canales en [0, 255]. */
    public Imagen(int[][][] pixeles) {
        if (pixeles == null || pixeles.length == 0 || pixeles[0] == null || pixeles[0].length == 0) {
            throw new IllegalArgumentException("La imagen debe tener al menos un píxel.");
        }
        int anchoEsperado = pixeles[0].length;
        for (int y = 0; y < pixeles.length; y++) {
            if (pixeles[y] == null || pixeles[y].length != anchoEsperado) {
                throw new IllegalArgumentException("La matriz de píxeles debe ser rectangular (fila " + y + ").");
            }
            for (int x = 0; x < anchoEsperado; x++) {
                int[] p = pixeles[y][x];
                if (p == null || p.length != 3) {
                    throw new IllegalArgumentException("Cada píxel debe tener 3 canales (" + x + ", " + y + ").");
                }
                for (int c = 0; c < 3; c++) {
                    if (p[c] < 0 || p[c] > 255) {
                        throw new IllegalArgumentException("Canal fuera de [0, 255] en (" + x + ", " + y + ").");
                    }
                }
            }
        }
        this.pixeles = pixeles;
        this.alto = pixeles.length;
        this.ancho = anchoEsperado;
    }

    /** Lee una imagen del disco. La transparencia (canal alfa) se ignora: solo se usan R, G y B. */
    public static Imagen desdeArchivo(String ruta) throws IOException {
        BufferedImage image = ImageIO.read(new File(ruta));
        if (image == null) {
            throw new IOException("No se pudo leer la imagen: " + ruta);
        }

        int[][][] datos = new int[image.getHeight()][image.getWidth()][3];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                datos[y][x][0] = (rgb >> 16) & 0xFF;
                datos[y][x][1] = (rgb >> 8) & 0xFF;
                datos[y][x][2] = rgb & 0xFF;
            }
        }
        return new Imagen(datos);
    }

    /** Guarda la imagen como PNG. */
    public void guardar(String ruta) throws IOException {
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int r = pixeles[y][x][0];
                int g = pixeles[y][x][1];
                int b = pixeles[y][x][2];
                img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        ImageIO.write(img, "png", new File(ruta));
    }

    public int getAncho() { return ancho; }
    public int getAlto() { return alto; }

    /** Cantidad total de píxeles (ancho x alto), calculada en long para evitar desbordamiento. */
    public long getTotalPixeles() { return (long) ancho * alto; }

    /** Acceso directo a la matriz interna (solo lectura por convención). */
    public int[][][] getPixeles() { return pixeles; }

    public Pixel getPixel(int x, int y) {
        if (x < 0 || x >= ancho || y < 0 || y >= alto) {
            throw new IllegalArgumentException("Coordenadas fuera de rango: (" + x + ", " + y + ")");
        }
        return new Pixel(pixeles[y][x][0], pixeles[y][x][1], pixeles[y][x][2]);
    }

    public void setPixel(int x, int y, Pixel pixel) {
        if (x < 0 || x >= ancho || y < 0 || y >= alto) {
            throw new IllegalArgumentException("Coordenadas fuera de rango: (" + x + ", " + y + ")");
        }
        pixeles[y][x][0] = pixel.getRojo();
        pixeles[y][x][1] = pixel.getVerde();
        pixeles[y][x][2] = pixel.getAzul();
    }

    /** true si ambas imágenes tienen las mismas dimensiones y exactamente los mismos píxeles. */
    public boolean iguales(Imagen otra) {
        if (otra == null || ancho != otra.ancho || alto != otra.alto) {
            return false;
        }
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                for (int c = 0; c < 3; c++) {
                    if (pixeles[y][x][c] != otra.pixeles[y][x][c]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Color promedio de la región [x, x+anchoRegion) x [y, y+altoRegion), redondeado al entero
     * más cercano por canal. Costo: O(anchoRegion * altoRegion).
     * Lanza IllegalArgumentException si la región no está completamente dentro de la imagen.
     */
    public Pixel promedioRegion(int x, int y, int anchoRegion, int altoRegion) {
        validarRegion(x, y, anchoRegion, altoRegion);
        long r = 0;
        long g = 0;
        long b = 0;
        for (int yy = y; yy < y + altoRegion; yy++) {
            for (int xx = x; xx < x + anchoRegion; xx++) {
                int[] p = pixeles[yy][xx];
                r += p[0];
                g += p[1];
                b += p[2];
            }
        }
        long total = (long) anchoRegion * altoRegion;
        return new Pixel((int) ((r + total / 2) / total),
                         (int) ((g + total / 2) / total),
                         (int) ((b + total / 2) / total));
    }

    /**
     * Criterio de uniformidad: la región es uniforme si, para TODO píxel p de la región,
     * max(|p.R - c.R|, |p.G - c.G|, |p.B - c.B|) <= tolerancia, donde c es el color representativo
     * (el promedio de la región). Con tolerancia 0 exige que todos los píxeles sean idénticos.
     * Termina en cuanto encuentra un píxel que incumple. Costo: O(anchoRegion * altoRegion).
     */
    public boolean esUniforme(int x, int y, int anchoRegion, int altoRegion, Pixel color, int tolerancia) {
        validarRegion(x, y, anchoRegion, altoRegion);
        int cr = color.getRojo();
        int cg = color.getVerde();
        int cb = color.getAzul();
        for (int yy = y; yy < y + altoRegion; yy++) {
            for (int xx = x; xx < x + anchoRegion; xx++) {
                int[] p = pixeles[yy][xx];
                if (Math.abs(p[0] - cr) > tolerancia
                        || Math.abs(p[1] - cg) > tolerancia
                        || Math.abs(p[2] - cb) > tolerancia) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Falla de forma explícita si la región se sale de la imagen, en lugar de ignorar píxeles en silencio. */
    private void validarRegion(int x, int y, int anchoRegion, int altoRegion) {
        if (x < 0 || y < 0 || anchoRegion <= 0 || altoRegion <= 0
                || x + anchoRegion > ancho || y + altoRegion > alto) {
            throw new IllegalArgumentException("Región fuera de la imagen: x=" + x + ", y=" + y
                    + ", " + anchoRegion + "x" + altoRegion + " en imagen " + ancho + "x" + alto);
        }
    }
}
