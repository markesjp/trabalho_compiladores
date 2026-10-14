import scanner.ErrorLexico;
import scanner.Scanner;
import scanner.TipoToken;
import scanner.Token;

import java.util.ArrayList;
import java.util.List;

public final class ScannerTest {
    private ScannerTest() {
    }

    public static void main(String[] args) {
        testCategoriasValidas();
        testMaximalMunch();
        testStringsEComentarios();
        testTrechoRealista();
        testRecuperacaoDeErros();
        testStringNaoFechadaAteLinha();
        testStringNaoFechadaAteEof();
        testCaractereForaDoAlfabeto();
        testLiteralChar();
        testComentarioDeBlocoNaoFechado();
        System.out.println("Todos os testes passaram.");
    }

    private static void testCategoriasValidas() {
        Scanner scanner = new Scanner("int total = 10; double media = 3.14; string nome = \"Ana\";");
        List<Token> tokens = tokensUntilEof(scanner);
        assertType(tokens.get(0), TipoToken.KEYWORD);
        assertType(tokens.get(1), TipoToken.IDENTIFIER);
        assertType(tokens.get(3), TipoToken.INTEGER_LITERAL);
        assertType(tokens.get(5), TipoToken.KEYWORD);
        assertType(tokens.get(8), TipoToken.DOUBLE_LITERAL);
        assertType(tokens.get(13), TipoToken.STRING_LITERAL);
        assertEquals(0, scanner.errors().size(), "não deveria haver erro em tokens válidos");
    }

    private static void testMaximalMunch() {
        Scanner scanner = new Scanner("== != <= >= && || = ! < > + - * / %");
        List<Token> tokens = tokensUntilEof(scanner);
        String[] expected = {"==", "!=", "<=", ">=", "&&", "||", "=", "!", "<", ">", "+", "-", "*", "/", "%"};
        assertEquals(expected.length, tokens.size(), "quantidade de operadores");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], tokens.get(i).lexema(), "operador na posição " + i);
        }
        assertEquals(0, scanner.errors().size(), "operadores válidos não devem gerar erro");
    }

    private static void testStringsEComentarios() {
        String source = "/* inicio */ fn int f() { // linha\n return 1; }";
        Scanner scanner = new Scanner(source);
        List<Token> tokens = tokensUntilEof(scanner);
        assertEquals("fn", tokens.get(0).lexema(), "palavra reservada após comentário");
        assertEquals("return", tokens.get(6).lexema(), "token após comentário de linha");
        assertEquals(0, scanner.errors().size(), "comentários válidos não devem gerar erro");
    }

    private static void testRecuperacaoDeErros() {
        Scanner scanner = new Scanner("int x = 10 @ y = 20;");
        List<Token> tokens = tokensUntilEof(scanner);
        assertEquals("y", tokens.get(4).lexema(), "scanner deve continuar após caractere inválido");
        assertEquals(1, scanner.errors().size(), "deveria registrar um erro");
        assertEquals(1, scanner.errors().get(0).linha(), "linha do caractere inválido");
        assertEquals(12, scanner.errors().get(0).coluna(), "coluna do caractere inválido");
    }

    private static void testTrechoRealista() {
        Scanner scanner = new Scanner("int total = 10; /* nota */ double media = 3.14; // fim\n total = total + 1;");
        List<Token> tokens = tokensUntilEof(scanner);
        String[] expected = {"int", "total", "=", "10", ";", "double", "media", "=", "3.14", ";",
                "total", "=", "total", "+", "1", ";"};
        assertEquals(expected.length, tokens.size(), "quantidade de tokens do trecho realista");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], tokens.get(i).lexema(), "lexema do trecho realista na posição " + i);
        }
        assertEquals(0, scanner.errors().size(), "trecho realista válido");
    }

    private static void testStringNaoFechadaAteLinha() {
        Scanner scanner = new Scanner("string a = \"sem fim\nint b = 2;");
        List<Token> tokens = tokensUntilEof(scanner);
        assertEquals("int", tokens.get(3).lexema(), "scanner deve recuperar após string na linha");
        assertContains(scanner.errors(), "fim da linha");
    }

    private static void testStringNaoFechadaAteEof() {
        Scanner scanner = new Scanner("\"sem fim");
        tokensUntilEof(scanner);
        assertContains(scanner.errors(), "EOF");
    }

    private static void testCaractereForaDoAlfabeto() {
        Scanner scanner = new Scanner("int x = 1; § int y = 2;");
        List<Token> tokens = tokensUntilEof(scanner);
        assertEquals("y", tokens.get(6).lexema(), "scanner deve continuar após caractere fora do alfabeto");
        assertContains(scanner.errors(), "fora do alfabeto");
    }

    private static void testComentarioDeBlocoNaoFechado() {
        Scanner scanner = new Scanner("int x = 1; /* sem fim");
        tokensUntilEof(scanner);
        assertContains(scanner.errors(), "comentário de bloco");
    }

    private static void testLiteralChar() {
        Scanner scanner = new Scanner("char c = 'L'; char quebra = '\\n';");
        List<Token> tokens = tokensUntilEof(scanner);
        assertType(tokens.get(3), TipoToken.CHAR_LITERAL);
        assertType(tokens.get(8), TipoToken.CHAR_LITERAL);
        assertEquals(0, scanner.errors().size(), "literais char válidos");
    }

    private static List<Token> tokensUntilEof(Scanner scanner) {
        List<Token> tokens = new ArrayList<>();
        Token token;
        do {
            token = scanner.nextToken();
            tokens.add(token);
        } while (token.tipo() != TipoToken.EOF);
        tokens.remove(tokens.size() - 1);
        return tokens;
    }

    private static void assertType(Token token, TipoToken expected) {
        assertEquals(expected, token.tipo(), "tipo do token " + token.lexema());
    }

    private static void assertContains(List<ErrorLexico> errors, String fragment) {
        for (ErrorLexico error : errors) {
            if (error.mensagem().contains(fragment)) {
                return;
            }
        }
        throw new AssertionError("nenhum erro contém: " + fragment + "\nErros: " + errors);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": esperado=" + expected + ", atual=" + actual);
        }
    }
}
