package parser;

import scanner.ErrorLexico;
import scanner.Scanner;
import scanner.TipoToken;
import scanner.Token;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Reconhecedor recursivo descendente, sem AST ou verificacao semantica. */
public final class Parser {
    private final Scanner scanner;
    private Token lookahead;
    private Token ultimoErro;
    private final List<ErroSintatico> erros = new ArrayList<>();
    private int consumidos;
    private boolean analisado;
    private boolean aceito;

    public Parser(Scanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        lookahead = scanner.nextToken();
    }

    public boolean parse() {
        if (!analisado) {
            parsePrograma();
            aceito = erros.isEmpty() && scanner.errors().isEmpty();
            analisado = true;
        }
        return aceito;
    }

    public List<ErroSintatico> erros() { return List.copyOf(erros); }
    public List<ErrorLexico> errosLexicos() { return List.copyOf(scanner.errors()); }

    // Adaptacao das categorias genericas do CP1, sem alterar Token ou Scanner.
    static String terminal(Token token) {
        return switch (token.tipo()) {
            case IDENTIFIER -> "id";
            case INTEGER_LITERAL -> "inteiro";
            case DOUBLE_LITERAL -> "decimal";
            case CHAR_LITERAL -> "caractere";
            case STRING_LITERAL -> "texto";
            case EOF -> "EOF";
            case KEYWORD, OPERATOR, DELIMITER -> token.lexema();
        };
    }

    private boolean em(String terminal) { return terminal(lookahead).equals(terminal); }
    private boolean first(String nt) { return Conjuntos.first(nt).contains(terminal(lookahead)); }
    private boolean follow(String nt) { return Conjuntos.follow(nt).contains(terminal(lookahead)); }

    private void avancar() {
        if (lookahead.tipo() != TipoToken.EOF) {
            consumidos++;
            lookahead = scanner.nextToken();
        }
    }

    // Aula 6: match verifica um terminal e atualiza lookahead.
    private boolean match(String esperado, String nt, String... ancoras) {
        if (em(esperado)) {
            avancar();
            return true;
        }
        error(nt, "esperava '" + esperado + "'");
        Set<String> sync = sync(nt, ancoras);
        sync.add(esperado);
        sincronizar(sync);
        if (em(esperado)) {
            avancar(); // terminal recuperado; o erro continua registrado
            return true;
        }
        return false;
    }

    private void error(String nt, String mensagem) {
        // Ao devolver por varios niveis, nao repetir o mesmo erro na mesma posicao.
        if (ultimoErro == lookahead) return;
        ultimoErro = lookahead;
        String encontrado = em("EOF") ? "EOF" : "'" + lookahead.lexema()
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "'";
        erros.add(new ErroSintatico(nt + ": " + mensagem + ", encontrou " + encontrado,
                lookahead.linha(), lookahead.coluna()));
    }

    private Set<String> sync(String nt, String... ancoras) {
        Set<String> resultado = new HashSet<>(Conjuntos.follow(nt));
        resultado.addAll(Set.of(";", "}", "EOF"));
        // Fronteiras de comandos preservam a proxima linha quando faltar um ';'.
        resultado.addAll(Conjuntos.first("Comando"));
        resultado.add("fn");
        resultado.addAll(Set.of(ancoras));
        return resultado;
    }

    // Aula 6: preserva o token sincronizador para o chamador.
    private void sincronizar(Set<String> sync) {
        while (!sync.contains(terminal(lookahead)) && !em("EOF")) {
            avancar();
        }
    }

    private void inesperado(String nt) {
        error(nt, "token fora de FIRST/FOLLOW");
        sincronizar(sync(nt));
    }

    private void epsilon(String nt) {
        if (!follow(nt)) {
            inesperado(nt); // caminho de erro, nao uma escolha valida de epsilon
        }
    }

