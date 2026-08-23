package com.dmaldonado.codex_latinus.model.analysis;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import com.dmaldonado.codex_latinus.model.ast.AstVisitor;
import com.dmaldonado.codex_latinus.model.ast.Expression;
import com.dmaldonado.codex_latinus.model.ast.declaration.ArrayDeclaration;
import com.dmaldonado.codex_latinus.model.ast.declaration.FunctionDeclaration;
import com.dmaldonado.codex_latinus.model.ast.declaration.Parameter;
import com.dmaldonado.codex_latinus.model.ast.declaration.Program;
import com.dmaldonado.codex_latinus.model.ast.declaration.StructDeclaration;
import com.dmaldonado.codex_latinus.model.ast.declaration.StructField;
import com.dmaldonado.codex_latinus.model.ast.declaration.VariableDeclaration;
import com.dmaldonado.codex_latinus.model.ast.expression.ArrayAccessExpression;
import com.dmaldonado.codex_latinus.model.ast.expression.BinaryExpression;
import com.dmaldonado.codex_latinus.model.ast.expression.CompositeLiteralExpression;
import com.dmaldonado.codex_latinus.model.ast.expression.FunctionCallExpression;
import com.dmaldonado.codex_latinus.model.ast.expression.IdentifierExpression;
import com.dmaldonado.codex_latinus.model.ast.expression.IncrementExpression;
import com.dmaldonado.codex_latinus.model.ast.expression.LiteralExpression;
import com.dmaldonado.codex_latinus.model.ast.expression.MemberAccessExpression;
import com.dmaldonado.codex_latinus.model.ast.expression.UnaryExpression;
import com.dmaldonado.codex_latinus.model.ast.statement.Assignment;
import com.dmaldonado.codex_latinus.model.ast.statement.Block;
import com.dmaldonado.codex_latinus.model.ast.statement.BreakStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.CallStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.ContinueStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.DoWhileStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.ForStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.IfStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.IncrementStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.InputStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.PrintStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.ReturnStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.WhileStatement;
import com.dmaldonado.codex_latinus.model.errors.ErrorManager;
import com.dmaldonado.codex_latinus.model.symbols.ArraySymbol;
import com.dmaldonado.codex_latinus.model.symbols.FunctionSymbol;
import com.dmaldonado.codex_latinus.model.symbols.StructSymbol;
import com.dmaldonado.codex_latinus.model.symbols.Symbol;
import com.dmaldonado.codex_latinus.model.symbols.SymbolCategory;
import com.dmaldonado.codex_latinus.model.symbols.SymbolTable;
import com.dmaldonado.codex_latinus.model.symbols.VariableSymbol;
import com.dmaldonado.codex_latinus.model.types.DataType;
import com.dmaldonado.codex_latinus.model.types.TypeSystem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Semantic analysis: every visitX validates its rule and returns the DataType,
 * so the parent keeps composing the check upwards. On failure it reports the
 * error and returns ERROR instead of throwing.
 *
 * Detalle regla por regla: docs/05-Manual-Tecnico.md (9)
 */
public class SemanticAnalyzer implements AstVisitor<DataType>
{
    private final ErrorManager errorManager;
    private final SymbolTable  symbolTable;

    /** Function being analyzed; null inside MAIOR. */
    private FunctionDeclaration currentFunction;
    /** Loop nesting: interrumpe/perge are only valid when greater than zero. */
    private int                 loopDepth;
    /**
     * True donde el enunciado prohibe declarar: el cuerpo de si/dum/facere/per
     * y el cuerpo de una funcion, cuyas variables van todas en VARIABILES[ ].
     */
    private boolean             declarationsForbidden;
    /** Only to give each per scope a distinct name in the symbol table graph. */
    private int                 forCounter;

    public SemanticAnalyzer(ErrorManager errorManager, SymbolTable symbolTable)
    {
        this.errorManager = errorManager;
        this.symbolTable  = symbolTable;
    }

    /* =================================================================
     * PROGRAM
     * ================================================================= */

    /** Paso 1 registra firmas, paso 2 recorre cuerpos: permite el uso adelantado. */
    @Override
    public DataType visitProgram(Program node)
    {
        for (AstNode global : node.getGlobals())
        {
            if (global instanceof StructDeclaration struct)
            {
                registerStruct(struct);
            }
        }
        for (FunctionDeclaration function : node.getFunctions())
        {
            registerFunctionSignature(function);
        }

        for (AstNode global : node.getGlobals())
        {
            if (global instanceof StructDeclaration struct)
            {
                validateStructFieldTypes(struct);   // already registered in pass 1
            }
            else
            {
                global.accept(this);
            }
        }
        for (FunctionDeclaration function : node.getFunctions())
        {
            function.accept(this);
        }

        symbolTable.openScope("MAIOR");
        for (AstNode statement : node.getMainStatements())
        {
            statement.accept(this);
        }
        symbolTable.closeScope();
        return DataType.VOID;
    }

