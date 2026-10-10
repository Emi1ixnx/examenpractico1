package pruebas;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import experimentos.GeneradorPruebas;
import modelo.Imagen;
import modelo.NodoQuadtree;
import modelo.Pixel;
import modelo.Quadtree;
import servicio.CompresorQuadtree;
import servicio.MetricasImagen;

/**
 * Pruebas de correctitud del Quadtree (sin librerías externas).
 * Verifica las propiedades que exige el enunciado: partición exacta con dimensiones impares,
 * reconstrucción sin pérdida con tolerancia 0, cota de error con tolerancia > 0, formato de
 * almacenamiento, casos frontera y entradas inválidas.
 * Termina con código 1 si alguna prueba falla.
 */
public class PruebasQuadtree {

    private static final int[][] DIMENSIONES = {
            {1, 1}, {1, 7}, {7, 1}, {2, 3}, {3, 5}, {64, 64}, {100, 75}, {129, 97}, {256, 256}
    };
    private static final String[] TIPOS = {"uniforme", "regiones", "detallada", "circulo", "ruido"};
    private static final int[] TOLERANCIAS = {0, 20, 50};

    private static int total = 0;
    private static int fallidas = 0;

    public static void main(String[] args) {
        particionCubreCadaPixelUnaVez();
        toleranciaCeroEsSinPerdida();
        errorAcotadoPorLaTolerancia();
        profundidadAcotada();
        imagenUniformeUnSoloNodo();
        regionesAlineadasCuatroHojas();
        casoQueNoComprime();
        casosFrontera();
        redondeoDelPromedio();
        estimacionDeBytes();
        metricas();
        entradasInvalidas();
        lecturaYEscrituraDeArchivos();

        System.out.println("RESULTADO FINAL: " + (total - fallidas) + "/" + total + " pruebas exitosas.");
        if (fallidas > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------ Pruebas

    /** Cada píxel pertenece a exactamente una hoja (sin huecos ni solapes), también con lados impares. */
    private static void particionCubreCadaPixelUnaVez() {
        for (int[] d : DIMENSIONES) {
            boolean ok = true;
            String detalle = "";
            for (String tipo : TIPOS) {
                for (int tol : TOLERANCIAS) {
                    Imagen img = GeneradorPruebas.crearPorTipo(tipo, d[0], d[1]);
                    Quadtree qt = CompresorQuadtree.construir(img, tol);
                    String problema = revisarCobertura(qt.getRaiz(), d[0], d[1]);
                    if (problema != null) {
                        ok = false;
                        detalle = tipo + " tol=" + tol + ": " + problema;
                    }
                }
            }
            verificar("Partición exacta (sin huecos ni solapes) en " + d[0] + "x" + d[1]
                    + " [5 tipos x 3 tolerancias]", ok, detalle);
        }
    }

    /** Con tolerancia 0 la reconstrucción es idéntica a la original (MSE = 0). */
    private static void toleranciaCeroEsSinPerdida() {
        for (int[] d : DIMENSIONES) {
            boolean ok = true;
            String detalle = "";
            for (String tipo : TIPOS) {
                Imagen img = GeneradorPruebas.crearPorTipo(tipo, d[0], d[1]);
                Imagen rec = CompresorQuadtree.construir(img, 0).reconstruir();
                if (!rec.iguales(img) || MetricasImagen.calcularMSE(img, rec) != 0.0) {
                    ok = false;
                    detalle = "difiere en " + tipo;
                }
            }
            verificar("Tolerancia 0 reconstruye sin pérdida en " + d[0] + "x" + d[1], ok, detalle);
        }
    }

    /** Con tolerancia t, ningún canal de ningún píxel se aleja más de t, y por tanto MSE <= t^2. */
    private static void errorAcotadoPorLaTolerancia() {
        for (int[] d : DIMENSIONES) {
            boolean ok = true;
            String detalle = "";
            for (String tipo : TIPOS) {
                for (int tol : new int[] {20, 50}) {
                    Imagen img = GeneradorPruebas.crearPorTipo(tipo, d[0], d[1]);
                    Imagen rec = CompresorQuadtree.construir(img, tol).reconstruir();
                    int maximo = errorMaximoPorCanal(img, rec);
                    double mse = MetricasImagen.calcularMSE(img, rec);
                    if (maximo > tol || mse > (double) tol * tol) {
                        ok = false;
                        detalle = tipo + " tol=" + tol + ": error máximo " + maximo + ", MSE " + mse;
                    }
                }
            }
            verificar("Error por canal <= tolerancia y MSE <= tolerancia^2 en " + d[0] + "x" + d[1], ok, detalle);
        }
    }

    /** La profundidad nunca supera el número de mitades sucesivas hasta llegar a 1 píxel, más la raíz. */
    private static void profundidadAcotada() {
        boolean ok = true;
        String detalle = "";
        for (int[] d : DIMENSIONES) {
            int limite = niveles(Math.max(d[0], d[1]));
            for (String tipo : TIPOS) {
                for (int tol : TOLERANCIAS) {
                    Imagen img = GeneradorPruebas.crearPorTipo(tipo, d[0], d[1]);
                    NodoQuadtree raiz = CompresorQuadtree.construir(img, tol).getRaiz();
                    if (raiz.profundidadMaxima() > limite || raiz.contarNodos() < raiz.contarHojas()) {
                        ok = false;
                        detalle = d[0] + "x" + d[1] + " " + tipo + " tol=" + tol;
                    }
                }
            }
        }
        verificar("Profundidad <= ceil(log2(lado mayor)) + 1 en todas las dimensiones", ok, detalle);
    }

    private static void imagenUniformeUnSoloNodo() {
        for (int[] d : new int[][] {{64, 64}, {100, 75}}) {
            Imagen img = GeneradorPruebas.crearPorTipo("uniforme", d[0], d[1]);
            Quadtree qt = CompresorQuadtree.construir(img, 0);
            NodoQuadtree raiz = qt.getRaiz();
            long bytes = CompresorQuadtree.estimarBytesArbol(raiz);
            boolean ok = raiz.contarNodos() == 1 && raiz.contarHojas() == 1 && raiz.profundidadMaxima() == 1
                    && raiz.getAncho() == d[0] && raiz.getAlto() == d[1]
                    && bytes == 12   // (64 encabezado + 1 + 24) bits = 89 bits -> 12 bytes
                    && qt.reconstruir().iguales(img);
            verificar("Imagen uniforme " + d[0] + "x" + d[1] + " -> 1 nodo, 1 hoja, 12 bytes",
                    ok, "nodos=" + raiz.contarNodos() + ", bytes=" + bytes);
        }
    }

    /** Cuatro cuadrantes planos alineados con la partición: 5 nodos (1 raíz + 4 hojas), profundidad 2. */
    private static void regionesAlineadasCuatroHojas() {
        for (int tam : new int[] {64, 128, 256}) {
            Imagen img = GeneradorPruebas.crearPorTipo("regiones", tam);
            Quadtree qt = CompresorQuadtree.construir(img, 0);
            NodoQuadtree raiz = qt.getRaiz();
            boolean ok = raiz.contarNodos() == 5 && raiz.contarHojas() == 4 && raiz.profundidadMaxima() == 2
                    && qt.reconstruir().iguales(img);
            verificar("Imagen de 4 regiones " + tam + "x" + tam + " -> 5 nodos, 4 hojas, profundidad 2",
                    ok, "nodos=" + raiz.contarNodos() + ", hojas=" + raiz.contarHojas());
        }
    }

    /**
     * Caso en que el Quadtree NO reduce el almacenamiento: con ruido y uniformidad exacta cada píxel
     * es una hoja de 25 bits (1 + 24) frente a los 24 bits crudos, más los nodos internos.
     */
    private static void casoQueNoComprime() {
        Imagen img = GeneradorPruebas.crearPorTipo("ruido", 256);
        NodoQuadtree raiz = CompresorQuadtree.construir(img, 0).getRaiz();
        long bytesOriginal = img.getTotalPixeles() * 3L;
        long bytes = CompresorQuadtree.estimarBytesArbol(raiz);
        double ratio = MetricasImagen.calcularRatioCompresion(bytesOriginal, bytes);
        boolean ok = raiz.contarHojas() == 256 * 256 && ratio < 1.0 && ratio > 0.90;
        verificar("Ruido 256x256 con tolerancia 0 NO comprime (razón < 1)", ok,
                "hojas=" + raiz.contarHojas() + ", razón=" + ratio);
    }

    private static void casosFrontera() {
        // 1x1: una sola hoja con el color exacto.
        Imagen uno = new Imagen(new int[][][] {{{200, 100, 50}}});
        Quadtree q1 = CompresorQuadtree.construir(uno, 0);
        verificar("Imagen de 1x1 -> una hoja con el color exacto",
                q1.getRaiz().isHoja() && q1.getRaiz().contarNodos() == 1
                        && q1.getRaiz().getColor().equals(new Pixel(200, 100, 50)), "");

        // 2x1 con colores distintos: raíz con 2 hijos reales y 2 cuadrantes vacíos (null).
        Imagen dos = new Imagen(new int[][][] {{{0, 0, 0}, {255, 255, 255}}});
        NodoQuadtree r2 = CompresorQuadtree.construir(dos, 0).getRaiz();
        verificar("Imagen de 2x1 distinta -> 3 nodos, 2 hojas, 2 cuadrantes vacíos",
                !r2.isHoja() && r2.contarNodos() == 3 && r2.contarHojas() == 2 && r2.profundidadMaxima() == 2
                        && r2.getSuroeste() == null && r2.getSureste() == null, "");
    }

    /** El promedio se redondea al entero más cercano (10 y 11 -> 11) y respeta la tolerancia. */
    private static void redondeoDelPromedio() {
        Imagen img = new Imagen(new int[][][] {{{10, 0, 0}, {11, 0, 0}}});
        NodoQuadtree conTol1 = CompresorQuadtree.construir(img, 1).getRaiz();
        NodoQuadtree conTol0 = CompresorQuadtree.construir(img, 0).getRaiz();
        verificar("Promedio redondeado: tolerancia 1 -> hoja con rojo 11; tolerancia 0 -> se divide",
                conTol1.isHoja() && conTol1.getColor().getRojo() == 11 && !conTol0.isHoja(), "");
    }

    private static void estimacionDeBytes() {
        // Uniforme: 64 + 25 = 89 bits -> 12 bytes. Cuatro regiones: 64 + 1 + 4*25 = 165 bits -> 21 bytes.
        NodoQuadtree uniforme = CompresorQuadtree.construir(GeneradorPruebas.crearPorTipo("uniforme", 64), 0).getRaiz();
        NodoQuadtree regiones = CompresorQuadtree.construir(GeneradorPruebas.crearPorTipo("regiones", 64), 0).getRaiz();
        verificar("Formato de almacenamiento: bits y bytes esperados",
                CompresorQuadtree.estimarBitsArbol(uniforme) == 89 && CompresorQuadtree.estimarBytesArbol(uniforme) == 12
                        && CompresorQuadtree.estimarBitsArbol(regiones) == 165 && CompresorQuadtree.estimarBytesArbol(regiones) == 21,
                "uniforme=" + CompresorQuadtree.estimarBitsArbol(uniforme)
                        + " bits, regiones=" + CompresorQuadtree.estimarBitsArbol(regiones) + " bits");
    }

    private static void metricas() {
        Imagen negro = new Imagen(new int[][][] {{{0, 0, 0}}});
        Imagen gris = new Imagen(new int[][][] {{{3, 3, 3}}});
        double mse = MetricasImagen.calcularMSE(negro, gris);
        double psnr = MetricasImagen.calcularPSNR(negro, gris);
        double psnrEsperado = 10.0 * Math.log10(255.0 * 255.0 / 9.0);
        verificar("MSE de (0,0,0) vs (3,3,3) = 9 y PSNR correcto",
                mse == 9.0 && Math.abs(psnr - psnrEsperado) < 1e-9, "mse=" + mse + ", psnr=" + psnr);
        verificar("PSNR infinito si las imágenes son idénticas",
                Double.isInfinite(MetricasImagen.calcularPSNR(negro, negro)), "");
        verificar("Razón de compresión = bytes originales / almacenados",
                MetricasImagen.calcularRatioCompresion(100, 10) == 10.0, "");
        verificar("MSE con dimensiones distintas lanza IllegalArgumentException",
                lanza(() -> MetricasImagen.calcularMSE(negro, new Imagen(2, 1))), "");
    }

    private static void entradasInvalidas() {
        Imagen img = GeneradorPruebas.crearPorTipo("uniforme", 4);
        verificar("Tolerancia negativa lanza IllegalArgumentException",
                lanza(() -> CompresorQuadtree.construir(img, -1)), "");
        verificar("Imagen null lanza IllegalArgumentException",
                lanza(() -> CompresorQuadtree.construir(null, 0)), "");
        verificar("Matriz vacía lanza IllegalArgumentException",
                lanza(() -> new Imagen(new int[0][0][0])), "");
        verificar("Dimensiones no positivas lanzan IllegalArgumentException",
                lanza(() -> new Imagen(0, 5)), "");
        verificar("Matriz no rectangular lanza IllegalArgumentException",
                lanza(() -> new Imagen(new int[][][] {{{0, 0, 0}, {0, 0, 0}}, {{0, 0, 0}}})), "");
        verificar("Canal fuera de [0, 255] lanza IllegalArgumentException",
                lanza(() -> new Pixel(256, 0, 0)), "");
        verificar("Región fuera de la imagen lanza IllegalArgumentException (no se ignora en silencio)",
                lanza(() -> img.promedioRegion(2, 2, 3, 3)), "");
        verificar("Tipo de imagen desconocido lanza IllegalArgumentException",
                lanza(() -> GeneradorPruebas.crearPorTipo("inexistente", 4)), "");
    }

    private static void lecturaYEscrituraDeArchivos() {
        boolean ok;
        String detalle = "";
        File tmp = null;
        try {
            Imagen img = GeneradorPruebas.crearPorTipo("circulo", 33, 21);
            tmp = Files.createTempFile("quadtree", ".png").toFile();
            img.guardar(tmp.getPath());
            Imagen leida = Imagen.desdeArchivo(tmp.getPath());
            ok = img.iguales(leida) && leida.getTotalPixeles() == 33L * 21L;
        } catch (IOException e) {
            ok = false;
            detalle = e.getMessage();
        } finally {
            if (tmp != null) {
                tmp.delete();
            }
        }
        verificar("Guardar y volver a leer un PNG conserva todos los píxeles", ok, detalle);
    }

    // ------------------------------------------------------------ Apoyo

    /** Devuelve null si cada píxel está cubierto por exactamente una hoja; si no, describe el problema. */
    private static String revisarCobertura(NodoQuadtree raiz, int ancho, int alto) {
        int[][] cuenta = new int[alto][ancho];
        try {
            marcar(raiz, cuenta);
        } catch (ArrayIndexOutOfBoundsException e) {
            return "una hoja se sale de la imagen";
        }
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                if (cuenta[y][x] != 1) {
                    return "el píxel (" + x + ", " + y + ") está cubierto " + cuenta[y][x] + " veces";
                }
            }
        }
        return null;
    }

