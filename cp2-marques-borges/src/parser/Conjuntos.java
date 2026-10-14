package parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** FIRST sem epsilon e FOLLOW da BNF de gramatica-first-follow.md. */
final class Conjuntos {
    private static final Map<String, Set<String>> FIRST = new HashMap<>();
    private static final Map<String, Set<String>> FOLLOW = new HashMap<>();

    static {
        Set<String> tipo = Set.of("int", "double", "bool", "char", "string");
        Set<String> declaracao = unir(tipo, "fn");
        Set<String> comando = unir(tipo, "if", "while", "return", "id", "{");
        Set<String> depoisDeclaracao = unir(declaracao, "EOF");
        Set<String> depoisComando = unir(comando, "}");
        Set<String> fator = Set.of("(", "id", "inteiro", "decimal", "caractere", "texto", "true", "false");
        Set<String> expressao = unir(fator, "!", "-");
        Set<String> depoisExpr = Set.of(")", ",", ";");
        Set<String> depoisConjuncao = unir(depoisExpr, "||");
        Set<String> depoisIgualdade = unir(depoisConjuncao, "&&");
        Set<String> depoisRelacao = unir(depoisIgualdade, "==", "!=");
        Set<String> depoisE = unir(depoisRelacao, "<", "<=", ">", ">=");
        Set<String> depoisT = unir(depoisE, "+", "-");
        Set<String> depoisF = unir(depoisT, "*", "/", "%");

        definir("Programa", declaracao, Set.of("EOF")); // $ teorico mapeado para EOF
        definir("ListaDeclaracoes", declaracao, Set.of("EOF"));
        definir("Declaracao", declaracao, depoisDeclaracao);
        definir("Funcao", Set.of("fn"), depoisDeclaracao);
        definir("TipoRetorno", unir(tipo, "void"), Set.of("id"));
        definir("Tipo", tipo, Set.of("id"));
        definir("ParametrosOpt", tipo, Set.of(")"));
        definir("Parametro", tipo, Set.of(",", ")"));
        definir("MaisParametros", Set.of(","), Set.of(")"));
        definir("Variavel", tipo, unir(depoisComando, "fn", "EOF"));
        definir("InicializacaoOpt", Set.of("="), Set.of(";"));
        definir("Bloco", Set.of("{"), unir(depoisComando, "else", "fn", "EOF"));
        definir("ListaComandos", comando, Set.of("}"));
        definir("Comando", comando, depoisComando);
        definir("Se", Set.of("if"), depoisComando);
        definir("SenaoOpt", Set.of("else"), depoisComando);
        definir("Enquanto", Set.of("while"), depoisComando);
        definir("Retorno", Set.of("return"), depoisComando);
        definir("RetornoOpt", expressao, Set.of(";"));
        definir("ComandoId", Set.of("id"), depoisComando);
        definir("ComandoIdResto", Set.of("=", "("), depoisComando);
        definir("ArgumentosOpt", expressao, Set.of(")"));
        definir("MaisArgumentos", Set.of(","), Set.of(")"));
        definir("Expr", expressao, depoisExpr);
        definir("ExprPrime", Set.of("||"), depoisExpr);
        definir("Conjuncao", expressao, depoisConjuncao);
        definir("ConjuncaoPrime", Set.of("&&"), depoisConjuncao);
        definir("Igualdade", expressao, depoisIgualdade);
        definir("IgualdadePrime", Set.of("==", "!="), depoisIgualdade);
        definir("Relacao", expressao, depoisRelacao);
        definir("RelacaoPrime", Set.of("<", "<=", ">", ">="), depoisRelacao);
        definir("E", expressao, depoisE);
        definir("EPrime", Set.of("+", "-"), depoisE);
        definir("T", expressao, depoisT);
        definir("TPrime", Set.of("*", "/", "%"), depoisT);
        definir("Unaria", expressao, depoisF);
        definir("F", fator, depoisF);
        definir("ChamadaOpt", Set.of("("), depoisF);
    }

    private Conjuntos() { }

    private static void definir(String nt, Set<String> first, Set<String> follow) {
        FIRST.put(nt, first);
        FOLLOW.put(nt, follow);
    }

    private static Set<String> unir(Set<String> base, String... outros) {
        Set<String> resultado = new HashSet<>(base);
        resultado.addAll(Set.of(outros));
        return Set.copyOf(resultado);
    }

    static Set<String> first(String nt) { return FIRST.get(nt); }
    static Set<String> follow(String nt) { return FOLLOW.get(nt); }
}
