//tool/GenerateAst.java
package jlack;

import java.util.List;

abstract class Stmt {
    interface Visitor<R> {
        R visitBlockStmt(Block stmt);
        R visitExpressionStmt(Expression stmt);
        R visitWriteStmt(Write stmt);
        R visitReadStmt(Read stmt);
        R visitReadNumStmt(ReadNum stmt);
        R visitLetStmt(Let stmt);
        R visitIfStmt(If stmt);
        R visitWhileStmt(While stmt);
        R visitRepeatUntilStmt(RepeatUntil stmt);
        R visitRepeatForStmt(RepeatFor stmt);
        R visitBreakStmt(Break stmt);
        R visitContinueStmt(Continue stmt);
    }
    static class Block extends Stmt {
        Block(List<Stmt> statements) {
            this.statements = statements;
        }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }

        final List<Stmt> statements;
    }
    static class Expression extends Stmt {
        Expression(Expr expression) {
            this.expression = expression;
        }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }

        final Expr expression;
    }
    static class Write extends Stmt {
        Write(Expr expression, String end) {
            this.expression = expression;
            this.end = end;
        }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWriteStmt(this);
    }

        final Expr expression;
        final String end;
    }
    static class Read extends Stmt {
        Read(Token name, Token token) {
            this.name = name;
            this.token = token;
        }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitReadStmt(this);
    }

        final Token name;
        final Token token;
    }
    static class ReadNum extends Stmt {
        ReadNum(Token name, Token token) {
            this.name = name;
            this.token = token;
        }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitReadNumStmt(this);
    }

        final Token name;
        final Token token;
    }
    static class Let extends Stmt {
        Let(Token name, Expr initialiser) {
            this.name = name;
            this.initialiser = initialiser;
        }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLetStmt(this);
    }

        final Token name;
        final Expr initialiser;
    }
    static class If extends Stmt {
        If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }

        final Expr condition;
        final Stmt thenBranch;
        final Stmt elseBranch;
    }
    static class While extends Stmt {
        While(Expr condition, Stmt body, Expr increment) {
            this.condition = condition;
            this.body = body;
            this.increment = increment;
        }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }

        final Expr condition;
        final Stmt body;
        final Expr increment;
    }
    static class RepeatUntil extends Stmt {
        RepeatUntil(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitRepeatUntilStmt(this);
    }

        final Expr condition;
        final Stmt body;
    }
    static class RepeatFor extends Stmt {
        RepeatFor(Expr times, Stmt body, Token forToken) {
            this.times = times;
            this.body = body;
            this.forToken = forToken;
        }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitRepeatForStmt(this);
    }

        final Expr times;
        final Stmt body;
        final Token forToken;
    }
    static class Break extends Stmt {
        Break(Token token) {
            this.token = token;
        }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBreakStmt(this);
    }

        final Token token;
    }
    static class Continue extends Stmt {
        Continue(Token token) {
            this.token = token;
        }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitContinueStmt(this);
    }

        final Token token;
    }

    abstract <R> R accept(Visitor<R> visitor);
}