    private static void marcar(NodoQuadtree nodo, int[][] cuenta) {
        if (nodo == null) {
            return;
        }
        if (nodo.isHoja()) {
            for (int y = nodo.getY(); y < nodo.getY() + nodo.getAlto(); y++) {
                for (int x = nodo.getX(); x < nodo.getX() + nodo.getAncho(); x++) {
                    cuenta[y][x]++;
                }
            }
            return;
        }
        marcar(nodo.getNoroeste(), cuenta);
        marcar(nodo.getNoreste(), cuenta);
        marcar(nodo.getSuroeste(), cuenta);
        marcar(nodo.getSureste(), cuenta);
    }

    private static int errorMaximoPorCanal(Imagen a, Imagen b) {
        int maximo = 0;
        int[][][] pa = a.getPixeles();
        int[][][] pb = b.getPixeles();
        for (int y = 0; y < a.getAlto(); y++) {
            for (int x = 0; x < a.getAncho(); x++) {
                for (int c = 0; c < 3; c++) {
                    maximo = Math.max(maximo, Math.abs(pa[y][x][c] - pb[y][x][c]));
                }
            }
        }
        return maximo;
    }

    /** Niveles de un árbol que parte un lado de longitud n por la mitad (hacia arriba) hasta llegar a 1. */
    private static int niveles(int n) {
        int niveles = 1;
        while (n > 1) {
            n = (n + 1) / 2;
            niveles++;
        }
        return niveles;
    }

    private static boolean lanza(Runnable accion) {
        try {
            accion.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static void verificar(String nombre, boolean ok, String detalleSiFalla) {
        total++;
        if (!ok) {
            fallidas++;
        }
        System.out.println("Prueba " + total + ": " + nombre + " -> " + (ok ? "OK" : "FALLÓ"));
        if (!ok && detalleSiFalla != null && !detalleSiFalla.isEmpty()) {
            System.out.println("    Detalle: " + detalleSiFalla);
        }
    }
}
