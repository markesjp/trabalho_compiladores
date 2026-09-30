package scanner;

public final class ErrorLexico {
    private final String mensagem;
    private final int linha;
    private final int coluna;

    public ErrorLexico(String mensagem, int linha, int coluna) {
        this.mensagem = mensagem;
        this.linha = linha;
        this.coluna = coluna;
    }

    public String mensagem() {
        return mensagem;
    }

    public int linha() {
        return linha;
    }

    public int coluna() {
        return coluna;
    }

    @Override
    public String toString() {
        return "Erro léxico em " + linha + ":" + coluna + ": " + mensagem;
    }
}
