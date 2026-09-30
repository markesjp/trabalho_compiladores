package scanner;

import java.util.Objects;

public final class Token {
    private final TipoToken tipo;
    private final String lexema;
    private final int linha;
    private final int coluna;

    public Token(TipoToken tipo, String lexema, int linha, int coluna) {
        if (linha < 1 || coluna < 1) {
            throw new IllegalArgumentException("linha e coluna devem começar em 1");
        }
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

    public CategoriaToken categoria() {
        return tipo.categoria();
    }

    public int linha() {
        return linha;
    }

    public int coluna() {
        return coluna;
    }

    @Override
    public String toString() {
        return tipo + "('" + escape(lexema) + "') @ " + linha + ":" + coluna;
    }

    static String escape(String text) {
        return text.replace("\\", "\\\\").replace("'", "\\'")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
