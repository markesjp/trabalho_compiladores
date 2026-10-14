package scanner;

import java.util.Objects;

public final class Token {
    private final TipoToken tipo;
    private final String lexema;
    private final int linha;
    private final int coluna;

    public Token(TipoToken tipo, String lexema, int linha, int coluna) {
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.lexema = Objects.requireNonNull(lexema, "lexema");
        this.linha = linha;
        this.coluna = coluna;
    }

    public TipoToken tipo() {
        return tipo;
    }

    public String lexema() {
        return lexema;
    }

    public int linha() {
        return linha;
    }

    public int coluna() {
        return coluna;
    }

    @Override
    public String toString() {
        return tipo + "('" + lexema.replace("\\", "\\\\").replace("'", "\\'") + "') @ " + linha + ":" + coluna;
    }
}
