parser grammar LatinParser;

options { tokenVocab = LatinLexer; }

/* =====================================================================
 *  PARSER DEL LENGUAJE LATIN
 *
 *  >>> GRAMATICA LIBRE DE RECURSIVIDAD POR LA IZQUIERDA <<<
 *
 *  Toda la precedencia usa el patron iterativo A : B (op B)* , resultado
 *  de la eliminacion clasica:  E -> E + T | T   ==   T ( '+' T )*
 *
 *  Precedencia (de menor a mayor):
 *      ||  ->  &&  ->  == !=  ->  < > <= >=  ->  + -  ->  * /  ->
 *      non, - unario, ++/-- prefijos  ->  sufijos ([] . ())  ->  primaria
 *
 *  Por que se escribe asi si ANTLR4 soporta recursividad izquierda, y
 *  la gramatica regla por regla: docs/05-Manual-Tecnico.md (5)
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
// El ';' es opcional tras un literal de estructura ("terminan con los }",
// confirmado por el auxiliar) y el '=' es opcional en toda declaracion.
// Ante el empate ANTLR gana la primera alternativa.
declaracionVariable
    : ESTO ID DOS_PUNTOS tipo ASIGNACION? literalCompuesto PUNTO_COMA?  # declaracionVariableEstructura
    | ESTO ID DOS_PUNTOS tipo ( ASIGNACION? expresion )? PUNTO_COMA     # declaracionVariableSimple
    ;

// series mis_enteros[2] : numerus {1, 1};
//
// El tipo es opcional por la forma vieja del arreglo booleano
// (Telegram 6/08 22:08): "series valores[2] : {verum, verum};".
declaracionArreglo
    : SERIES ID COR_IZQ expresion COR_DER DOS_PUNTOS tipo?
      ( LLAVE_IZQ listaExpresiones? LLAVE_DER )?
      PUNTO_COMA
    ;

// structura Persona { esto nombre : textum; esto edad : numerus; } finis;
declaracionEstructura
    : STRUCTURA ID LLAVE_IZQ atributoEstructura* LLAVE_DER FINIS PUNTO_COMA
    ;

// Un campo arreglo NO lleva tamano: la dimension llega al declarar la
// variable. El separador puede ser ';' o ',': el enunciado usa ambos.
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

// VARIABILES[ ... ] es el unico lugar donde se declaran locales. Por eso
// cuerpoFuncion es distinto de bloque (el de si/dum/per).
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
// La cadena de aliter con condicion es el else-if; el aliter final va aparte.
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
// La inicializacion y la condicion consumen cada una su ';'. El finis;
// final es opcional: el enunciado lo muestra en unos ciclos y en otros no.
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

// comandante <<   guarda lo leido    |    <<   lee y descarta
// Unica instruccion que NO termina en ';' (confirmado por el auxiliar),
// pero se acepta igual si viene. Sin destino, lee y descarta.
// El destino va ANTES del '<<': una forma prefija haria que un '<<' solo se
// tragara el ID de la instruccion siguiente.
instruccionEntrada : destino? ENTRADA PUNTO_COMA? ;

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

// Con nombre para structuras (orden libre), posicional para arreglos.
// "animales: Animal[7]" no necesita regla propia: parsea como sufijoIndice
// y la semantica lo distingue por el tipo declarado del campo.
literalCompuesto
    : LLAVE_IZQ campoLiteral ( ( COMA | PUNTO_COMA ) campoLiteral )* LLAVE_DER  # literalConNombre
    | LLAVE_IZQ listaExpresiones? LLAVE_DER                                     # literalPosicional
    ;

campoLiteral : ID DOS_PUNTOS expresion ;

llamadaFuncion   : ID PAR_IZQ listaExpresiones? PAR_DER ;
listaExpresiones : expresion ( COMA expresion )* ;
