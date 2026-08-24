parser grammar LatinParser;

options { tokenVocab = LatinLexer; }

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

declaracionVariable
    : ESTO ID DOS_PUNTOS tipo ASIGNACION? literalCompuesto PUNTO_COMA?  # declaracionVariableEstructura
    | ESTO ID DOS_PUNTOS tipo ( ASIGNACION? expresion )? PUNTO_COMA     # declaracionVariableSimple
    ;

declaracionArreglo
    : SERIES ID COR_IZQ expresion COR_DER DOS_PUNTOS tipo?
      ( LLAVE_IZQ listaExpresiones? LLAVE_DER )?
      PUNTO_COMA
    ;

declaracionEstructura
    : STRUCTURA ID LLAVE_IZQ atributoEstructura* LLAVE_DER FINIS PUNTO_COMA
    ;

atributoEstructura
    : ESTO   ID DOS_PUNTOS tipo ( PUNTO_COMA | COMA )?   # campoSimple
    | SERIES ID DOS_PUNTOS tipo ( PUNTO_COMA | COMA )?   # campoArreglo
    ;

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

funcionSinRetorno
    : ACTIO ID PAR_IZQ listaParametros? PAR_DER cuerpoFuncion FINIS PUNTO_COMA
    ;

funcionConRetorno
    : RATIO tipo ID PAR_IZQ listaParametros? PAR_DER cuerpoFuncion FINIS PUNTO_COMA
    ;

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

asignacion
    : destino ASIGNACION literalCompuesto PUNTO_COMA?  # asignacionEstructura
    | destino ASIGNACION expresion PUNTO_COMA          # asignacionSimple
    ;

destino : ID sufijoDestino* ;

sufijoDestino
    : COR_IZQ expresion COR_DER   # accesoIndice
    | PUNTO ID                    # accesoAtributo
    ;

instruccionIncremento : destino ( INCREMENTO | DECREMENTO ) PUNTO_COMA ;

instruccionLlamada : llamadaFuncion PUNTO_COMA ;

instruccionSi
    : SI PAR_IZQ expresion PAR_DER bloque
      aliterCondicional*
      ( ALITER bloque )?
      FINIS PUNTO_COMA
    ;

aliterCondicional : ALITER PAR_IZQ expresion PAR_DER bloque ;

instruccionMientras
    : DUM PAR_IZQ expresion PAR_DER bloque FINIS PUNTO_COMA
    ;

instruccionHacerMientras
    : FACERE bloque DUM PAR_IZQ expresion PAR_DER PUNTO_COMA
    ;

instruccionPara
    : PER PAR_IZQ inicializacionPara expresion PUNTO_COMA
      actualizacionPara PAR_DER bloque ( FINIS PUNTO_COMA )?
    ;

inicializacionPara : declaracionVariable | asignacion ;

actualizacionPara
    : destino ( INCREMENTO | DECREMENTO )   # actualizacionUnaria
    | destino ASIGNACION expresion          # actualizacionAsignacion
    ;

instruccionRetorno    : REDDERE expresion? PUNTO_COMA ;
instruccionInterrumpe : INTERRUMPE PUNTO_COMA ;
instruccionPerge      : PERGE PUNTO_COMA ;

instruccionSalida : SALIDA expresion ( SALIDA expresion )* PUNTO_COMA ;

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

literalCompuesto
    : LLAVE_IZQ campoLiteral ( ( COMA | PUNTO_COMA ) campoLiteral )* LLAVE_DER  # literalConNombre
    | LLAVE_IZQ listaExpresiones? LLAVE_DER                                     # literalPosicional
    ;

campoLiteral : ID DOS_PUNTOS expresion ;

llamadaFuncion   : ID PAR_IZQ listaExpresiones? PAR_DER ;
listaExpresiones : expresion ( COMA expresion )* ;
