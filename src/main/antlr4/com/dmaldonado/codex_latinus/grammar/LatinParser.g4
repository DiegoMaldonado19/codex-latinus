parser grammar LatinParser;

options { tokenVocab = LatinLexer; }

/* =====================================================================
 *  PARSER DEL LENGUAJE LATIN
 *
 *  >>> GRAMATICA LIBRE DE RECURSIVIDAD POR LA IZQUIERDA <<<
 *
 *  Toda la cadena de precedencia de expresiones usa el patron iterativo
 *  A : B (op B)* , que es el resultado de aplicar la eliminacion clasica
 *  de recursividad izquierda:
 *
 *      E -> E + T | T                    (recursiva por la izquierda)
 *      ---------------------------------------------------------------
 *      E  -> T E'
 *      E' -> + T E' | epsilon   ==   T ( '+' T )*
 *
 *  Se escribe asi a proposito, aunque ANTLR4 soporte recursividad
 *  izquierda: cada nivel de precedencia queda como una regla nombrada
 *  distinta, que es el punto de enganche que necesita el listener de la
 *  pila de procesos para simular shift/reduce.
 *
 *  Precedencia (de menor a mayor):
 *      ||  ->  &&  ->  == !=  ->  < > <= >=  ->  + -  ->  * /  ->
 *      non, - unario, ++/-- prefijos  ->  sufijos ([] . ())  ->  primaria
 * ===================================================================== */

/* =====================================================================
 * 1. ESTRUCTURA GLOBAL DEL PROGRAMA
 * ===================================================================== */
programa
    : seccionVariables? seccionFunciones? seccionPrincipal
      FIN_PROGRAMA PUNTO_COMA EOF
    ;

seccionVariables : VARIABILES declaracionGlobal* ;
seccionFunciones : MUNERA declaracionFuncion* ;
seccionPrincipal : MAIOR instruccion* ;

declaracionGlobal
    : declaracionVariable
    | declaracionArreglo
    | declaracionEstructura
    ;

/* =====================================================================
 * 2. DECLARACIONES
 * ===================================================================== */

// esto edad : numerus 20;
// esto mi_selva : Selva { valido: verum, animales: Animal[7] }
//
// El ';' final es opcional cuando el valor inicial es un literal de
// estructura, porque el auxiliar confirmo que esas declaraciones
// "terminan con los }". El '=' tambien es opcional: el enunciado lo usa
// una sola vez ("esto resultado : numerus = calcularPoder(10, 0.5);").
//
// Las dos alternativas son viables para "esto x : Persona {...};", y
// ANTLR resuelve el empate por orden de declaracion: gana la primera, asi
// que ese caso siempre sale como declaracionVariableEstructura.
declaracionVariable
    : ESTO ID DOS_PUNTOS tipo ASIGNACION? literalCompuesto PUNTO_COMA?  # declaracionVariableEstructura
    | ESTO ID DOS_PUNTOS tipo ( ASIGNACION? expresion )? PUNTO_COMA     # declaracionVariableSimple
    ;

// series mis_enteros[2] : numerus {1, 1};
//
// El tipo es opcional por la forma vieja del arreglo booleano que dio el
// auxiliar antes de que existiera 'bool' (Telegram 6/08 22:08):
//     series valores[2] : {verum, verum};
// Cuando falta, el AstBuilderVisitor infiere el tipo del primer valor.
declaracionArreglo
    : SERIES ID COR_IZQ expresion COR_DER DOS_PUNTOS tipo?
      ( LLAVE_IZQ listaExpresiones? LLAVE_DER )?
      PUNTO_COMA
    ;

// structura Persona { esto nombre : textum; esto edad : numerus; } finis;
declaracionEstructura
    : STRUCTURA ID LLAVE_IZQ atributoEstructura* LLAVE_DER FINIS PUNTO_COMA
    ;

// Un campo arreglo dentro de una structura NO lleva tamano: la dimension
// se da recien al declarar la variable (series animales : Animal;).
// El separador puede ser ';' o ',' porque el enunciado usa ambos, y es
// opcional en el ultimo campo.
atributoEstructura
    : ESTO   ID DOS_PUNTOS tipo ( PUNTO_COMA | COMA )?   # campoSimple
    | SERIES ID DOS_PUNTOS tipo ( PUNTO_COMA | COMA )?   # campoArreglo
    ;

