import java.util.*;

abstract class AbstractEqObject {

}


class Value extends AbstractEqObject {
    private double value;

    public Value(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }
}

class Function extends AbstractEqObject {


    public enum FuncArity {
        None,
        Unary,
        Binary
    }
    public enum Func {
        OpenBracket,
        CloseBracket,
        Plus,
        Minus,
        Multiply,
        Divide,
        Power,
        Mod,
        UnaryMinus
    }

    public static Map<FuncArity, Integer> ArityDict = new HashMap<>();
    static {
        ArityDict.put(FuncArity.None, 0);
        ArityDict.put(FuncArity.Unary, 1);
        ArityDict.put(FuncArity.Binary, 2);
    }


    private Func func;
    private FuncArity funcArity;


    public Function(String f) throws Exception {
        setFunc(parseFunc(f));
    }

    private void checkArity() {
        switch (getFunc()) {
            case OpenBracket:
            case CloseBracket:
                funcArity = FuncArity.None;
                break;
            case Plus:
            case Minus:
            case Multiply:
            case Divide:
            case Power:
            case Mod:
                funcArity = FuncArity.Binary;
                break;
            case UnaryMinus:
                funcArity = FuncArity.Unary;
                break;
            default:
                break;

        }
    }

    public Func getFunc() {
        return func;
    }

    public void setFunc(Func func) {
        this.func = func;
        checkArity();
    }


    public FuncArity getFuncArity() {
        return funcArity;
    }



    public int getPriotity() {
        switch (getFunc()) {
            case OpenBracket:
            case CloseBracket:
                return 0;
            case Plus:
            case Minus:
                return 1;
            case Multiply:
            case Divide:
                return 2;
            case Power:
                return 3;
            case Mod:
                return 4;
            case UnaryMinus:
                return 5;
            default:
                return 0;

        }
    }



    private Func parseFunc(String f) throws Exception{
        switch (f.toLowerCase()) {
            case "(":
                return Func.OpenBracket;
            case ")":
                return Func.CloseBracket;
            case "+":
                return Func.Plus;
            case "-":
                return Func.Minus;
            case "*":
                return Func.Multiply;
            case "/":
                return Func.Divide;
            case "^":
                return Func.Power;
            case "%":
                return Func.Mod;
            default:
                throw new Exception("Function not recognized");
        }
    }

    public double calc(Value[] args) throws Exception{
        switch (funcArity) {
            case Binary:
                if (args.length != 2)
                    throw new Exception("You need exactly 2 args for a binary function");
                break;
            case Unary:
                if (args.length != 1)
                    throw new Exception("You need exactly 1 arg for an unary function");
                break;
            default:
                throw new Exception("Unexpected function");
        }

        switch (getFunc()) {
            case Plus:
                return args[0].getValue() + args[1].getValue();
            case Minus:
                return args[0].getValue() - args[1].getValue();
            case Divide:
                return args[0].getValue() / args[1].getValue();
            case Multiply:
                return args[0].getValue() * args[1].getValue();
            case Power:
                return Math.pow(args[0].getValue(), args[1].getValue());
            case Mod:
                return args[0].getValue() % args[1].getValue();
            case UnaryMinus:
                return - args[0].getValue();
            default:
                throw new Exception("No function implementation found.");

        }
    }
}




class Token {
    public enum Type {
        Value,
        Function
    }

    private Type tokenType;
    private AbstractEqObject object;


    public Token(Type tokenType, AbstractEqObject object) {
        this.tokenType = tokenType;
        this.object = object;
    }

    public Type getTokenType() {
        return tokenType;
    }

    public AbstractEqObject getObject() {
        return object;
    }

    public Value getAsValue() throws Exception{
        if (tokenType != Type.Value)
            throw new Exception("This is not a value");


        try {
            return (Value) object;
        }
        catch(Exception ex) {
            throw new Exception("This is not a value");
        }
    }

    public Function getAsFunction() throws Exception{
        if (tokenType != Type.Function)
            throw new Exception("This is not a function");


        try {
            return (Function) object;
        }
        catch(Exception ex) {
            throw new Exception("This is not a function");
        }
    }
}