    /* =================================================================
     * DECLARATIONS
     * ================================================================= */
    @Override
    public DataType visitVariableDeclaration(VariableDeclaration node)
    {
        requireDeclarationPlace(node, node.getName());

        if (symbolTable.lookupLocal(node.getName()) != null)
        {
            error(node, "La variable '" + node.getName() + "' ya fue declarada en el ambito '"
                    + symbolTable.getCurrentScopeName() + "'.", node.getName());
            return DataType.ERROR;
        }

        DataType declaredType = node.getType();
        String   structName   = null;

        if (declaredType == DataType.ESTRUCTURA)
        {
            StructSymbol struct = symbolTable.lookupStruct(node.getTypeText());

            if (struct == null)
            {
                error(node, "El tipo '" + node.getTypeText() + "' no existe.", node.getTypeText());
                symbolTable.declare(new VariableSymbol(node.getName(), DataType.ERROR,
                        node.getTypeText(), SymbolCategory.VARIABLE,
                        symbolTable.getCurrentScopeName(), node.getInitialValue() != null,
                        null, false, node.getLine(), node.getColumn()));
                return DataType.ERROR;
            }
            structName = struct.getName();
        }

        Expression value = node.getInitialValue();

        if (value != null)
        {
            DataType valueType = checkValue(value, declaredType, structName);

            if (!assignable(declaredType, structName, valueType, value.getStructName()))
            {
                error(node, "No se puede asignar un valor '"
                        + describe(valueType, value.getStructName()) + "' a la variable '"
                        + node.getName() + "' de tipo '" + node.getTypeText() + "'.",
                        node.getName());
            }
        }
        else if (declaredType == DataType.ESTRUCTURA)
        {
            // Statement: "todos los atributos tengan un valor explicito".
            error(node, "La variable '" + node.getName() + "' de tipo '" + node.getTypeText()
                    + "' debe inicializar todos sus atributos.", node.getName());
        }

        symbolTable.declare(new VariableSymbol(node.getName(), declaredType, node.getTypeText(),
                SymbolCategory.VARIABLE, symbolTable.getCurrentScopeName(), value != null,
                structName, false, node.getLine(), node.getColumn()));
        return declaredType;
    }

    @Override
    public DataType visitArrayDeclaration(ArrayDeclaration node)
    {
        requireDeclarationPlace(node, node.getName());

        if (symbolTable.lookupLocal(node.getName()) != null)
        {
            error(node, "El arreglo '" + node.getName() + "' ya fue declarado en el ambito '"
                    + symbolTable.getCurrentScopeName() + "'.", node.getName());
            return DataType.ERROR;
        }

        DataType elementType       = node.getElementType();
        String   elementStructName = null;

        if (node.getTypeText() == null)
        {
            error(node, "Falta el tipo del arreglo '" + node.getName()
                    + "' y no hay valores iniciales de los cuales deducirlo.", node.getName());
            elementType = DataType.ERROR;
        }
        else if (elementType == DataType.ESTRUCTURA)
        {
            StructSymbol struct = symbolTable.lookupStruct(node.getTypeText());

            if (struct == null)
            {
                error(node, "El tipo '" + node.getTypeText() + "' no existe.", node.getTypeText());
                symbolTable.declare(new ArraySymbol(node.getName(), DataType.ERROR,
                        node.getTypeText(), symbolTable.getCurrentScopeName(), -1,
                        node.getInitialValues().size(), null, node.getLine(), node.getColumn()));
                return DataType.ERROR;
            }
            elementStructName = struct.getName();
        }

        int size = -1;

        if (node.getSize() != null)
        {
            DataType sizeType = node.getSize().accept(this);

            if (sizeType != DataType.NUMERUS && sizeType != DataType.ERROR)
            {
                error(node, "El tamano del arreglo '" + node.getName()
                        + "' debe ser 'numerus', se recibio '" + sizeType + "'.",
                        node.getName());
            }
            Integer declared = extractInteger(node.getSize());

            if (declared != null && declared < 0)
            {
                error(node, "El tamano del arreglo '" + node.getName()
                        + "' no puede ser negativo.", node.getName());
            }
            else if (declared != null)
            {
                size = declared;
            }
        }

        List<Expression> values = node.getInitialValues();

        if (!values.isEmpty() && size >= 0 && values.size() != size)
        {
            error(node, "El arreglo '" + node.getName() + "' declara " + size
                    + " posiciones pero recibe " + values.size() + " valores.", node.getName());
        }
        for (Expression value : values)
        {
            DataType valueType = checkValue(value, elementType, elementStructName);

            if (!assignable(elementType, elementStructName, valueType, value.getStructName()))
            {
                error(value, "Valor de tipo '" + describe(valueType, value.getStructName())
                        + "' incompatible con el arreglo '" + node.getName() + "' de tipo '"
                        + node.getTypeText() + "'.", node.getName());
            }
        }

        symbolTable.declare(new ArraySymbol(node.getName(), elementType, node.getTypeText(),
                symbolTable.getCurrentScopeName(), size, values.size(), elementStructName,
                node.getLine(), node.getColumn()));
        return elementType;
    }