    private void parsePrograma() {
        if (!first("Programa")) {
            error("Programa", "esperava ao menos uma declaracao");
            sincronizar(sync("Declaracao"));
        }
        if (first("Declaracao")) {
            parseDeclaracao();
        }
        parseListaDeclaracoes();
        match("EOF", "Programa");
    }

    private void parseListaDeclaracoes() {
        while (!follow("ListaDeclaracoes")) {
            int antes = consumidos;
            if (first("Declaracao")) {
                parseDeclaracao();
            } else {
                error("ListaDeclaracoes", "esperava declaracao ou EOF");
                sincronizar(sync("Declaracao"));
                if (em(";") || em("}")) avancar();
            }
            if (consumidos == antes && !em("EOF")) avancar();
        }
        epsilon("ListaDeclaracoes");
    }

    private void parseDeclaracao() {
        if (first("Funcao")) parseFuncao();
        else if (first("Variavel")) parseVariavel();
        else inesperado("Declaracao");
    }

    private void parseFuncao() {
        if (!match("fn", "Funcao")) return;
        parseTipoRetorno();
        if (!match("id", "Funcao", "(")) return;
        if (!match("(", "Funcao", ")", "{")) return;
        parseParametrosOpt();
        match(")", "Funcao", "{");
        parseBloco();
    }

    private void parseTipoRetorno() {
        if (em("void")) match("void", "TipoRetorno");
        else if (first("Tipo")) parseTipo();
        else inesperado("TipoRetorno");
    }

    private void parseTipo() {
        if (first("Tipo")) match(terminal(lookahead), "Tipo");
        else inesperado("Tipo");
    }

    private void parseParametrosOpt() {
        if (first("ParametrosOpt")) {
            parseParametro();
            parseMaisParametros();
        } else epsilon("ParametrosOpt");
    }

    private void parseParametro() {
        parseTipo();
        match("id", "Parametro");
    }

    private void parseMaisParametros() {
        while (first("MaisParametros")) {
            match(",", "MaisParametros");
            parseParametro();
        }
        epsilon("MaisParametros");
    }

    private void parseVariavel() {
        parseTipo();
        if (!match("id", "Variavel", "=")) return;
        parseInicializacaoOpt();
        match(";", "Variavel");
    }

    private void parseInicializacaoOpt() {
        if (first("InicializacaoOpt")) {
            match("=", "InicializacaoOpt");
            parseExpr();
        } else epsilon("InicializacaoOpt");
    }

    private void parseBloco() {
        if (!match("{", "Bloco")) return;
        parseListaComandos();
        match("}", "Bloco");
    }

    private void parseListaComandos() {
        while (!follow("ListaComandos")) {
            // EOF e fn permitem devolver o bloco incompleto sem engolir outra funcao.
            if (em("EOF") || em("fn")) {
                error("ListaComandos", "esperava comando ou '}'");
                sincronizar(sync("Bloco"));
                return;
            }
            int antes = consumidos;
            if (first("Comando")) {
                parseComando();
            } else {
                error("ListaComandos", "esperava comando ou '}'");
                sincronizar(sync("Comando"));
                if (em(";")) avancar();
            }
            // Um caminho de erro que devolveu a mesma ancora nao repete para sempre.
            if (consumidos == antes && !follow("ListaComandos") && !em("EOF")) avancar();
        }
        epsilon("ListaComandos");
    }

    private void parseComando() {
        if (first("Variavel")) parseVariavel();
        else if (first("Se")) parseSe();
        else if (first("Enquanto")) parseEnquanto();
        else if (first("Retorno")) parseRetorno();
        else if (first("ComandoId")) parseComandoId();
        else if (first("Bloco")) parseBloco();
        else inesperado("Comando");
    }

    private void parseSe() {
        match("if", "Se");
        if (!match("(", "Se", "{")) return;
        parseExpr();
        match(")", "Se", "{");
        parseBloco();
        parseSenaoOpt();
    }

    private void parseSenaoOpt() {
        if (first("SenaoOpt")) {
            match("else", "SenaoOpt");
            parseBloco();
        } else epsilon("SenaoOpt");
    }

