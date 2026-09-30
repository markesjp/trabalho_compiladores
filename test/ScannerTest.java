import scanner.CategoriaToken;
import scanner.ErrorLexico;
import scanner.Scanner;
import scanner.TipoToken;
import scanner.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ScannerTest {
    private static int passed;

    private ScannerTest() {
    }

    public static void main(String[] args) {
        run("cinco categorias e tipos específicos", ScannerTest::testCategoriasValidas);
        run("todos os operadores e maximal munch", ScannerTest::testMaximalMunch);
        run("todos os delimitadores", ScannerTest::testDelimitadores);
        run("tabela completa de palavras reservadas", ScannerTest::testPalavrasReservadas);
        run("case sensitivity e prefixos de keywords", ScannerTest::testIdentificadores);
        run("lookahead não consome o próximo token", ScannerTest::testLookahead);
        run("comentários entre tokens", ScannerTest::testComentarios);
        run("comentário de linha termina em EOF", ScannerTest::testComentarioLinhaEof);
        run("comentários de bloco não aninhados", ScannerTest::testComentarioNaoAninhado);
        run("trecho realista com funções e controle", ScannerTest::testTrechoRealista);
        run("caractere inválido e posição do erro", ScannerTest::testRecuperacao);
        run("string não fechada até fim de linha", ScannerTest::testStringLinha);
        run("string não fechada até EOF", ScannerTest::testStringEof);
        run("string terminada em barra invertida", ScannerTest::testStringBarraEof);
        run("strings válidas e lexema original", ScannerTest::testStringsValidas);
        run("escape inválido descarta a string inteira", ScannerTest::testStringEscapeInvalido);
        run("recuperação de string respeita aspas escapadas", ScannerTest::testStringRecuperacao);
        run("alfabeto inválido dentro de string", ScannerTest::testStringAlfabeto);
        run("caractere fora do alfabeto entre tokens", ScannerTest::testAlfabeto);
        run("todos os escapes de char", ScannerTest::testCharValido);
        run("chars vazios, longos e escapes inválidos", ScannerTest::testCharInvalido);
        run("char não fechado em linha e EOF", ScannerTest::testCharNaoFechado);
        run("recuperação de char respeita escape", ScannerTest::testCharRecuperacao);
        run("comentário de bloco não fechado e posição", ScannerTest::testComentarioEof);
        run("alfabeto inválido em comentários", ScannerTest::testComentarioAlfabeto);
        run("linhas CRLF, LF, CR e coluna de tab", ScannerTest::testPosicoes);
        run("números e decisão sobre ponto incompleto", ScannerTest::testNumeros);
        run("operadores incompletos e recuperação", ScannerTest::testOperadoresInvalidos);
        run("entrada vazia, trivia e EOF repetível", ScannerTest::testEof);
        run("hasNext consulta caracteres, não tokens", ScannerTest::testHasNext);
        run("erros não podem ser removidos pela API", ScannerTest::testErrorsReadOnly);
        run("sequências longas sem recursão", ScannerTest::testSequenciasLongas);
        run("trace mostra estados sem mudar tokens", ScannerTest::testTrace);
        run("integridade e impressão da classe Token", ScannerTest::testToken);
        run("exploração determinística de 2000 entradas", ScannerTest::testExploracao);
        System.out.println("Todos os " + passed + " testes passaram.");
    }

    private static void run(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("OK - " + name);
    }

    private static void testCategoriasValidas() {
        Scanner scanner = new Scanner("int total = 10; double media = 3.14; string nome = \"Ana\";");
        List<Token> t = read(scanner);
        assertTypes(t, TipoToken.INT, TipoToken.ID, TipoToken.ASSIGN, TipoToken.INT_LIT, TipoToken.SEMI,
                TipoToken.DOUBLE, TipoToken.ID, TipoToken.ASSIGN, TipoToken.DOUBLE_LIT, TipoToken.SEMI,
                TipoToken.STRING, TipoToken.ID, TipoToken.ASSIGN, TipoToken.STRING_LIT, TipoToken.SEMI);
        equal(CategoriaToken.KEYWORD, t.get(0).categoria(), "categoria keyword");
        equal(CategoriaToken.IDENTIFIER, t.get(1).categoria(), "categoria ID");
        equal(CategoriaToken.OPERATOR, t.get(2).categoria(), "categoria operador");
        equal(CategoriaToken.INTEGER_LITERAL, t.get(3).categoria(), "categoria inteiro");
        equal(CategoriaToken.STRING_LITERAL, t.get(13).categoria(), "categoria string");
        equal(0, scanner.errors().size(), "fonte válida");
    }

    private static void testMaximalMunch() {
        Scanner s = new Scanner("== != <= >= && || = ! < > + - * / %");
        List<Token> t = read(s);
        assertTypes(t, TipoToken.EQEQ, TipoToken.NE, TipoToken.LE, TipoToken.GE, TipoToken.AND, TipoToken.OR,
                TipoToken.ASSIGN, TipoToken.NOT, TipoToken.LT, TipoToken.GT, TipoToken.PLUS, TipoToken.MINUS,
                TipoToken.STAR, TipoToken.SLASH, TipoToken.MOD);
        assertLexemes(t, "==", "!=", "<=", ">=", "&&", "||", "=", "!", "<", ">", "+", "-", "*", "/", "%");
        equal(0, s.errors().size(), "operadores válidos");
    }

    private static void testDelimitadores() {
        assertTypes(read(new Scanner("(){}[],;:")), TipoToken.LPAREN, TipoToken.RPAREN, TipoToken.LBRACE,
                TipoToken.RBRACE, TipoToken.LBRACKET, TipoToken.RBRACKET, TipoToken.COMMA, TipoToken.SEMI, TipoToken.COLON);
    }

    private static void testPalavrasReservadas() {
        assertTypes(read(new Scanner("int double bool char string void if else while fn return true false")),
                TipoToken.INT, TipoToken.DOUBLE, TipoToken.BOOL, TipoToken.CHAR, TipoToken.STRING, TipoToken.VOID,
                TipoToken.IF, TipoToken.ELSE, TipoToken.WHILE, TipoToken.FN, TipoToken.RETURN, TipoToken.TRUE, TipoToken.FALSE);
    }

    private static void testIdentificadores() {
        List<Token> t = read(new Scanner("while1 ifelse Int INT _ _aux total2 returnValue"));
        equal(8, t.size(), "maximal munch para identificadores");
        for (Token token : t) {
            equal(TipoToken.ID, token.tipo(), "keyword só após lexema completo");
        }
    }

    private static void testLookahead() {
        List<Token> t = read(new Scanner("x=y==z+1;!a>=2"));
        assertLexemes(t, "x", "=", "y", "==", "z", "+", "1", ";", "!", "a", ">=", "2");
        position(t.get(2), 1, 3);
        position(t.get(4), 1, 6);
    }

    private static void testComentarios() {
        Scanner s = new Scanner("/* inicio */ fn int f() { // linha\n return 1; }");
        assertLexemes(read(s), "fn", "int", "f", "(", ")", "{", "return", "1", ";", "}");
        equal(0, s.errors().size(), "comentários válidos");
    }

    private static void testComentarioLinhaEof() {
        Scanner s = new Scanner("a//fim");
        assertLexemes(read(s), "a");
        equal(0, s.errors().size(), "comentário de linha no EOF");
    }

    private static void testComentarioNaoAninhado() {
        Scanner s = new Scanner("/* externo /* interno */ x */");
        assertLexemes(read(s), "x", "*", "/");
        equal(0, s.errors().size(), "primeiro fechamento encerra o comentário");
    }

    private static void testTrechoRealista() {
        String source = "fn int soma(int a,int b){return a+b;} fn void main(){"
                + "int total=soma(10,20); /* nota */ bool pronto=true;\n"
                + "if(pronto&&total>=30){while(total>0){total=total-1;}}else{return;}}";
        Scanner s = new Scanner(source);
        List<Token> t = read(s);
        assertLexemes(t, "fn", "int", "soma", "(", "int", "a", ",", "int", "b", ")", "{", "return",
                "a", "+", "b", ";", "}", "fn", "void", "main", "(", ")", "{",
                "int", "total", "=", "soma", "(", "10", ",", "20", ")", ";", "bool", "pronto", "=", "true", ";",
                "if", "(", "pronto", "&&", "total", ">=", "30", ")", "{", "while", "(", "total", ">", "0", ")",
                "{", "total", "=", "total", "-", "1", ";", "}", "}", "else", "{", "return", ";", "}", "}");
        equal(TipoToken.IF, t.get(38).tipo(), "tipo usado no futuro parser");
        equal(0, s.errors().size(), "trecho realista");
    }

    private static void testRecuperacao() {
        Scanner s = new Scanner("int x = 10 @ y = 20;");
        List<Token> t = read(s);
        equal("y", t.get(4).lexema(), "token após erro");
        errors(s, 1, "não reconhecido");
        position(s.errors().get(0), 1, 12);
    }

    private static void testStringLinha() {
        Scanner s = new Scanner("string a = \"sem fim\nint b = 2;");
        List<Token> t = read(s);
        equal(TipoToken.INT, t.get(3).tipo(), "token recuperado");
        position(t.get(3), 2, 1);
        errors(s, 1, "fim da linha");
        position(s.errors().get(0), 1, 12);
    }

    private static void testStringEof() {
        Scanner s = new Scanner("\"sem fim");
        equal(0, read(s).size(), "não emitir string incompleta");
        errors(s, 1, "EOF");
        position(s.errors().get(0), 1, 1);
    }

    private static void testStringBarraEof() {
        Scanner s = new Scanner("\"abc\\");
        equal(0, read(s).size(), "não emitir escape incompleto");
        errors(s, 1, "EOF");
    }

    private static void testStringsValidas() {
        String source = "\"\" \"ok\" \"a\\n\\r\\t\\\\\\\"b\" \"a\tb\" \"// /* */\"";
        Scanner s = new Scanner(source);
        List<Token> t = read(s);
        assertLexemes(t, "\"\"", "\"ok\"", "\"a\\n\\r\\t\\\\\\\"b\"", "\"a\tb\"", "\"// /* */\"");
        for (Token token : t) {
            equal(TipoToken.STRING_LIT, token.tipo(), "strings válidas");
        }
        equal(0, s.errors().size(), "escapes permitidos e tab original");
    }

    private static void testStringEscapeInvalido() {
        Scanner s = new Scanner("\"a\\q\" int x;");
        assertLexemes(read(s), "int", "x", ";");
        errors(s, 1, "escape inválido");
        position(s.errors().get(0), 1, 3);
    }

    private static void testStringRecuperacao() {
        Scanner s = new Scanner("\"a\\q\\\"b\" total");
        assertLexemes(read(s), "total");
        errors(s, 1, "escape inválido");
    }

    private static void testStringAlfabeto() {
        Scanner s = new Scanner("\"a§b\" total");
        assertLexemes(read(s), "total");
        errors(s, 1, "fora do alfabeto");
        position(s.errors().get(0), 1, 3);
    }

    private static void testAlfabeto() {
        Scanner s = new Scanner("int x=1; § int y=2;");
        assertLexemes(read(s), "int", "x", "=", "1", ";", "int", "y", "=", "2", ";");
        errors(s, 1, "fora do alfabeto");
    }

    private static void testCharValido() {
        Scanner s = new Scanner("'L' ' ' '\\n' '\\r' '\\t' '\\\\' '\\'' '\\\"'");
        List<Token> t = read(s);
        equal(8, t.size(), "char e todos os escapes");
        for (Token token : t) {
            equal(TipoToken.CHAR_LIT, token.tipo(), "char válido");
        }
        equal("'\\n'", t.get(2).lexema(), "escape original");
        equal(0, s.errors().size(), "char válido");
    }

    private static void testCharInvalido() {
        Scanner s = new Scanner("'' 'ab' '\\q' '§' '\t' total");
        assertLexemes(read(s), "total");
        equal(5, s.errors().size(), "um erro por char inválido");
    }

    private static void testCharNaoFechado() {
        Scanner s = new Scanner("'a\nint x; '");
        assertLexemes(read(s), "int", "x", ";");
        equal(2, s.errors().size(), "char sem fechamento em linha e EOF");
        position(s.errors().get(1), 2, 8);
    }

    private static void testCharRecuperacao() {
        Scanner s = new Scanner("'ab\\'c' total");
        assertLexemes(read(s), "total");
        errors(s, 1, "mais de um");
    }

    private static void testComentarioEof() {
        Scanner s = new Scanner("x;\n  /* aberto");
        assertLexemes(read(s), "x", ";");
        errors(s, 1, "comentário de bloco");
        position(s.errors().get(0), 2, 3);
    }

    private static void testComentarioAlfabeto() {
        Scanner s = new Scanner("// §\n/* § */ int");
        assertLexemes(read(s), "int");
        equal(2, s.errors().size(), "ASCII também nos comentários");
    }

    private static void testPosicoes() {
        Scanner s = new Scanner("a\r\n\tb\n c\rd");
        List<Token> t = read(s);
        position(t.get(0), 1, 1);
        position(t.get(1), 2, 2);
        position(t.get(2), 3, 2);
        position(t.get(3), 4, 1);
        position(s.nextToken(), 4, 2);
        Scanner broken = new Scanner("\"ab\r\nint");
        position(read(broken).get(0), 2, 1);
        equal(1, broken.errors().size(), "recuperação com CRLF");
    }

    private static void testNumeros() {
        Scanner s = new Scanner("0 001 3.14 -2 3. .5 1.2.3 12abc 1e3");
        List<Token> t = read(s);
        assertLexemes(t, "0", "001", "3.14", "-", "2", "3", "5", "1.2", "3", "12", "abc", "1", "e3");
        equal(TipoToken.DOUBLE_LIT, t.get(2).tipo(), "decimal");
        equal(TipoToken.MINUS, t.get(3).tipo(), "sinal não pertence ao literal");
        equal(3, s.errors().size(), "ponto só é válido entre dígitos");
    }

    private static void testOperadoresInvalidos() {
        Scanner s = new Scanner("&x |y &&& z");
        assertLexemes(read(s), "x", "y", "&&", "z");
        equal(3, s.errors().size(), "operadores incompletos");
        position(s.errors().get(0), 1, 1);
        position(s.errors().get(2), 1, 9);
    }

    private static void testEof() {
        for (String source : List.of("", " \t\r\n//fim", "/* nota */")) {
            Scanner s = new Scanner(source);
            Token first = s.nextToken();
            Token second = s.nextToken();
            equal(TipoToken.EOF, first.tipo(), "EOF explícito");
            equal("", first.lexema(), "lexema EOF vazio");
            equal(first.toString(), second.toString(), "EOF idempotente");
            equal(false, s.hasNext(), "cursor no EOF");
            equal(0, s.errors().size(), "trivia válida");
        }
    }

    private static void testHasNext() {
        Scanner s = new Scanner(" ");
        equal(true, s.hasNext(), "há um caractere antes de consumir");
        equal(TipoToken.EOF, s.nextToken().tipo(), "não há token no espaço");
        equal(false, s.hasNext(), "fim após ignorar espaço");
    }

    private static void testErrorsReadOnly() {
        Scanner s = new Scanner("@");
        read(s);
        try {
            s.errors().clear();
            throw new AssertionError("lista de erros deve ser somente leitura");
        } catch (UnsupportedOperationException expected) {
            equal(1, s.errors().size(), "erro preservado");
        }
    }

    private static void testSequenciasLongas() {
        Scanner broken = new Scanner("@".repeat(20000) + " total");
        assertLexemes(read(broken), "total");
        equal(20000, broken.errors().size(), "recuperação sem pilha recursiva");
        String id = "a".repeat(20000);
        assertLexemes(read(new Scanner(id)), id);
    }

    private static void testTrace() {
        List<String> trace = new ArrayList<>();
        String source = "while1 = \"ok\\n\";";
        List<Token> traced = read(new Scanner(source, trace::add));
        List<Token> plain = read(new Scanner(source));
        equal(plain.toString(), traced.toString(), "trace não altera resultado");
        check(trace.stream().anyMatch(s -> s.startsWith("ID START")), "estado inicial de ID");
        check(trace.stream().anyMatch(s -> s.startsWith("ID ACCEPT_IDENTIFIER")), "estado final de ID");
        check(trace.stream().anyMatch(s -> s.startsWith("STRING ESCAPE")), "escape de string");
        check(trace.stream().anyMatch(s -> s.startsWith("STRING ACCEPT_STRING")), "aceitação de string");
        check(trace.stream().anyMatch(s -> s.startsWith("OP OP_LOOKAHEAD")), "lookahead de operador");
    }

    private static void testToken() {
        Token t = new Token(TipoToken.STRING_LIT, "\"a\tb\"", 1, 2);
        equal("\"a\tb\"", t.lexema(), "não normalizar lexema armazenado");
        check(!t.toString().contains("\t"), "impressão deve escapar tab");
        try {
            new Token(TipoToken.ID, "x", 0, 1);
            throw new AssertionError("posição inválida deve ser rejeitada");
        } catch (IllegalArgumentException expected) {
            // O contrato de Token exige posições positivas.
        }
    }

    private static void testExploracao() {
        Random random = new Random(20260929L);
        String alphabet = "ab0_3. =!&|*/;\"'\\\r\n\t§\u0000";
        for (int sample = 0; sample < 2000; sample++) {
            StringBuilder source = new StringBuilder();
            int length = random.nextInt(80);
            for (int i = 0; i < length; i++) {
                source.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            List<String> trace = new ArrayList<>();
            Scanner traced = new Scanner(source.toString(), trace::add);
            Scanner plain = new Scanner(source.toString());
            List<Token> t = read(traced);
            equal(read(plain).toString(), t.toString(), "resultado com e sem trace na amostra " + sample);
            equal(plain.errors().toString(), traced.errors().toString(), "diagnósticos na amostra " + sample);
            int lastLine = 1;
            int lastColumn = 0;
            for (Token token : t) {
                check(!token.lexema().isEmpty(), "token não EOF deve ter lexema");
                check(token.linha() > lastLine || (token.linha() == lastLine && token.coluna() > lastColumn),
                        "posições devem avançar");
                lastLine = token.linha();
                lastColumn = token.coluna();
            }
            equal(traced.nextToken().toString(), traced.nextToken().toString(), "EOF repetível");
        }
    }

    private static List<Token> read(Scanner scanner) {
        List<Token> tokens = new ArrayList<>();
        for (int count = 0; count < 100000; count++) {
            Token token = scanner.nextToken();
            if (token.tipo() == TipoToken.EOF) {
                return tokens;
            }
            tokens.add(token);
        }
        throw new AssertionError("scanner emitiu tokens sem alcançar EOF");
    }

    private static void assertTypes(List<Token> tokens, TipoToken... expected) {
        equal(expected.length, tokens.size(), "quantidade de tokens");
        for (int i = 0; i < expected.length; i++) {
            equal(expected[i], tokens.get(i).tipo(), "tipo do token " + i);
        }
    }

    private static void assertLexemes(List<Token> tokens, String... expected) {
        equal(expected.length, tokens.size(), "quantidade de lexemas");
        for (int i = 0; i < expected.length; i++) {
            equal(expected[i], tokens.get(i).lexema(), "lexema " + i);
        }
    }

    private static void errors(Scanner s, int count, String fragment) {
        equal(count, s.errors().size(), "quantidade de erros");
        check(s.errors().stream().anyMatch(e -> e.mensagem().contains(fragment)), "mensagem: " + fragment);
    }

    private static void position(Token t, int line, int column) {
        equal(line, t.linha(), "linha de " + t.lexema());
        equal(column, t.coluna(), "coluna de " + t.lexema());
    }

    private static void position(ErrorLexico e, int line, int column) {
        equal(line, e.linha(), "linha do erro");
        equal(column, e.coluna(), "coluna do erro");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void equal(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": esperado=" + expected + ", atual=" + actual);
        }
    }
}
