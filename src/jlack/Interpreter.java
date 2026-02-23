package jlack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    Env env = new Env();
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    private boolean isInLoop = false;
    private boolean breakSignal = false;
    private boolean continueSignal = false;

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lack.runtimeError(error);
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case NOT:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings");
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case STAR:
                String string = null;
                Double number = 0.0;
                if (left instanceof String) {
                    if (right instanceof Double) {
                        string = (String) left;
                        number = (Double) right;
                        if (number % 1 != 0) throw new RuntimeError(expr.operator, "String can only be multiplied by int");
                    } else {
                        throw new RuntimeError(expr.operator, "String can only be multiplied by int");
                    }
                } else if (right instanceof String) {
                    if (left instanceof Double) {
                        string = (String) right;
                        number = (Double) left;
                        if (number % 1 != 0) throw new RuntimeError(expr.operator, "String can only be multiplied by int");
                    } else {
                        throw new RuntimeError(expr.operator, "String can only be multiplied by int");
                    }
                }
                if (string != null) {
                    double d = (double) number;
                    int i = (int) d;
                    return string.repeat(i);
                }

                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double) right == 0) {
                    throw new RuntimeError(expr.operator, "Division by zero");
                }
                return (double)left / (double)right;
            case MODULO:
                checkNumberOperands(expr.operator, left, right);
                if ((double) right == 0) {
                    throw new RuntimeError(expr.operator, "Modulo by zero");
                }
                return (double)left % (double)right;
        }
        return null;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }
        return (evaluate(expr.right));
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return env.get(expr.name);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object val = evaluate(expr.value);
        env.assign(expr.name, val);
        return val;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitWriteStmt(Stmt.Write stmt) {
        Object value = evaluate(stmt.expression);
        System.out.print(stringify(value) + stmt.end);
        return null;
    }

    @Override
    public Void visitReadStmt(Stmt.Read stmt) {
        Object val = getUserInput(false, stmt.token);
        env.assign(stmt.name, val);
        return null;
    }

    @Override
    public Void visitReadNumStmt(Stmt.ReadNum stmt) {
        Object val = getUserInput(true, stmt.token);
        env.assign(stmt.name, val);
        return null;
    }

    @Override
    public Object visitEvalExpr(Expr.Eval expr) {
        Object value = evaluate(expr.string);
        // Lexer lexer = new Lexer(value);
        // List<Token> tokens = lexer.lexTokens();
        // // for (Token token : tokens) System.out.println(token);

        // Parser parser = new Parser(tokens);
        // List<Stmt> statements = parser.parse();

        // if (hadError) return;
        // // System.out.println(new AstPrinter().print(expression));
        // final Interpreter interpreter = new Interpreter();
        // interpreter.evaluate(statements);

        return null;
    }

    @Override
    public Void visitLetStmt(Stmt.Let stmt) {
        Object val = null;
        if (stmt.initialiser != null) {
            val = evaluate(stmt.initialiser);
        }
        env.define(stmt.name.lexeme, val);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Env(env));
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        isInLoop = true;
        while (isTruthy(evaluate(stmt.condition))) {
            isInLoop = true;
            if (breakSignal) {
                breakSignal = false;
                continueSignal = false;
                break;
            }

            execute(stmt.body);
            isInLoop = true;
            if (breakSignal) {
                breakSignal = false;
                continueSignal = false;
                break;
            }
            breakSignal = false;

            if (stmt.increment != null) evaluate(stmt.increment);
            if (continueSignal) {
                continueSignal = false;
                continue;
            }
        }
        breakSignal = false;
        continueSignal = false;
        isInLoop = false;
        return null;
    }

    @Override
    public Void visitRepeatUntilStmt(Stmt.RepeatUntil stmt) {
        isInLoop = true;
        for (;;) {
            isInLoop = true;
            if (breakSignal) {
                breakSignal = false;
                continueSignal = false;
                break;
            }
            execute(stmt.body);
            isInLoop = true;
            if (breakSignal) {
                breakSignal = false;
                continueSignal = false;
                break;
            }
            breakSignal = false;

            if (isTruthy(evaluate(stmt.condition))) break;

            if (continueSignal) {
                continueSignal = false;
                continue;
            }
        }
        isInLoop = false;
        return null;
    }

    @Override
    public Void visitRepeatForStmt(Stmt.RepeatFor stmt) {
        Object times = evaluate(stmt.times);
        Double n = 0.0;
        if (!(times instanceof Double)) {
            throw new RuntimeError(stmt.forToken, "Expected integer after 'for'");
        }
        n = (Double) times;
        if (n % 1 != 0) {
            throw new RuntimeError(stmt.forToken, "Expected integer after 'for'");
        }
        isInLoop = true;
        for (int i=0; i < n; i++) {
            isInLoop = true;
            if (breakSignal) {
                breakSignal = false;
                continueSignal = false;
                break;
            }
            execute(stmt.body);
            isInLoop = true;
            if (breakSignal) {
                breakSignal = false;
                continueSignal = false;
                break;
            }
            breakSignal = false;

            if (continueSignal) {
                continueSignal = false;
                continue;
            }
        }
        isInLoop = false;
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        return null;
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        return null;
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        if (!isInLoop) {
            if (stmt instanceof Stmt.Break) {
                Stmt.Break breakStmt = (Stmt.Break) stmt;
                throw new RuntimeError(breakStmt.token, "'break' must be inside a loop");
            } else if (stmt instanceof Stmt.Continue) {
                Stmt.Continue continueStmt = (Stmt.Continue) stmt;
                throw new RuntimeError(continueStmt.token, "'continue' must be inside a loop");
            }
        } else {
            if (stmt instanceof Stmt.Break) {
                breakSignal = true;
            } else if (stmt instanceof Stmt.Continue) {
                continueSignal = true;
            }
        }
        stmt.accept(this);
    }

    void executeBlock(List<Stmt> statements, Env env) {
        Env previous = this.env;
        try {
            this.env = env;
            if (isInLoop) {
                for (Stmt statement : statements) {
                    if (statement instanceof Stmt.Break || breakSignal) {
                        breakSignal = true;
                        break;
                    } else if (statement instanceof Stmt.Continue || continueSignal) {
                        continueSignal = true;
                        break;
                    }
                    execute(statement);
                }
            } else {
                for (Stmt statement : statements) {
                    if (statement instanceof Stmt.Break) {
                        Stmt.Break breakStmt = (Stmt.Break) statement;
                        throw new RuntimeError(breakStmt.token, "'break' must be inside a loop");
                    } else if (statement instanceof Stmt.Continue) {
                        Stmt.Continue continueStmt = (Stmt.Continue) statement;
                        throw new RuntimeError(continueStmt.token, "'continue' must be inside a loop");
                    } else {
                        execute(statement);
                    }
                }
            }
        } finally {
            this.env = previous;
        }
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        if (object instanceof Double) {
            if (object.toString() == "0.0") return false;
        }
        if (object instanceof String) {
            if (object == "") return false;
        }
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers");
    }

    private String stringify(Object object) {
        if (object == null) return "nil";
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length()-2);
            }
            return text;
        }
        return object.toString();
    }
    
    private Object getUserInput(boolean isNum, Token token) {
        Object result = null;
        try {
            String text = reader.readLine();

            if (isNum) {
                result = Double.parseDouble(text);
            } else {
                result = text;
            }
        } catch (IOException error) {
            throw new RuntimeError(token, "Invalid input");
        } catch (NumberFormatException error) {}
        return result;
    }
}