    private void parseEnquanto() {
        match("while", "Enquanto");
        if (!match("(", "Enquanto", "{")) return;
        parseExpr();
        match(")", "Enquanto", "{");
        parseBloco();
    }

    private void parseRetorno() {
        match("return", "Retorno");
        parseRetornoOpt();
        match(";", "Retorno");
    }

    private void parseRetornoOpt() {
        if (first("RetornoOpt")) parseExpr();
        else epsilon("RetornoOpt");
    }

    private void parseComandoId() {
        match("id", "ComandoId");
        parseComandoIdResto();
    }

    private void parseComandoIdResto() {
        if (em("=")) {
            match("=", "ComandoIdResto");
            parseExpr();
            match(";", "ComandoIdResto");
        } else if (em("(")) {
            match("(", "ComandoIdResto");
            parseArgumentosOpt();
            match(")", "ComandoIdResto", ";");
            match(";", "ComandoIdResto");
        } else inesperado("ComandoIdResto");
    }

    private void parseArgumentosOpt() {
        if (first("ArgumentosOpt")) {
            parseExpr();
            parseMaisArgumentos();
        } else epsilon("ArgumentosOpt");
    }

    private void parseMaisArgumentos() {
        while (first("MaisArgumentos")) {
            match(",", "MaisArgumentos");
            parseExpr();
        }
        epsilon("MaisArgumentos");
    }

    private void parseExpr() { parseConjuncao(); parseExprPrime(); }

    private void parseExprPrime() {
        while (first("ExprPrime")) {
            match("||", "ExprPrime");
            parseConjuncao();
        }
        epsilon("ExprPrime");
    }

    private void parseConjuncao() { parseIgualdade(); parseConjuncaoPrime(); }

    private void parseConjuncaoPrime() {
        while (first("ConjuncaoPrime")) {
            match("&&", "ConjuncaoPrime");
            parseIgualdade();
        }
        epsilon("ConjuncaoPrime");
    }

    private void parseIgualdade() { parseRelacao(); parseIgualdadePrime(); }

    private void parseIgualdadePrime() {
        while (first("IgualdadePrime")) {
            match(terminal(lookahead), "IgualdadePrime");
            parseRelacao();
        }
        epsilon("IgualdadePrime");
    }

    private void parseRelacao() { parseE(); parseRelacaoPrime(); }

    private void parseRelacaoPrime() {
        while (first("RelacaoPrime")) {
            match(terminal(lookahead), "RelacaoPrime");
            parseE();
        }
        epsilon("RelacaoPrime");
    }

    // E -> T EPrime, T -> Unaria TPrime: estrutura dos exemplos da Aula 6.
    private void parseE() { parseT(); parseEPrime(); }

    private void parseEPrime() {
        while (first("EPrime")) {
            match(terminal(lookahead), "EPrime");
            parseT();
        }
        epsilon("EPrime");
    }

    private void parseT() { parseUnaria(); parseTPrime(); }

    private void parseTPrime() {
        while (first("TPrime")) {
            match(terminal(lookahead), "TPrime");
            parseUnaria();
        }
        epsilon("TPrime");
    }

    private void parseUnaria() {
        if (em("!") || em("-")) {
            match(terminal(lookahead), "Unaria");
            parseUnaria();
        } else if (first("F")) parseF();
        else inesperado("Unaria");
    }

    private void parseF() {
        if (em("(")) {
            match("(", "F");
            parseExpr();
            match(")", "F");
        } else if (em("id")) {
            match("id", "F");
            parseChamadaOpt();
        } else if (first("F")) {
            match(terminal(lookahead), "F");
        } else inesperado("F");
    }

    private void parseChamadaOpt() {
        if (first("ChamadaOpt")) {
            match("(", "ChamadaOpt");
            parseArgumentosOpt();
            match(")", "ChamadaOpt");
        } else epsilon("ChamadaOpt");
    }
}
