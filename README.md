# Compresión de imágenes con Quadtree (Divide y Vencerás)

Prototipo en Java que comprime una imagen dividiéndola recursivamente en cuatro cuadrantes hasta que cada
región es uniforme (según una tolerancia), la reconstruye a partir del árbol y mide tiempo, tamaño y error.
El Quadtree está implementado desde cero: no se usa ninguna biblioteca de compresión.

## Integrantes

- Nombre completo del integrante 1
- Nombre completo del integrante 2
- Nombre completo del integrante 3

## Requisitos y dependencias

- **JDK 8 o superior** (probado con OpenJDK 21).
- Sin dependencias externas. Solo se usan clases estándar de Java: `javax.imageio` y `java.awt.image`
  (únicamente para leer y guardar archivos de imagen) y `System.nanoTime` (para medir tiempos).

## Estructura

| Archivo | Responsabilidad |
|---|---|
| `src/modelo/Pixel.java` | Color RGB inmutable (canales 0-255). |
| `src/modelo/Imagen.java` | Imagen como matriz `[alto][ancho][3]`; promedio de región y criterio de uniformidad. |
| `src/modelo/NodoQuadtree.java` | Nodo: región (x, y, ancho, alto), color y 4 hijos; conteo de nodos, hojas y profundidad. |
| `src/modelo/Quadtree.java` | Árbol + dimensiones originales; reconstrucción de la imagen. |
| `src/servicio/CompresorQuadtree.java` | Construcción recursiva (Divide y Vencerás) y formato de almacenamiento. |
| `src/servicio/MetricasImagen.java` | MSE, PSNR y razón de compresión. |
| `src/experimentos/GeneradorPruebas.java` | Imágenes de prueba reproducibles, medición y escritura de CSV. |
| `src/experimentos/Resultado.java` | Métricas de un experimento (fila de CSV y texto). |
| `src/aplicacion/Main.java` | Ejecuta evidencia y experimentos. |
| `src/pruebas/PruebasQuadtree.java` | Pruebas de correctitud (51 comprobaciones). |

## Compilación