    /** Solo para una structura suelta: las de VARIABILES ya las registro el paso 1. */
    @Override
    public DataType visitStructDeclaration(StructDeclaration node)
    {
        registerStruct(node);
        validateStructFieldTypes(node);
        return DataType.ESTRUCTURA;
    }

    @Override
    public DataType visitStructField(StructField node)
    {
        return node.getType();
    }

    @Override
    public DataType visitParameter(Parameter node)
    {
        return node.getType();
    }

    @Override
    public DataType visitFunctionDeclaration(FunctionDeclaration node)
    {
        FunctionDeclaration previousFunction  = currentFunction;
        boolean             previousForbidden = declarationsForbidden;
        int                 previousDepth     = loopDepth;

        currentFunction       = node;
        declarationsForbidden = false;   // VARIABILES[ ] es justo donde SI se declara
        loopDepth             = 0;

        symbolTable.openScope(node.getName());

        if (node.returnsValue() && node.getReturnType() == DataType.ESTRUCTURA
                && symbolTable.lookupStruct(node.getReturnTypeText()) == null)
        {
            error(node, "El tipo de retorno '" + node.getReturnTypeText() + "' no existe.",
                    node.getReturnTypeText());
        }

        for (Parameter parameter : node.getParameters())
        {
            if (symbolTable.lookupLocal(parameter.getName()) != null)
            {
                error(parameter, "El parametro '" + parameter.getName() + "' esta repetido.",
                        parameter.getName());
                continue;
            }
            if (parameter.getType() == DataType.ESTRUCTURA
                    && symbolTable.lookupStruct(parameter.getTypeText()) == null)
            {
                error(parameter, "El tipo '" + parameter.getTypeText() + "' del parametro '"
                        + parameter.getName() + "' no existe.", parameter.getTypeText());
            }
            symbolTable.declare(new VariableSymbol(parameter.getName(), parameter.getType(),
                    parameter.getTypeText(), SymbolCategory.PARAMETER, node.getName(), true,
                    structNameOf(parameter.getType(), parameter.getTypeText()), false,
                    parameter.getLine(), parameter.getColumn()));
        }

        // Las structuras locales van primero: la siguiente variable ya puede usarlas.
        for (AstNode local : node.getLocalVariables())
        {
            if (local instanceof StructDeclaration struct)
            {
                registerStruct(struct);
            }
        }
        for (AstNode local : node.getLocalVariables())
        {
            if (local instanceof StructDeclaration struct)
            {
                validateStructFieldTypes(struct);
            }
            else
            {
                local.accept(this);
            }
        }

        // Enunciado: "dentro de las funciones solo se pueden definir variables
        // al principio". Pasada la seccion VARIABILES[ ], el cuerpo ya no admite
        // ninguna declaracion mas.
        declarationsForbidden = true;
        node.getBody().accept(this);
        symbolTable.closeScope();

        if (node.returnsValue() && !ReturnPathChecker.alwaysReturns(node.getBody()))
        {
            error(node, "La funcion '" + node.getName()
                    + "' no retorna un valor en todos sus caminos posibles.", node.getName());
        }

        currentFunction       = previousFunction;
        declarationsForbidden = previousForbidden;
        loopDepth             = previousDepth;
        return node.getReturnType();
    }

    /* =================================================================
     * STATEMENTS
     * ================================================================= */

    /** No abre ambito: declarar dentro de un bloque de control ya es error. */
    @Override
    public DataType visitBlock(Block node)
    {
        int unreachable = ReturnPathChecker.firstUnreachableIndex(node);

        if (unreachable >= 0)
        {
            error(node.getStatements().get(unreachable),
                    "Codigo inalcanzable: nunca se ejecutara esta instruccion.", "");
        }
        for (AstNode statement : node.getStatements())
        {
            statement.accept(this);
        }
        return DataType.VOID;
    }

    @Override
    public DataType visitAssignment(Assignment node)
    {
        DataType targetType   = node.getTarget().accept(this);
        String   targetStruct = node.getTarget().getStructName();
        DataType valueType    = checkValue(node.getValue(), targetType, targetStruct);

        if (!assignable(targetType, targetStruct, valueType, node.getValue().getStructName()))
        {
            error(node, "Asignacion invalida: no se puede guardar '"
                    + describe(valueType, node.getValue().getStructName())
                    + "' en un destino de tipo '" + describe(targetType, targetStruct) + "'.", "=");
        }
        return targetType;
    }

