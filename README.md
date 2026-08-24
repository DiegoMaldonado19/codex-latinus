# Codex Latinus

Compilador del lenguaje **Latin** (`.lat`) con traducción a **PigLatin** (`.pig`).
Práctica 1 — Compiladores II, Universidad de San Carlos de Guatemala, CUNOC.

> _"Los Cerdos Tiranos poseen firewalls biométricos que purgan instantáneamente cualquier dato
> que no esté escrito en su lenguaje nativo: el Pig Latin. Tu compilador es la única llave."_

---

## Qué hace

Análisis **léxico**, **sintáctico** y **semántico** completo de un lenguaje tipado con
estructuras, arreglos, funciones y control de flujo; grafica el AST, la tabla de símbolos y la
pila de procesos; y traduce el programa a PigLatin **recorriendo el AST** — sin `replace` y sin
regex.

| Requisito                                                     | Estado                    |
| ------------------------------------------------------------- | ------------------------- |
| Análisis léxico, sintáctico y semántico con manejo de errores |                           |
| Lenguaje _case sensitive_                                     |                           |
| Gráfica del AST                                               | pestaña AST               |
| Gráfica de la tabla de símbolos                               | pestaña Tabla de símbolos |
| Gráfica de la pila de procesos, con Anterior/Siguiente y log  | pestaña Pila de procesos  |
| Interfaz amigable                                             | JavaFX                    |
| Traducción a PigLatin                                         | pestaña PigLatin          |
| Coloreado del código desde el análisis propio                 | con el `LatinLexer` real  |
| Gestión de archivos: abrir, guardar, descargar                |                           |
| Archivo principal `.lat` / traducido `.pig`                   |                           |
| Documentación técnica                                         | [`docs/`](docs/)          |

---

## Ejecutar

```bash
mvn clean javafx:run
```

Requiere **JDK 21+** y **Maven 3.8+**. La primera ejecución descarga las dependencias y genera el lexer y el parser desde los `.g4`.

```bash
mvn clean compile     # solo compilar
mvn clean package     # generar el .jar
```

Prueba con [`examples/resistencia.lat`](examples/resistencia.lat), que ejercita todas las construcciones del enunciado.

---

## Estructura

```
codex_latinus/
├── docs/                          documentación entregable
│   └── diagramas/                 11 diagramas PlantUML
├── examples/                      10 programas válidos
│   └── errores/                   18 programas con errores, uno por familia
├── src/main/
│   ├── antlr4/.../grammar/        LatinLexer.g4, LatinParser.g4
│   ├── java/com/dmaldonado/codex_latinus/
│   │   ├── model/
│   │   │   ├── ast/               29 nodos del AST + AstVisitor<T>
│   │   │   ├── analysis/          AstBuilderVisitor, SemanticAnalyzer, ReturnPathChecker
│   │   │   ├── symbols/           SymbolTable, Scope, Symbol y sus 4 hijos
│   │   │   ├── types/             DataType, TypeSystem
│   │   │   ├── errors/            ErrorManager, CompilerError, SyntaxErrorListener
│   │   │   ├── stack/             ProcessStackListener, ProcessStep, StepAction
│   │   │   ├── translation/       PigLatinTranslator, PigLatinWordConverter
│   │   │   └── LatinCompiler.java el pipeline completo
│   │   ├── view/                  MainView, HighlightingCodeArea, AstTreeBuilder, ProcessStackPanel
│   │   ├── controller/            ApplicationController, FileController
│   │   ├── util/                  Constants, FileManager
│   │   └── App.java
│   └── resources/                 codex-latinus.css
└── pom.xml
```

**64 clases · 13 paquetes · 2 gramáticas.**

---

## El pipeline

```
.lat → LatinLexer → ¿léxicos? ─┐
                               ├─► LatinParser → ¿sintácticos? ─┐
                               │   + ProcessStackListener       │
                               │                                ├─► AstBuilderVisitor
                               │                                │   → SemanticAnalyzer
                               │                                │   → ¿semánticos? ─┐
                               ▼                                ▼                   ▼
                          CORTA: sin AST                   CORTA: sin AST      CORTA: no grafica
                                                                                ni traduce
                                                                                    │
                                                                                    ▼
                                                            AST + tabla + pila + PigLatin → .pig
```

---

## Decisiones de diseño

| Decisión                                 | Razón corta                                                                                 |
| ---------------------------------------- | ------------------------------------------------------------------------------------------- |
| **Gramática sin recursividad izquierda** | Cada nivel de precedencia es una regla nombrada — el punto de enganche que necesita la pila |
| **Visitor propio, no Listener**          | El valor de retorno es el canal hijo → padre; sin tablas auxiliares                         |
| **AST propio, no el parse tree**         | Sin reglas puente, y con dónde guardar el tipo inferido                                     |
| **La pila es una simulación declarada**  | ANTLR4 es LL(\*); _shift/reduce_ es vocabulario LR.                                         |
| **Coloreado con el lexer real**          | Compara el **tipo de token**, no el texto                                                   |
| **Traducción recorriendo el AST**        | Se demuestra con los paréntesis que el fuente no tenía                                      |
| **Sin `CompilerController`**             | `LatinCompiler` ya devuelve un record agnóstico de la interfaz                              |
| **Errores léxicos con nombre**           | Cadena, carácter y comentario sin cerrar son reglas del lexer, no un símbolo suelto         |
| **Todo se traduce, sin casilla**         | Palabras reservadas y marcadores de sección incluidos                                       |

---

## Autor

Diego Maldonado — Compiladores II · Agosto 2026