class Equation {
    private String expression;
    private List<Token> tokens;

    public Equation(String expr) throws Exception {
        expression = expr;
        convertToRPN();
    }

    private void convertToRPN() throws Exception{
        List<Token> separated = configureTokens();
        Stack<Token> functionsStack = new Stack<>();
        tokens = new LinkedList<>();

        for (Token token : separated) {
            switch (token.getTokenType()) {
                case Value:
                    tokens.add(token);
                    break;
                case Function:
                {
                    Function func = token.getAsFunction();

                    if (!functionsStack.isEmpty() && func.getFunc() != Function.Func.OpenBracket) {
                        if (func.getFunc() == Function.Func.CloseBracket) {
                            while (!functionsStack.isEmpty() &&
                                    functionsStack.peek().getAsFunction().getFunc() != Function.Func.OpenBracket) {
                                tokens.add(functionsStack.pop());
                            }
                            functionsStack.pop(); // Removing an opening bracket
                        }
                        else if (func.getPriotity() >= functionsStack.peek().getAsFunction().getPriotity()) {
                            functionsStack.add(token);
                        }
                        else {
                            while (!functionsStack.isEmpty() &&
                                    func.getPriotity() < functionsStack.peek().getAsFunction().getPriotity()) {
                                tokens.add(functionsStack.pop());
                            }
                            functionsStack.add(token);
                        }
                    }
                    else {
                        functionsStack.add(token);
                    }
                    break;
                }
            }
        }
        while (!functionsStack.isEmpty())
            tokens.add(functionsStack.pop());
    }

    private List<Token> configureTokens() throws Exception {
        List<Token> tokens = new ArrayList<>();
        List<String> separated = separateInput();
        for (String s : separated) {  // running through all the string tokens
            if (Character.isDigit(s.charAt(0))) { // this is a number
                Token token = new Token(Token.Type.Value, new Value(Double.parseDouble(s)));
                tokens.add(token);
            }
            //else if (Character.isLetter(s.charAt(0))) { //TODO: variables expansion
            //
            //}
            else { // all functions

                // handle unary plus and minus
                Function func = new Function(s);
                if (func.getFunc() == Function.Func.Plus
                        && (tokens.isEmpty() || tokens.get(tokens.size()-1).getTokenType() != Token.Type.Value)) {
                    continue;
                }

                if (func.getFunc() == Function.Func.Minus
                        && (tokens.isEmpty() || tokens.get(tokens.size()-1).getTokenType() != Token.Type.Value)) {
                    func.setFunc(Function.Func.UnaryMinus);
                }


                Token token = new Token(Token.Type.Function, func);
                tokens.add(token);
            }
        }

        return tokens;
    }

    private List<String> separateInput() {
        String[] test = {
                "-",
                "2",
                "*",
                "3",
                "+",
                "(",
                "-",
                "4",
                "-",
                "7",
                ")"
        };

        return Arrays.asList(test);
    }

    public double calc() throws Exception {
        Stack<Token> stack = new Stack<>();
        for (Token token  : tokens) {
            switch (token.getTokenType()) {
                case Value:
                    stack.add(token);
                    break;
                case Function:
                    // get exact number of arguments
                    int arity = Function.ArityDict.get(token.getAsFunction().getFuncArity());
                    Value[] values = new Value[arity];
                    for (int i = arity-1; i >= 0; --i) {
                        values[i] = stack.pop().getAsValue();
                    }
                    Value result = new Value(token.getAsFunction().calc(values));
                    stack.add(new Token(Token.Type.Value, result));
                    break;
            }
        }
        if (stack.size() != 1)
            throw new Exception("Unexpected error");
        return stack.peek().getAsValue().getValue();
    }

}


public class EQSolver {
    public EQSolver() {
    }

    private String trimExpression(String expression) {
        return expression;
    }

    public double calc(String expr) throws Exception {
        try {
            Equation equation = new Equation(trimExpression(expr));
            return equation.calc();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return Double.NaN;
    }





    public static void main(String[] args) throws Exception{
        EQSolver solver = new EQSolver();
        double result = solver.calc("2*3+(4-7)");
        System.out.println(result);
    }

}