    @Override
    public DataType visitIfStatement(IfStatement node)
    {
        requireBooleanCondition(node.getCondition(), "si");

        boolean previousForbidden = declarationsForbidden;
        declarationsForbidden = true;

        node.getThenBranch().accept(this);

        if (node.getElseBranch() != null)
        {
            node.getElseBranch().accept(this);
        }
        declarationsForbidden = previousForbidden;
        return DataType.VOID;
    }

    @Override
    public DataType visitWhileStatement(WhileStatement node)
    {
        requireBooleanCondition(node.getCondition(), "dum");
        walkLoopBody(node.getBody());
        return DataType.VOID;
    }

    @Override
    public DataType visitDoWhileStatement(DoWhileStatement node)
    {
        walkLoopBody(node.getBody());
        requireBooleanCondition(node.getCondition(), "facere-dum");
        return DataType.VOID;
    }

    @Override
    public DataType visitForStatement(ForStatement node)
    {
        symbolTable.openScope("per_" + (++forCounter));

        // El iterador es la unica declaracion que el enunciado permite fuera de
        // VARIABILES[ ], asi que la bandera se limpia solo para el: dentro de una
        // funcion ya viene levantada y su "per (esto i : numerus 0; ...)" es valido.
        boolean previousForbidden = declarationsForbidden;
        declarationsForbidden     = false;

        if (node.getInitialization() != null)
        {
            node.getInitialization().accept(this);
        }
        declarationsForbidden = previousForbidden;

        requireBooleanCondition(node.getCondition(), "per");

        if (node.getUpdate() != null)
        {
            node.getUpdate().accept(this);
        }
        walkLoopBody(node.getBody());
        symbolTable.closeScope();
        return DataType.VOID;
    }

    @Override
    public DataType visitReturnStatement(ReturnStatement node)
    {
        if (currentFunction == null)
        {
            error(node, "'reddere' solo puede usarse dentro de una funcion.", "reddere");
            return DataType.ERROR;
        }

        if (node.getValue() == null)
        {
            if (currentFunction.returnsValue())
            {
                error(node, "La funcion '" + currentFunction.getName() + "' debe retornar '"
                        + currentFunction.getReturnTypeText() + "'.", "reddere");
            }
            return DataType.VOID;
        }

        String   expectedStruct = structNameOf(currentFunction.getReturnType(),
                                               currentFunction.getReturnTypeText());
        DataType returnedType   = checkValue(node.getValue(), currentFunction.getReturnType(),
                                             expectedStruct);

        if (!currentFunction.returnsValue())
        {
            error(node, "La funcion '" + currentFunction.getName()
                    + "' fue declarada con 'actio' y no puede retornar un valor.", "reddere");
        }
        else if (!assignable(currentFunction.getReturnType(), expectedStruct, returnedType,
                             node.getValue().getStructName()))
        {
            error(node, "Se retorna '"
                    + describe(returnedType, node.getValue().getStructName())
                    + "' pero la funcion '" + currentFunction.getName() + "' declara '"
                    + currentFunction.getReturnTypeText() + "'.", "reddere");
        }
        return returnedType;
    }

    @Override
    public DataType visitBreakStatement(BreakStatement node)
    {
        if (loopDepth == 0)
        {
            error(node, "'interrumpe' solo puede usarse dentro de un ciclo.", "interrumpe");
        }
        return DataType.VOID;
    }

    @Override
    public DataType visitContinueStatement(ContinueStatement node)
    {
        if (loopDepth == 0)
        {
            error(node, "'perge' solo puede usarse dentro de un ciclo.", "perge");
        }
        return DataType.VOID;
    }

    @Override
    public DataType visitPrintStatement(PrintStatement node)
    {
        for (Expression value : node.getValues())
        {
            value.accept(this);
        }
        return DataType.VOID;
    }

    @Override
    public DataType visitInputStatement(InputStatement node)
    {
        // A bare "<<" is valid: it reads and discards. There is nothing to check.
        if (node.getTarget() == null)
        {
            return DataType.VOID;
        }

        DataType type = node.getTarget().accept(this);

        if (type == DataType.ESTRUCTURA)
        {
            error(node, "No se puede leer una structura completa desde la entrada.", "<<");
        }
        return DataType.VOID;
    }

    @Override
    public DataType visitCallStatement(CallStatement node)
    {
        return node.getCall().accept(this);
    }

    @Override
    public DataType visitIncrementStatement(IncrementStatement node)
    {
        DataType type = operandType(node.getTarget(), node.getOperator());

        if (TypeSystem.incrementResult(type) == DataType.ERROR && type != DataType.ERROR)
        {
            error(node, "El operador '" + node.getOperator()
                    + "' solo aplica a numerus o decimalis, no a '" + type + "'.",
                    node.getOperator());
        }
        return type;
    }