Linux / macOS:

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
```

Windows (CMD):

```bat
mkdir out
dir /s /b src\*.java > fuentes.txt
javac -encoding UTF-8 -d out @fuentes.txt
```

## Ejecución

Experimentos y evidencia (desde la carpeta del proyecto):

```bash
java -Dfile.encoding=UTF-8 -cp out aplicacion.Main
```

Pruebas de correctitud (terminan con código 1 si alguna falla):

```bash
java -Dfile.encoding=UTF-8 -cp out pruebas.PruebasQuadtree
```

> En Windows, si los acentos se ven mal, ejecutar antes `chcp 65001`.

Si se colocan fotos (`.png`, `.jpg`, `.jpeg`, `.bmp`, `.gif`) en la carpeta `imagenes/`, `Main` también las comprime
con tolerancias 0, 20 y 50. Un archivo que no se pueda leer se omite con un aviso.

### Salidas (carpeta `resultados/`)

| Archivo | Contenido |
|---|---|
| `experimentos.csv` | Matriz obligatoria: 3 tamaños (64, 128, 256) x 3 tolerancias (0, 20, 50) x 3 tipos = 27 filas. |
| `experimentos_extra.csv` | Dimensiones impares y no cuadradas (100x75, 129x97) con 5 tipos de imagen. |
| `<caso>_original.png`, `<caso>_reconstruida.png`, `<caso>_metricas.txt` | Evidencia: imagen original, reconstruida y métricas con su configuración. |

Columnas del CSV: `imagen, ancho, alto, tolerancia, pixeles, nodos, hojas, profundidad, bytesOriginal,
bytesAlmacenados, razonCompresion, mse, psnr, tConstruccionMs, tRecorridoMs, tReconstruccionMs`.
El `psnr` queda vacío cuando MSE = 0 (reconstrucción exacta, PSNR infinito).

Los tiempos son la **mediana de 7 ejecuciones** tras 3 de calentamiento de la JVM. `tRecorridoMs` es el tiempo de
las tres consultas sobre el árbol (contar nodos, contar hojas y calcular la profundidad). Los tiempos dependen de la
máquina: conviene ejecutar `Main` en el equipo con el que se redactará el reporte.

Tipos de imagen: `uniforme` (un solo color), `regiones` (cuatro cuadrantes planos alineados con la partición),
`detallada` (ondas sinusoidales suaves), `circulo` (disco descentrado sobre degradado) y `ruido` (+-40 alrededor de
128, semilla fija). Todas se generan por código, así que los experimentos son reproducibles sin archivos externos.

## Decisiones de diseño

**Representación.** Imagen RGB como `int[alto][ancho][3]`, canales en 0-255. La imagen se lee con `ImageIO`; la
transparencia se ignora.

**Color representativo.** Promedio de la región por canal, redondeado al entero más cercano.

**Criterio de uniformidad.** Una región con color representativo `c` es uniforme si para todo píxel `p` de la región

```
max(|p.R - c.R|, |p.G - c.G|, |p.B - c.B|) <= tolerancia
```

Con `tolerancia = 0` exige que todos los píxeles sean idénticos, así que la reconstrucción es exacta. Con
`tolerancia = t`, ningún canal de ningún píxel se aleja más de `t` del valor reconstruido, por lo que `MSE <= t^2`.

**Dimensiones no potencia de dos.** Para un lado de longitud `L` se usa `mitad = ceil(L / 2)`: el cuadrante
izquierdo/superior mide `mitad` y el derecho/inferior mide `L - mitad`. Así la partición cubre todos los píxeles
sin huecos ni solapes. Si un lado mide 1, la mitad vacía no genera nodo (`null`), de modo que no se subdividen regiones
vacías. Cada subregión es estrictamente menor que su padre, por lo que la recursión siempre llega a un caso base.

**Casos base.** Región vacía (`null`), región de un píxel, o región uniforme (hoja).

**Profundidad.** Se mide en niveles: la raíz es el nivel 1, así que un árbol de un solo nodo tiene profundidad 1.

**Formato de almacenamiento.** Encabezado de 64 bits (ancho y alto, 32 bits cada uno) más un recorrido en preorden
con 1 bit por nodo (1 = hoja, 0 = interno) y 24 bits de color por hoja. Las dimensiones de cada subregión no se
guardan: el lector las deduce de las del padre con la misma regla de partición, y los cuadrantes vacíos no ocupan bits.
Los bytes se obtienen redondeando los bits hacia arriba. La razón de compresión es
`bytesOriginal / bytesAlmacenados`, con `bytesOriginal = ancho x alto x 3`.

**Cuándo no comprime.** Cada hoja cuesta 25 bits frente a los 24 de un píxel crudo, más 1 bit por nodo interno. Si
casi todos los píxeles son distintos y la tolerancia es 0, la razón queda por debajo de 1 (cerca de 0.95): el árbol
ocupa más que la imagen. Estos casos aparecen en los CSV y deben reportarse.

## Complejidad

Sea `N = ancho x alto` el número de píxeles. En cada nodo se calcula el promedio y se verifica la uniformidad, y ambas
operaciones recorren toda la región (costo `Theta(area)`). Con regiones que se reparten en cuatro partes de tamaño
`N/4`, la recurrencia es `T(N) = 4 T(N/4) + Theta(N)`.

| Operación | Mejor caso | Peor caso |
|---|---|---|
| Construcción | `Theta(N)` (imagen uniforme: una sola región) | `Theta(N log N)` (cada nivel suma `N` y hay `Theta(log N)` niveles) |
| Recorrido (contar nodos, hojas, profundidad) | `Theta(nodos)` | `Theta(nodos)`, con `nodos <= ~4N/3` |
| Reconstrucción | `Theta(N)` | `Theta(N)` (se pinta cada píxel una vez) |

**Espacio.** `O(nodos)` para el árbol (hasta unos `4N/3` nodos en el peor caso) más `O(log N)` de pila de recursión.

## Uso de herramientas de IA

Según la sección 6.4 del enunciado, el uso de LLMs debe documentarse. Completar con lo que el equipo realmente hizo:

- Se usó Claude (Anthropic) para revisar el código del Quadtree contra el enunciado y proponer correcciones
  (redondeo del promedio, validaciones, pruebas de correctitud, experimento adicional, este README).
- El equipo revisó y comprendió el código, y debe poder explicar cada decisión algorítmica.
- _Agregar aquí qué partes escribió o decidió cada integrante._

## Referencias

_Agregar las fuentes consultadas (apuntes de la materia, libros, artículos sobre Quadtrees, etc.)._