// bool es el tipo booleano explicito. verum/falsus se conservan como
// tipo por la forma especial del enunciado (esto activo : verum;).
tipo
    : NUMERUS
    | DECIMALIS
    | TEXTUM
    | LITTERA
    | BOOL
    | VERUM
    | FALSUS
    | ID        // tipo definido por el usuario (structura)
    ;

/* =====================================================================
 * 3. FUNCIONES
 * ===================================================================== */
declaracionFuncion
    : funcionSinRetorno
    | funcionConRetorno
    ;

// actio atacarCerdos(esto fuerza : numerus) { ... } finis;
funcionSinRetorno
    : ACTIO ID PAR_IZQ listaParametros? PAR_DER cuerpoFuncion FINIS PUNTO_COMA
    ;

// ratio numerus calcularPoder(esto fuerza : numerus) { ... } finis;
funcionConRetorno
    : RATIO tipo ID PAR_IZQ listaParametros? PAR_DER cuerpoFuncion FINIS PUNTO_COMA
    ;

// El cuerpo de una funcion puede abrir con la seccion VARIABILES[ ... ],
// que es el unico lugar donde el enunciado permite declarar variables
// locales. Es distinto de bloque (el de si/dum/per), que no la lleva.
cuerpoFuncion
    : LLAVE_IZQ seccionVariablesLocales? instruccion* LLAVE_DER
    ;

seccionVariablesLocales
    : VARIABILES_LOCAL declaracionGlobal* COR_DER
    ;

listaParametros : parametro ( COMA parametro )* ;
parametro       : ESTO ID DOS_PUNTOS tipo ;
bloque          : LLAVE_IZQ instruccion* LLAVE_DER ;

/* =====================================================================
 * 4. INSTRUCCIONES
 * ===================================================================== */
instruccion
    : declaracionVariable
    | declaracionArreglo
    | declaracionEstructura
    | instruccionSi
    | instruccionMientras
    | instruccionHacerMientras
    | instruccionPara
    | instruccionRetorno
    | instruccionInterrumpe
    | instruccionPerge
    | instruccionSalida
    | instruccionEntrada
    | asignacion
    | instruccionIncremento
    | instruccionLlamada
    ;

// x = 5;   arr[0] = 5;   persona.edad = 5;
// mi_selva.animales[1] = { nombre: "Perro", apodo: "Canis" }
// El ';' es opcional solo en la forma que asigna un literal de estructura,
// tal como aparece en el enunciado.
asignacion
    : destino ASIGNACION literalCompuesto PUNTO_COMA?  # asignacionEstructura
    | destino ASIGNACION expresion PUNTO_COMA          # asignacionSimple
    ;

// El destino usa el patron iterativo: sin recursividad izquierda.
destino : ID sufijoDestino* ;

sufijoDestino
    : COR_IZQ expresion COR_DER   # accesoIndice
    | PUNTO ID                    # accesoAtributo
    ;

// i++;  i--;   (validos en cualquier ambito, no solo dentro del per)
instruccionIncremento : destino ( INCREMENTO | DECREMENTO ) PUNTO_COMA ;

// atacarCerdos(10);
instruccionLlamada : llamadaFuncion PUNTO_COMA ;

// si (c) { } aliter (c2) { } aliter { } finis;
// La cadena de aliter con condicion es lo que permite el else-if del
// enunciado. El aliter final, sin condicion, va aparte y es opcional.
instruccionSi
    : SI PAR_IZQ expresion PAR_DER bloque
      aliterCondicional*
      ( ALITER bloque )?
      FINIS PUNTO_COMA
    ;

aliterCondicional : ALITER PAR_IZQ expresion PAR_DER bloque ;

// dum (c) { } finis;
instruccionMientras
    : DUM PAR_IZQ expresion PAR_DER bloque FINIS PUNTO_COMA
    ;

// facere { } dum (c);
instruccionHacerMientras
    : FACERE bloque DUM PAR_IZQ expresion PAR_DER PUNTO_COMA
    ;

// per (esto i : numerus 0; i < 10; i++) { }
// La inicializacion y la condicion consumen cada una su propio ';'.
// El finis; final es opcional: el enunciado lo muestra en unos ciclos
// y en otros no.
instruccionPara
    : PER PAR_IZQ inicializacionPara expresion PUNTO_COMA
      actualizacionPara PAR_DER bloque ( FINIS PUNTO_COMA )?
    ;