    /* =================================================================
     * EXPRESSIONS
     * ================================================================= */
    @Override
    public DataType visitBinaryExpression(BinaryExpression node)
    {
        String   operator = node.getOperator();
        DataType left     = operandType(node.getLeft(), operator);
        DataType right    = operandType(node.getRight(), operator);

        DataType result = switch (operator)
        {
            case "+", "-", "*", "/"               -> TypeSystem.arithmeticResult(left, right, operator);
            case "==", "!=", "<", ">", "<=", ">=" -> TypeSystem.relationalResult(left, right, operator);
            case "&&", "||"                       -> TypeSystem.logicalResult(left, right);
            default                               -> DataType.ERROR;
        };

        if (result == DataType.ERROR && left != DataType.ERROR && right != DataType.ERROR)
        {
            // La pista solo aplica si hay un textum de por medio: sin esta guarda
            // salia hasta en "'Carro' == 'Carro'", donde no viene a cuento.
            boolean concatenation = (left == DataType.TEXTUM || right == DataType.TEXTUM)
                    && !"+".equals(operator);

            error(node, "Operacion invalida: '"
                    + describe(left, node.getLeft().getStructName()) + "' " + operator + " '"
                    + describe(right, node.getRight().getStructName()) + "'."
                    + (concatenation ? " Recuerde que textum solo admite '+'." : ""),
                    operator);
        }
        return typeCheck(node, result);
    }

    @Override
    public DataType visitUnaryExpression(UnaryExpression node)
    {
        DataType operand = operandType(node.getOperand(), node.getOperator());
        DataType result  = "non".equals(node.getOperator())
                ? TypeSystem.negationResult(operand)
                : TypeSystem.unaryMinusResult(operand);

        if (result == DataType.ERROR && operand != DataType.ERROR)
        {
            error(node, "El operador '" + node.getOperator() + "' no puede aplicarse a '"
                    + operand + "'.", node.getOperator());
        }
        return typeCheck(node, result);
    }

    @Override
    public DataType visitIncrementExpression(IncrementExpression node)
    {
        DataType operand = operandType(node.getTarget(), node.getOperator());
        DataType result  = TypeSystem.incrementResult(operand);

        if (result == DataType.ERROR && operand != DataType.ERROR)
        {
            error(node, "El operador '" + node.getOperator()
                    + "' solo aplica a numerus o decimalis, no a '" + operand + "'.",
                    node.getOperator());
        }
        return typeCheck(node, result);
    }

    @Override
    public DataType visitLiteralExpression(LiteralExpression node)
    {
        return typeCheck(node, node.getType());
    }

    @Override
    public DataType visitIdentifierExpression(IdentifierExpression node)
    {
        Symbol symbol = symbolTable.lookup(node.getName());

        if (symbol == null)
        {
            error(node, "La variable '" + node.getName() + "' no ha sido declarada.",
                    node.getName());
            return typeCheck(node, DataType.ERROR);
        }

        if (symbol instanceof FunctionSymbol)
        {
            error(node, "'" + node.getName() + "' es una funcion; para llamarla escriba '"
                    + node.getName() + "(...)'.", node.getName());
            return typeCheck(node, DataType.ERROR);
        }
        if (symbol instanceof StructSymbol)
        {
            error(node, "'" + node.getName() + "' es una structura, no una variable.",
                    node.getName());
            return typeCheck(node, DataType.ERROR);
        }
        if (symbol instanceof VariableSymbol variable)
        {
            node.setStructName(variable.getStructName());
        }
        else if (symbol instanceof ArraySymbol array)
        {
            // Reporta el tipo del ELEMENTO, que es lo que numeros[i] necesita.
            // Operar el arreglo pelado lo corta antes operandType().
            node.setStructName(array.getElementStructName());
        }
        return typeCheck(node, symbol.getType());
    }

    @Override
    public DataType visitArrayAccessExpression(ArrayAccessExpression node)
    {
        DataType indexType = node.getIndex().accept(this);

        if (indexType != DataType.NUMERUS && indexType != DataType.ERROR)
        {
            error(node, "El indice de un arreglo debe ser 'numerus', se recibio '"
                    + indexType + "'.", "[]");
        }

        DataType elementType = node.getArray().accept(this);

        // El nombre de la structura sobrevive al indice: sin eso no se resuelve
        // una cadena como mi_selva.animales[1].nombre.
        node.setStructName(node.getArray().getStructName());

        if (node.getArray() instanceof IdentifierExpression identifier)
        {
            Symbol symbol = symbolTable.lookup(identifier.getName());

            if (symbol != null && !(symbol instanceof ArraySymbol))
            {
                error(node, "'" + identifier.getName() + "' no es un arreglo.",
                        identifier.getName());
                return typeCheck(node, DataType.ERROR);
            }
            if (symbol instanceof ArraySymbol array)
            {
                Integer index = extractInteger(node.getIndex());

                if (index != null && index < 0)
                {
                    error(node, "Indice fuera de rango: las posiciones de un arreglo "
                            + "empiezan en 0.", "[]");
                }
                else if (index != null && array.getSize() >= 0 && index >= array.getSize())
                {
                    error(node, "Indice fuera de rango: el arreglo '" + array.getName()
                            + "' tiene " + array.getSize() + " posiciones.", "[]");
                }
            }
        }
        return typeCheck(node, elementType);
    }

