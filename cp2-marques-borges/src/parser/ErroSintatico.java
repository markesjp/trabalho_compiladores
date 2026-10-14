package parser;

public final class ErroSintatico {
    private final String mensagem;
    private final int linha;
    private final int coluna;

    public ErroSintatico(String mensagem, int linha, int coluna) {
        this.mensagem = mensagem;
        this.linha = linha;
        this.coluna = coluna;
    }

    public String mensagem() { return mensagem; }
    public int linha() { return linha; }
    public int coluna() { return coluna; }

    @Override
    public String toString() {
        return "Erro sintático em " + linha + ":" + coluna + ": " + mensagem;
    }
}
