import parser.ErroSintatico;
import parser.Parser;
import scanner.Scanner;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Testes sem bibliotecas externas; falha por AssertionError, mesmo sem -ea. */
public final class ParserTest {
    private static int testes;
    private ParserTest() { }

    public static void main(String[] args) throws Exception {
        Path raiz = args.length == 0 ? Path.of(".") : Path.of(args[0]);
        validarArquivo(raiz, "test/validos/01-cp1.lumen", true);
        validarArquivo(raiz, "test/validos/02-controle.lumen", true);
        validarArquivo(raiz, "test/validos/03-funcoes.lumen", true);
        Parser semPonto = validarArquivo(raiz, "test/invalidos/01-sem-ponto-virgula.lumen", false);
        conferir(semPonto.erros().get(0).linha() == 3 && semPonto.erros().get(0).coluna() == 1,
                "posicao da ausencia de ;");
        validarArquivo(raiz, "test/invalidos/02-sem-operando.lumen", false);
        validarArquivo(raiz, "test/invalidos/03-sem-parentese.lumen", false);
        Parser varios = validarArquivo(raiz, "test/invalidos/04-multiplos-erros.lumen", false);
        Set<Integer> linhas = new HashSet<>();
        for (ErroSintatico erro : varios.erros()) linhas.add(erro.linha());
        conferir(linhas.equals(Set.of(2, 4)), "recuperacao deve encontrar os dois erros independentes: " + varios.erros());
        conferir(varios.erros().size() == 2, "sem erros em cascata no exemplo multiplo");
        System.out.println("Traco de recuperacao:");
        varios.erros().forEach(System.out::println);

        Parser fronteira = verificar("fn void f() {\n int x = 1\n int y = ;\n return;\n}", false,
                "falta ; nao pode descartar o proximo comando errado");
        conferir(fronteira.erros().size() == 2, "dois erros apos fronteira: " + fronteira.erros());
        conferir(fronteira.erros().get(0).linha() == 3 && fronteira.erros().get(0).coluna() == 2,
                "inicio do proximo comando");
        conferir(fronteira.erros().get(1).linha() == 3 && fronteira.erros().get(1).coluna() == 10,
                "operando ausente no proximo comando");
        Parser outraFuncao = verificar("fn void f() {\n return;\nfn int g() {\n return +;\n}", false,
                "falta } nao pode engolir a proxima funcao");
        conferir(outraFuncao.erros().stream().anyMatch(e -> e.linha() == 4), "outra funcao tambem analisada");

        aceitar("int global; fn void f() {}", "global e bloco vazio");
        aceitar("fn void f(int a, double b, bool c, char d, string e) { f(1,2.0,true,'x',\"y\"); return; }",
                "todos os parametros, chamada e retorno vazio");
        aceitar("fn int f() { return g(g(1), h()); }", "chamadas aninhadas");
        aceitar("fn void f() { bool a = !false || true && false; int b = -1 + 2 * 3 % 2; }", "precedencia e unarios");
        aceitar("fn void f() { int x = 1 < 2 <= 3 > 0 >= 0 == 1 != 2; }", "todos os relacionais e igualdade");
        aceitar("fn void f() { bool a = true; if(a) {} int x; while(a) {} return; }", "epsilon seguido por comando");
        aceitar("/* inicio */ fn void f() { // linha\r\n return; }", "comentarios e CRLF");
        aceitar("fn void f() { int x = \"texto\"; naoDeclarada(1); return 2; }", "sintaxe nao verifica semantica");

        rejeitar("", "programa vazio");
        rejeitar("fn void f() { ; }", "comando vazio nao definido");
        rejeitar("fn void f() { int ; }", "nome ausente");
        rejeitar("fn void f(int) {}", "parametro sem nome");
        rejeitar("fn void f(int a,) {}", "virgula final parametro");
        rejeitar("fn void f() { g(1,); }", "virgula final argumento");
        rejeitar("fn void f() { g(1 2); }", "virgula ausente argumento");
        rejeitar("fn void f() { int x = 1 2; }", "epsilon fora de FOLLOW");
        rejeitar("fn void f() { int x = 1 = 2; }", "atribuicao nao e expressao");
        rejeitar("fn void f() { int x = +1; }", "unario mais nao definido");
        rejeitar("fn void f() { if(true) return; }", "if exige bloco");
        rejeitar("fn void f() { while(true) return; }", "while exige bloco");
        rejeitar("fn void f() { else {} }", "else isolado");
        rejeitar("fn void f() { int x[2]; }", "array fora do escopo");
        rejeitar("fn void f() { void x; }", "void nao e tipo de variavel");
        rejeitar("fn void f() { fn int g() { return 1; } }", "funcao aninhada");
        rejeitar("fn void f() {", "EOF em bloco");
        rejeitar("fn void f() { int x = (1 + 2; }", "parentese interno ausente");
        rejeitar("int a; }", "sobra depois do programa");
        rejeitar("fn void f() { return", "EOF em expressao opcional");
        rejeitar("fn void f() { int x = 1; @ }", "erro lexico nao pode aceitar");
        rejeitar("fn void f() { string x = \"\\q\"; }", "scanner emite string com erro de escape");
        rejeitar("fn void f() {} /* aberto", "erro lexico apos ultimo bloco");

        StringBuilder muitos = new StringBuilder("fn void f() { int x = 0");
        for (int i = 0; i < 2000; i++) muitos.append("+1");
        muitos.append(';');
        for (int i = 0; i < 2000; i++) muitos.append("x = x + 1;");
        muitos.append('}');
        aceitar(muitos.toString(), "listas e caudas longas usam lacos");
        System.out.println("Parser: " + testes + " casos passaram.");
    }

    private static Parser validarArquivo(Path raiz, String nome, boolean esperado) throws Exception {
        return verificar(Files.readString(raiz.resolve(nome), StandardCharsets.UTF_8), esperado, nome);
    }

    private static void aceitar(String fonte, String nome) { verificar(fonte, true, nome); }
    private static void rejeitar(String fonte, String nome) { verificar(fonte, false, nome); }

    private static Parser verificar(String fonte, boolean esperado, String nome) {
        Parser parser = new Parser(new Scanner(fonte));
        boolean resultado = parser.parse();
        conferir(resultado == esperado, nome + ": " + parser.erros() + " " + parser.errosLexicos());
        conferir(parser.parse() == resultado, "parse repetido deve devolver o mesmo resultado");
        if (!esperado) conferir(!parser.erros().isEmpty() || !parser.errosLexicos().isEmpty(), "rejeicao com diagnostico");
        for (ErroSintatico erro : parser.erros()) {
            conferir(erro.linha() >= 1 && erro.coluna() >= 1, "posicao positiva");
        }
        testes++;
        return parser;
    }

    private static void conferir(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }
}