    @Override
    public DataType visitMemberAccessExpression(MemberAccessExpression node)
    {
        node.getOwner().accept(this);
        String structName = node.getOwner().getStructName();

        if (structName == null)
        {
            error(node, "El acceso '." + node.getMemberName()
                    + "' solo es valido sobre una variable de tipo structura.",
                    node.getMemberName());
            return typeCheck(node, DataType.ERROR);
        }

        StructSymbol struct = symbolTable.lookupStruct(structName);

        if (struct == null || !struct.hasAttribute(node.getMemberName()))
        {
            error(node, "La structura '" + structName + "' no tiene el atributo '"
                    + node.getMemberName() + "'.", node.getMemberName());
            return typeCheck(node, DataType.ERROR);
        }

        VariableSymbol attribute = struct.getAttribute(node.getMemberName());
        node.setStructName(attribute.getStructName());
        return typeCheck(node, attribute.getType());
    }

    @Override
    public DataType visitFunctionCallExpression(FunctionCallExpression node)
    {
        FunctionSymbol function = symbolTable.lookupFunction(node.getName());

        if (function == null)
        {
            error(node, "La funcion '" + node.getName() + "' no ha sido declarada.",
                    node.getName());
            for (Expression argument : node.getArguments())
            {
                argument.accept(this);
            }
            return typeCheck(node, DataType.ERROR);
        }

        List<Expression> arguments = node.getArguments();

        if (arguments.size() != function.getParameterCount())
        {
            error(node, "La funcion '" + function.getSignature() + "' espera "
                    + function.getParameterCount() + " argumento(s) y recibio "
                    + arguments.size() + ".", node.getName());
        }

        int matched = Math.min(arguments.size(), function.getParameterCount());

        for (int i = 0; i < matched; i++)
        {
            VariableSymbol parameter    = function.getParameters().get(i);
            Expression     argument     = arguments.get(i);
            DataType       argumentType = checkValue(argument, parameter.getType(),
                                                     parameter.getStructName());

            if (!assignable(parameter.getType(), parameter.getStructName(), argumentType,
                            argument.getStructName()))
            {
                error(argument, "El argumento " + (i + 1) + " de '" + node.getName()
                        + "' debe ser '" + parameter.getTypeText() + "' y es '"
                        + describe(argumentType, argument.getStructName()) + "'.", node.getName());
            }
        }
        for (int i = matched; i < arguments.size(); i++)
        {
            arguments.get(i).accept(this);
        }

        node.setStructName(function.getReturnStructName());
        return typeCheck(node, function.getType());
    }

    /** Solo se alcanza con un { } fuera de lugar: checkValue desvia los tres validos. */
    @Override
    public DataType visitCompositeLiteralExpression(CompositeLiteralExpression node)
    {
        error(node, "Un literal '{ ... }' solo puede usarse al declarar o asignar una "
                + "structura, o como valores iniciales de un arreglo.", "{}");

        for (Expression value : node.getValues())
        {
            value.accept(this);
        }
        return typeCheck(node, DataType.ERROR);
    }

    /* =================================================================
     * Registration (pass 1)
     * ================================================================= */
    private void registerStruct(StructDeclaration node)
    {
        requireDeclarationPlace(node, node.getName());

        if (symbolTable.lookupLocal(node.getName()) != null)
        {
            error(node, "La structura '" + node.getName() + "' ya fue declarada en el ambito '"
                    + symbolTable.getCurrentScopeName() + "'.", node.getName());
            return;
        }

        StructSymbol struct = new StructSymbol(node.getName(), symbolTable.getCurrentScopeName(),
                node.getLine(), node.getColumn());

        for (StructField field : node.getFields())
        {
            VariableSymbol attribute = new VariableSymbol(field.getName(), field.getType(),
                    field.getTypeText(), SymbolCategory.ATTRIBUTE, node.getName(), false,
                    structNameOf(field.getType(), field.getTypeText()), field.isArray(),
                    field.getLine(), field.getColumn());

            if (!struct.addAttribute(attribute))
            {
                error(field, "El atributo '" + field.getName()
                        + "' esta repetido en la structura '" + node.getName() + "'.",
                        field.getName());
            }
        }
        symbolTable.declare(struct);
    }

