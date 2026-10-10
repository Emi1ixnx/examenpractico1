# Compresión de imágenes con Quadtree (Divide y Vencerás)

Prototipo en Java que comprime una imagen dividiéndola recursivamente en cuatro cuadrantes hasta que cada
región es uniforme (según una tolerancia), la reconstruye a partir del árbol y mide tiempo, tamaño y error.
El Quadtree está implementado desde cero: no se usa ninguna biblioteca de compresión.

## Integrantes

- Aguilar Avendaño Emiliano
- Sánchez Rojas Diego

## Requisitos y dependencias

- **JDK 8 o superior** (probado con OpenJDK 21).
- Sin dependencias externas. Solo se usan clases estándar de Java: `javax.imageio` y `java.awt.image`
  (únicamente para leer y guardar archivos de imagen) y `System.nanoTime` (para medir tiempos).

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

## Uso de herramientas de IA

- Se usó Claude (Anthropic) para revisar el código del Quadtree contra el enunciado y proponer correcciones
  (redondeo del promedio, validaciones, pruebas de correctitud, experimento adicional, este README).
- El equipo revisó y comprendió el código, y debe poder explicar cada decisión algorítmica.