package modelo;

/** Quadtree de una imagen: raíz del árbol más las dimensiones originales necesarias para reconstruirla. */
public class Quadtree {
    private final NodoQuadtree raiz;
    private final int ancho;
    private final int alto;

    public Quadtree(NodoQuadtree raiz, int ancho, int alto) {
        this.raiz = raiz;
        this.ancho = ancho;
        this.alto = alto;
    }

    public NodoQuadtree getRaiz() { return raiz; }
    public int getAncho() { return ancho; }
    public int getAlto() { return alto; }

    /**
     * Reconstruye una imagen de las dimensiones originales pintando cada hoja con su color.
     * Costo: O(nodos + píxeles) = O(N), con N = ancho x alto.
     */
    public Imagen reconstruir() {
        Imagen imagen = new Imagen(ancho, alto);
        pintar(raiz, imagen);
        return imagen;
    }

    private void pintar(NodoQuadtree nodo, Imagen imagen) {
        if (nodo == null) {
            return;
        }

        if (nodo.isHoja()) {
            for (int yy = nodo.getY(); yy < nodo.getY() + nodo.getAlto(); yy++) {
                for (int xx = nodo.getX(); xx < nodo.getX() + nodo.getAncho(); xx++) {
                    imagen.setPixel(xx, yy, nodo.getColor());
                }
            }
            return;
        }

        pintar(nodo.getNoroeste(), imagen);
        pintar(nodo.getNoreste(), imagen);
        pintar(nodo.getSuroeste(), imagen);
        pintar(nodo.getSureste(), imagen);
    }
}
