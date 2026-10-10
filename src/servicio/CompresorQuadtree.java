package servicio;

import modelo.Imagen;
import modelo.NodoQuadtree;
import modelo.Pixel;
import modelo.Quadtree;

/**
 * Construcción del Quadtree por Divide y Vencerás y estimación de su tamaño almacenado.
 *
 * Dividir:   si la región no es uniforme, se parte en cuatro cuadrantes. Con mitad = ceil(lado / 2),
 *            el cuadrante izquierdo/superior mide mitad y el derecho/inferior mide lado - mitad, de modo que
 *            la partición cubre todos los píxeles sin huecos ni solapes aunque el lado sea impar.
 * Resolver:  cada cuadrante se procesa recursivamente. Casos base: región vacía (null), región de un
 *            píxel o región uniforme (hoja).
 * Combinar:  los cuatro resultados se enlazan en un nodo interno.
 */
public class CompresorQuadtree {
    /** Encabezado: ancho (32 bits) + alto (32 bits). */
    public static final int BITS_ENCABEZADO = 64;
    /** Un bit por nodo: 1 = hoja, 0 = interno. */
    public static final int BITS_TIPO_NODO = 1;
    /** Color RGB de 24 bits, solo en hojas. */
    public static final int BITS_COLOR = 24;

    public static Quadtree construir(Imagen imagen, int tolerancia) {
        if (imagen == null) {
            throw new IllegalArgumentException("La imagen no puede ser null.");
        }
        if (tolerancia < 0) {
            throw new IllegalArgumentException("La tolerancia no puede ser negativa: " + tolerancia);
        }
        NodoQuadtree raiz = construirRecursivo(imagen, 0, 0, imagen.getAncho(), imagen.getAlto(), tolerancia);
        return new Quadtree(raiz, imagen.getAncho(), imagen.getAlto());
    }

    private static NodoQuadtree construirRecursivo(Imagen imagen, int x, int y, int anchoRegion, int altoRegion, int tolerancia) {
        // Caso base 1: región vacía (ocurre con lados de 1 píxel); no se crea nodo.
        if (anchoRegion <= 0 || altoRegion <= 0) {
            return null;
        }

        Pixel promedio = imagen.promedioRegion(x, y, anchoRegion, altoRegion);

        // Caso base 2: un solo píxel (siempre uniforme) o región uniforme -> hoja.
        boolean unPixel = anchoRegion == 1 && altoRegion == 1;
        if (unPixel || imagen.esUniforme(x, y, anchoRegion, altoRegion, promedio, tolerancia)) {
            return new NodoQuadtree(x, y, anchoRegion, altoRegion, true, promedio, null, null, null, null);
        }

        // Dividir: cada subregión es estrictamente menor (la región tiene al menos 2 píxeles).
        int mitadX = (anchoRegion + 1) / 2;
        int mitadY = (altoRegion + 1) / 2;

        NodoQuadtree noroeste = construirRecursivo(imagen, x, y, mitadX, mitadY, tolerancia);
        NodoQuadtree noreste = construirRecursivo(imagen, x + mitadX, y, anchoRegion - mitadX, mitadY, tolerancia);
        NodoQuadtree suroeste = construirRecursivo(imagen, x, y + mitadY, mitadX, altoRegion - mitadY, tolerancia);
        NodoQuadtree sureste = construirRecursivo(imagen, x + mitadX, y + mitadY, anchoRegion - mitadX, altoRegion - mitadY, tolerancia);

        // Combinar.
        return new NodoQuadtree(x, y, anchoRegion, altoRegion, false, promedio, noroeste, noreste, suroeste, sureste);
    }

    /**
     * Tamaño del formato de almacenamiento, en bits: encabezado + recorrido en preorden con
     * 1 bit por nodo y 24 bits de color por hoja. Las dimensiones de cada subregión no se guardan:
     * el lector las deduce de las del padre con la misma regla de partición, y los cuadrantes vacíos
     * no ocupan bits.
     */
    public static long estimarBitsArbol(NodoQuadtree raiz) {
        return BITS_ENCABEZADO + contarBits(raiz);
    }

    /** Tamaño estimado de la representación, en bytes (bits redondeados hacia arriba). */
    public static long estimarBytesArbol(NodoQuadtree raiz) {
        return (estimarBitsArbol(raiz) + 7) / 8;
    }

    private static long contarBits(NodoQuadtree nodo) {
        if (nodo == null) {
            return 0;
        }
        if (nodo.isHoja()) {
            return BITS_TIPO_NODO + BITS_COLOR;
        }
        return BITS_TIPO_NODO
                + contarBits(nodo.getNoroeste())
                + contarBits(nodo.getNoreste())
                + contarBits(nodo.getSuroeste())
                + contarBits(nodo.getSureste());
    }
}
