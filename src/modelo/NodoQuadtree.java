package modelo;

/**
 * Nodo del Quadtree. Identifica una región rectangular (x, y, ancho, alto) de la imagen.
 * - Hoja: la región se representa con un único color (color).
 * - Nodo interno: la región se dividió en cuatro cuadrantes (noroeste, noreste, suroeste, sureste).
 *   Un cuadrante puede ser null cuando la región tiene ancho o alto 1 (la mitad vacía no se crea).
 * En los nodos internos, color guarda el promedio de la región (solo informativo; no se almacena).
 */
public class NodoQuadtree {
    private final int x;
    private final int y;
    private final int ancho;
    private final int alto;
    private final boolean hoja;
    private final Pixel color;
    private final NodoQuadtree noroeste;
    private final NodoQuadtree noreste;
    private final NodoQuadtree suroeste;
    private final NodoQuadtree sureste;

    public NodoQuadtree(int x, int y, int ancho, int alto, boolean hoja, Pixel color,
                        NodoQuadtree noroeste, NodoQuadtree noreste,
                        NodoQuadtree suroeste, NodoQuadtree sureste) {
        this.x = x;
        this.y = y;
        this.ancho = ancho;
        this.alto = alto;
        this.hoja = hoja;
        this.color = color;
        this.noroeste = noroeste;
        this.noreste = noreste;
        this.suroeste = suroeste;
        this.sureste = sureste;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getAncho() { return ancho; }
    public int getAlto() { return alto; }
    public boolean isHoja() { return hoja; }
    public Pixel getColor() { return color; }
    public NodoQuadtree getNoroeste() { return noroeste; }
    public NodoQuadtree getNoreste() { return noreste; }
    public NodoQuadtree getSuroeste() { return suroeste; }
    public NodoQuadtree getSureste() { return sureste; }

    /** Total de nodos del subárbol (hojas e internos). Recorrido O(nodos). */
    public int contarNodos() {
        if (hoja) {
            return 1;
        }
        return 1 + contar(noroeste) + contar(noreste) + contar(suroeste) + contar(sureste);
    }

    /** Total de hojas del subárbol. Recorrido O(nodos). */
    public int contarHojas() {
        if (hoja) {
            return 1;
        }
        return hojas(noroeste) + hojas(noreste) + hojas(suroeste) + hojas(sureste);
    }

    /**
     * Profundidad máxima medida en NIVELES: la raíz está en el nivel 1, así que un árbol de un solo
     * nodo tiene profundidad 1. Recorrido O(nodos).
     */
    public int profundidadMaxima() {
        if (hoja) {
            return 1;
        }
        return 1 + Math.max(
                Math.max(prof(noroeste), prof(noreste)),
                Math.max(prof(suroeste), prof(sureste))
        );
    }

    private static int contar(NodoQuadtree n) { return n == null ? 0 : n.contarNodos(); }
    private static int hojas(NodoQuadtree n) { return n == null ? 0 : n.contarHojas(); }
    private static int prof(NodoQuadtree n) { return n == null ? 0 : n.profundidadMaxima(); }
}
