package jlack;

import java.util.HashMap;
import java.util.Map;

class Env {
    final Env enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Env() {
        this.enclosing = null;
    }

    Env(Env enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, Object val) {
        values.put(name, val);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) return values.get(name.lexeme);

        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name,
            String.format("Undefined variable '%s'", name.lexeme)
        );
    }

    void assign(Token name, Object val) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, val);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, val);
            return;
        }

        throw new RuntimeError(name,
            String.format("Undefined variable '%s'", name.lexeme)
        );
    }
}
