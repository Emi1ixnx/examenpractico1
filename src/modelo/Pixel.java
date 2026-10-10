package modelo;

/**
 * Color RGB inmutable. Cada canal es un entero entre 0 y 255.
 * Se valida en el constructor para que nunca exista un color fuera de rango.
 */
public class Pixel {
    private final int rojo;
    private final int verde;
    private final int azul;

    public Pixel(int rojo, int verde, int azul) {
        validar(rojo, "rojo");
        validar(verde, "verde");
        validar(azul, "azul");
        this.rojo = rojo;
        this.verde = verde;
        this.azul = azul;
    }

    private static void validar(int valor, String canal) {
        if (valor < 0 || valor > 255) {
            throw new IllegalArgumentException("El canal " + canal + " debe estar entre 0 y 255: " + valor);
        }
    }

    public int getRojo() { return rojo; }
    public int getVerde() { return verde; }
    public int getAzul() { return azul; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pixel)) return false;
        Pixel otro = (Pixel) o;
        return rojo == otro.rojo && verde == otro.verde && azul == otro.azul;
    }

    @Override
    public int hashCode() {
        return (rojo << 16) | (verde << 8) | azul;
    }

    @Override
    public String toString() {
        return "(" + rojo + ", " + verde + ", " + azul + ")";
    }
}