    /** Paso 2: ya estan todas registradas, un campo puede citar una declarada despues. */
    private void validateStructFieldTypes(StructDeclaration node)
    {
        for (StructField field : node.getFields())
        {
            if (field.getType() == DataType.ESTRUCTURA
                    && symbolTable.lookupStruct(field.getTypeText()) == null)
            {
                error(field, "El tipo '" + field.getTypeText() + "' del atributo '"
                        + field.getName() + "' no existe.", field.getTypeText());
            }
        }
    }

    private void registerFunctionSignature(FunctionDeclaration node)
    {
        if (symbolTable.lookupFunction(node.getName()) != null)
        {
            error(node, "La funcion '" + node.getName() + "' ya fue declarada.", node.getName());
            return;
        }

        List<VariableSymbol> parameters = new ArrayList<>();

        for (Parameter parameter : node.getParameters())
        {
            parameters.add(new VariableSymbol(parameter.getName(), parameter.getType(),
                    parameter.getTypeText(), SymbolCategory.PARAMETER, node.getName(), true,
                    structNameOf(parameter.getType(), parameter.getTypeText()), false,
                    parameter.getLine(), parameter.getColumn()));
        }

        symbolTable.declare(new FunctionSymbol(node.getName(), node.getReturnType(),
                node.getReturnTypeText(), symbolTable.getCurrentScopeName(), parameters,
                node.returnsValue(),
                structNameOf(node.getReturnType(), node.getReturnTypeText()),
                node.getLine(), node.getColumn()));
    }

    /* =================================================================
     * Structure literals
     * ================================================================= */

    /**
     * Valida { nombre: "Yennifer", edad: 999 } contra su structura. El mapa
     * 'pending' cumple las tres reglas a la vez: nombre obligatorio, todos los
     * atributos presentes, y el orden no importa.
     */
    private void validateStructLiteral(CompositeLiteralExpression literal, StructSymbol struct)
    {
        if (struct == null)
        {
            return;
        }
        if (!literal.isNamed())
        {
            error(literal, "La structura '" + struct.getName()
                    + "' requiere valores con nombre de atributo, por ejemplo 'nombre: \"X\"'.",
                    struct.getName());
            return;
        }

        Map<String, VariableSymbol> pending = new LinkedHashMap<>(struct.getAttributes());

        for (int i = 0; i < literal.getFieldNames().size(); i++)
        {
            String         fieldName = literal.getFieldNames().get(i);
            Expression     value     = literal.getValues().get(i);
            VariableSymbol attribute = pending.remove(fieldName);

            if (attribute == null)
            {
                error(value, "'" + fieldName + "' no es un atributo de '"
                        + struct.getName() + "'.", fieldName);
                continue;
            }
            if (attribute.isArray())
            {
                validateArrayAttribute(value, attribute);
                continue;
            }

            DataType valueType = checkValue(value, attribute.getType(), attribute.getStructName());

            if (!assignable(attribute.getType(), attribute.getStructName(), valueType,
                            value.getStructName()))
            {
                error(value, "El atributo '" + fieldName + "' es '" + attribute.getTypeText()
                        + "' y recibio '" + describe(valueType, value.getStructName()) + "'.",
                        fieldName);
            }
        }

        for (String missing : pending.keySet())
        {
            error(literal, "Falta el atributo obligatorio '" + missing + "' de '"
                    + struct.getName() + "'.", missing);
        }
    }

    /**
     * "animales: Animal[7]" parsea como indice sobre el nombre del TIPO, no de
     * una variable, asi que no se resuelve contra la tabla de simbolos.
     */
    private void validateArrayAttribute(Expression value, VariableSymbol attribute)
    {
        if (!(value instanceof ArrayAccessExpression access)
                || !(access.getArray() instanceof IdentifierExpression typeName)
                || !typeName.getName().equals(attribute.getTypeText()))
        {
            error(value, "El atributo '" + attribute.getName()
                    + "' es un arreglo y se declara con su tamano, por ejemplo '"
                    + attribute.getName() + ": " + attribute.getTypeText() + "[7]'.",
                    attribute.getName());
            return;
        }

        DataType sizeType = access.getIndex().accept(this);

        if (sizeType != DataType.NUMERUS && sizeType != DataType.ERROR)
        {
            error(access, "El tamano del arreglo '" + attribute.getName()
                    + "' debe ser 'numerus', se recibio '" + sizeType + "'.",
                    attribute.getName());
        }

        typeName.setComputedType(attribute.getType());
        typeName.setStructName(attribute.getStructName());
        access.setComputedType(attribute.getType());
        access.setStructName(attribute.getStructName());
    }

    /* =================================================================
     * Support
     * ================================================================= */

