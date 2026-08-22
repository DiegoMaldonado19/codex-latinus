lexer grammar LatinLexer;

/* =====================================================================
 *  LEXER DEL LENGUAJE LATIN  (.lat)
 *
 *  El orden de las reglas es la prioridad ante empates de longitud:
 *  literales largos ANTES que los cortos, y palabras reservadas ANTES
 *  que ID (si no, 'si' se leeria como un identificador de dos letras).
 * ===================================================================== */

/* ---------- 1. Marcadores de seccion --------------------------------
 * Van primero porque VARIABILES> y VARIABILES[ empiezan igual que un
 * identificador, y FINIS en mayusculas cierra el programa entero.
 * -------------------------------------------------------------------- */
VARIABILES       : 'VARIABILES>' ;
VARIABILES_LOCAL : 'VARIABILES[' ;   // seccion de variables dentro de una funcion
MUNERA           : 'MUNERA>' ;
MAIOR            : 'MAIOR>' ;
FIN_PROGRAMA     : 'FINIS' ;         // mayusculas: cierra el programa

/* ---------- 2. Palabras reservadas ---------------------------------- */
// Declaracion
ESTO       : 'esto' ;
SERIES     : 'series' ;
STRUCTURA  : 'structura' ;
FINIS      : 'finis' ;               // minusculas: cierra un bloque

// Tipos
NUMERUS    : 'numerus' ;
DECIMALIS  : 'decimalis' ;
TEXTUM     : 'textum' ;
LITTERA    : 'littera' ;
BOOL       : 'bool' ;
VERUM      : 'verum' ;
FALSUS     : 'falsus' ;

// Control de flujo
SI         : 'si' ;
ALITER     : 'aliter' ;
DUM        : 'dum' ;
FACERE     : 'facere' ;
PER        : 'per' ;
PERGE      : 'perge' ;
INTERRUMPE : 'interrumpe' ;

// Funciones
ACTIO      : 'actio' ;
RATIO      : 'ratio' ;
REDDERE    : 'reddere' ;

// Negacion logica
NON        : 'non' ;

/* ---------- 3. Operadores de 2 caracteres (antes que los de 1) ------ */
ENTRADA     : '<<' ;                 // lectura de consola
SALIDA      : '>>' ;                 // impresion en consola
INCREMENTO  : '++' ;
DECREMENTO  : '--' ;
IGUALDAD    : '==' ;
DIFERENTE   : '!=' ;
MENOR_IGUAL : '<=' ;
MAYOR_IGUAL : '>=' ;
AND         : '&&' ;
OR          : '||' ;

/* ---------- 4. Operadores de 1 caracter ----------------------------- */
MAS        : '+' ;
MENOS      : '-' ;
POR        : '*' ;
DIVISION   : '/' ;
MENOR      : '<' ;
MAYOR      : '>' ;
ASIGNACION : '=' ;

/* ---------- 5. Signos de puntuacion --------------------------------- */
PAR_IZQ    : '(' ;
PAR_DER    : ')' ;
LLAVE_IZQ  : '{' ;
LLAVE_DER  : '}' ;
COR_IZQ    : '[' ;
COR_DER    : ']' ;
PUNTO_COMA : ';' ;
DOS_PUNTOS : ':' ;
COMA       : ',' ;
PUNTO      : '.' ;

/* ---------- 6. Literales -------------------------------------------- */
DECIMAL   : DIGITO+ '.' DIGITO+ ;    // antes que ENTERO: 9.81 no es 9 . 81
ENTERO    : DIGITO+ ;
TEXTO     : '"' ( ESCAPE | ~["\\\r\n] )* '"' ;
CARACTER  : '\'' ( ESCAPE | ~['\\\r\n] ) '\'' ;

/* ---------- 7. Identificadores -------------------------------------- */
ID : ( LETRA | '_' ) ( LETRA | DIGITO | '_' )* ;

/* ---------- 8. Ignorados por el parser -------------------------------
 * channel(HIDDEN) en vez de skip: el parser los ignora igual, pero
 * siguen disponibles via lexer.getAllTokens() para que el coloreado de
 * sintaxis pueda pintar los comentarios sin dejar huecos en el texto.
 * -------------------------------------------------------------------- */
COMENTARIO_LINEA  : '//' ~[\r\n]* -> channel(HIDDEN) ;
COMENTARIO_BLOQUE : '/*' .*? '*/' -> channel(HIDDEN) ;
ESPACIOS          : [ \t\r\n\f]+  -> channel(HIDDEN) ;

/* ---------- 9. Recuperacion lexica -----------------------------------
 * Cualquier caracter que no encaje cae aqui para reportarlo como error
 * lexico con linea y columna, en vez de que ANTLR lo descarte callado.
 * -------------------------------------------------------------------- */
CARACTER_INVALIDO : . ;

/* ---------- Fragmentos ---------------------------------------------- */
fragment LETRA  : [a-zA-ZáéíóúÁÉÍÓÚñÑ] ;
fragment DIGITO : [0-9] ;
fragment ESCAPE : '\\' [nrt"'\\] ;