inicializacionPara : declaracionVariable | asignacion ;

actualizacionPara
    : destino ( INCREMENTO | DECREMENTO )   # actualizacionUnaria
    | destino ASIGNACION expresion          # actualizacionAsignacion
    ;

// reddere total;   reddere;
instruccionRetorno    : REDDERE expresion? PUNTO_COMA ;
instruccionInterrumpe : INTERRUMPE PUNTO_COMA ;
instruccionPerge      : PERGE PUNTO_COMA ;

// >> "Bienvenido" >> comandante ;
instruccionSalida : SALIDA expresion ( SALIDA expresion )* PUNTO_COMA ;

// comandante <<     |     << comandante     |     <<
// La lectura es la unica instruccion que NO termina en ';' (confirmado
// por el auxiliar), pero se acepta igual si viene.
//
// ponytail: un '<<' solo, seguido de una instruccion que empieza con ID,
// se traga ese ID como destino porque 'destino?' es greedy. Es ambiguedad
// del propio lenguaje del enunciado; se documenta en vez de inventar una
// regla artificial para taparla.
instruccionEntrada
    : destino ENTRADA PUNTO_COMA?    # entradaSufija
    | ENTRADA destino? PUNTO_COMA?   # entradaPrefija
    ;

/* =====================================================================
 * 5. EXPRESIONES  -- SIN RECURSIVIDAD POR LA IZQUIERDA --
 * ===================================================================== */
expresion : expresionOr ;

expresionOr             : expresionAnd ( OR expresionAnd )* ;
expresionAnd            : expresionIgualdad ( AND expresionIgualdad )* ;
expresionIgualdad       : expresionRelacional ( ( IGUALDAD | DIFERENTE ) expresionRelacional )* ;
expresionRelacional     : expresionAditiva ( ( MENOR | MAYOR | MENOR_IGUAL | MAYOR_IGUAL ) expresionAditiva )* ;
expresionAditiva        : expresionMultiplicativa ( ( MAS | MENOS ) expresionMultiplicativa )* ;
expresionMultiplicativa : expresionUnaria ( ( POR | DIVISION ) expresionUnaria )* ;

// Recursividad POR LA DERECHA: permitida, y necesaria para -(-x), non non x
expresionUnaria
    : NON expresionUnaria                            # unariaNegacionLogica
    | MENOS expresionUnaria                          # unariaNegativo
    | ( INCREMENTO | DECREMENTO ) expresionUnaria    # unariaPrefija
    | expresionSufijo                                # unariaSufijoDelegado
    ;

expresionSufijo : expresionPrimaria sufijoExpresion* ;

sufijoExpresion
    : COR_IZQ expresion COR_DER   # sufijoIndice
    | PUNTO ID                    # sufijoAtributo
    | INCREMENTO                  # sufijoIncremento
    | DECREMENTO                  # sufijoDecremento
    ;

expresionPrimaria
    : ENTERO                       # primariaEntero
    | DECIMAL                      # primariaDecimal
    | TEXTO                        # primariaTexto
    | CARACTER                     # primariaCaracter
    | VERUM                        # primariaVerdadero
    | FALSUS                       # primariaFalso
    | llamadaFuncion               # primariaLlamada
    | ID                           # primariaIdentificador
    | PAR_IZQ expresion PAR_DER    # primariaAgrupacion
    | literalCompuesto             # primariaLiteral
    ;

// Con nombre de atributo para instanciar structuras (orden libre,
// separado por ',' o ';'), y posicional para arreglos: {1, 1}.
//
// "animales: Animal[7]" no necesita regla propia: parsea como
// sufijoIndice sobre un identificador, y el analizador semantico lo
// distingue por el tipo declarado del campo.
literalCompuesto
    : LLAVE_IZQ campoLiteral ( ( COMA | PUNTO_COMA ) campoLiteral )* LLAVE_DER  # literalConNombre
    | LLAVE_IZQ listaExpresiones? LLAVE_DER                                     # literalPosicional
    ;

campoLiteral : ID DOS_PUNTOS expresion ;

llamadaFuncion   : ID PAR_IZQ listaExpresiones? PAR_DER ;
listaExpresiones : expresion ( COMA expresion )* ;