    /** Verifica un valor contra su destino, desviando el literal de structura. */
    private DataType checkValue(Expression value, DataType targetType, String targetStruct)
    {
        if (targetType == DataType.ESTRUCTURA
                && value instanceof CompositeLiteralExpression literal)
        {
            validateStructLiteral(literal, symbolTable.lookupStruct(targetStruct));
            literal.setComputedType(DataType.ESTRUCTURA);
            literal.setStructName(targetStruct);
            return DataType.ESTRUCTURA;
        }
        return value.accept(this);
    }

    /**
     * Enunciado: "no se consideran primitivos los arreglos de datos". El nombre
     * pelado de un arreglo se indexa, no se opera.
     */
    private DataType operandType(Expression operand, String operator)
    {
        if (operand instanceof IdentifierExpression identifier
                && symbolTable.lookup(identifier.getName()) instanceof ArraySymbol)
        {
            error(operand, "El arreglo '" + identifier.getName() + "' no es un valor "
                    + "primitivo y no puede operarse con '" + operator + "'; use '"
                    + identifier.getName() + "[indice]'.", identifier.getName());
            return typeCheck(operand, DataType.ERROR);
        }
        return operand.accept(this);
    }

    /**
     * Same as TypeSystem.isAssignable, plus the rule TypeSystem cannot express:
     * two different structuras are both ESTRUCTURA, but a Persona is not a Carro.
     */
    private boolean assignable(DataType target, String targetStruct,
                               DataType source, String sourceStruct)
    {
        if (!TypeSystem.isAssignable(target, source))
        {
            return false;
        }
        if (target == DataType.ESTRUCTURA && source == DataType.ESTRUCTURA)
        {
            return targetStruct != null && targetStruct.equals(sourceStruct);
        }
        return true;
    }

    private void requireBooleanCondition(Expression condition, String statement)
    {
        if (condition == null)
        {
            return;
        }

        DataType type = condition.accept(this);

        if (type != DataType.BOOLEANO && type != DataType.ERROR)
        {
            error(condition, "La condicion de '" + statement
                    + "' debe ser booleana, se recibio '" + type + "'.", statement);
        }
    }

    /**
     * Enunciado: las variables solo se definen al principio de la funcion.
     * Reporta, pero el simbolo se declara igual para no encadenar un
     * "no declarada" en cada uso posterior.
     *
     * MAIOR> queda fuera a proposito (desviacion declarada en docs/05): alli
     * visitProgram nunca levanta la bandera.
     */
    private void requireDeclarationPlace(AstNode node, String name)
    {
        if (declarationsForbidden)
        {
            error(node, "'" + name + "' no puede declararse aqui. Las declaraciones van "
                    + "al inicio de la funcion, en la seccion VARIABILES[ ].", name);
        }
    }

    private void walkLoopBody(Block body)
    {
        boolean previousForbidden = declarationsForbidden;
        declarationsForbidden = true;
        loopDepth++;

        body.accept(this);

        loopDepth--;
        declarationsForbidden = previousForbidden;
    }

    /**
     * Constant value of an integer expression, or null when it is not constant.
     * Integer and not int: -1 is a valid index, so it cannot double as the
     * "not constant" marker.
     */
    private Integer extractInteger(Expression expression)
    {
        if (expression instanceof LiteralExpression literal
                && literal.getType() == DataType.NUMERUS)
        {
            try
            {
                return Integer.parseInt(literal.getText());
            }
            catch (NumberFormatException ignored)
            {
                return null;   // out of int range
            }
        }
        if (expression instanceof UnaryExpression unary && "-".equals(unary.getOperator()))
        {
            Integer value = extractInteger(unary.getOperand());
            return value == null ? null : -value;
        }
        // Statement: an index "que se puede evaluar al hacer la verificacion
        // de semantica" has to be range checked, so numeros[1 + 1] is folded.
        if (expression instanceof BinaryExpression binary)
        {
            Integer left  = extractInteger(binary.getLeft());
            Integer right = extractInteger(binary.getRight());

            if (left == null || right == null)
            {
                return null;
            }

            return switch (binary.getOperator())
            {
                case "+" -> left + right;
                case "-" -> left - right;
                case "*" -> left * right;
                case "/" -> right == 0 ? null : left / right;
                default  -> null;
            };
        }
        return null;
    }

    /** Como se nombra un tipo en un mensaje: "Carro", no "structura". */
    private String describe(DataType type, String structName)
    {
        return (type == DataType.ESTRUCTURA && structName != null)
                ? structName : type.toString();
    }

    private String structNameOf(DataType type, String typeText)
    {
        return type == DataType.ESTRUCTURA ? typeText : null;
    }

    private DataType typeCheck(Expression node, DataType type)
    {
        node.setComputedType(type);
        return type;
    }

    private void error(AstNode node, String description, String lexeme)
    {
        errorManager.addSemantic(description, lexeme, node.getLine(), node.getColumn());
    }
}
