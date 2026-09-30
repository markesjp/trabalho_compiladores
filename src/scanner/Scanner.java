package scanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Map.entry;

/** Scanner manual: cada estado dos reconhecedores corresponde a um case. */
public final class Scanner {
    private enum IdentifierState { START, ID_BODY, ACCEPT_IDENTIFIER }
    private enum NumberState { NUM_START, INT_BODY, DECIMAL_POINT, FRACTION_BODY, ACCEPT_INT, ACCEPT_DOUBLE }
    private enum StringState {
        STRING_START, STRING_BODY, ESCAPE, STRING_RECOVERY, RECOVERY_ESCAPE, ACCEPT_STRING, ERROR_STRING
    }
    private enum CharState {
        CHAR_START, CHAR_BODY, CHAR_ESCAPE, CHAR_CLOSE, CHAR_RECOVERY,
        CHAR_RECOVERY_ESCAPE, ACCEPT_CHAR, ERROR_CHAR
    }
    private enum OperatorState { OP_START, OP_LOOKAHEAD, ACCEPT_OPERATOR, ERROR_OPERATOR }
    private enum CommentState { NORMAL, LINE_COMMENT, BLOCK_COMMENT }

    private static final Map<String, TipoToken> KEYWORDS = Map.ofEntries(
            entry("int", TipoToken.INT), entry("double", TipoToken.DOUBLE),
            entry("bool", TipoToken.BOOL), entry("char", TipoToken.CHAR),
            entry("string", TipoToken.STRING), entry("void", TipoToken.VOID),
            entry("if", TipoToken.IF), entry("else", TipoToken.ELSE), entry("while", TipoToken.WHILE),
            entry("fn", TipoToken.FN), entry("return", TipoToken.RETURN),
            entry("true", TipoToken.TRUE), entry("false", TipoToken.FALSE));

    private static final Map<String, TipoToken> OPERATORS = Map.ofEntries(
            entry("=", TipoToken.ASSIGN), entry("==", TipoToken.EQEQ), entry("!=", TipoToken.NE),
            entry("<", TipoToken.LT), entry("<=", TipoToken.LE), entry(">", TipoToken.GT), entry(">=", TipoToken.GE),
            entry("&&", TipoToken.AND), entry("||", TipoToken.OR), entry("!", TipoToken.NOT),
            entry("+", TipoToken.PLUS), entry("-", TipoToken.MINUS), entry("*", TipoToken.STAR),
            entry("/", TipoToken.SLASH), entry("%", TipoToken.MOD));

    private static final Map<Character, TipoToken> DELIMITERS = Map.ofEntries(
            entry('(', TipoToken.LPAREN), entry(')', TipoToken.RPAREN),
            entry('{', TipoToken.LBRACE), entry('}', TipoToken.RBRACE),
            entry('[', TipoToken.LBRACKET), entry(']', TipoToken.RBRACKET),
            entry(',', TipoToken.COMMA), entry(';', TipoToken.SEMI), entry(':', TipoToken.COLON));

    private static final Set<Character> STRING_ESCAPES = Set.of('"', '\\', 'n', 'r', 't');
    private static final Set<Character> CHAR_ESCAPES = Set.of('"', '\'', '\\', 'n', 'r', 't');

    private final String source;
    private final Consumer<String> trace;
    private final List<ErrorLexico> errors = new ArrayList<>();
    private int index;
    private int line = 1;
    private int column = 1;

    public Scanner(String source) {
        this(source, null);
    }

    /** trace é opcional e recebe os estados visitados, sem alterar o reconhecimento. */
    public Scanner(String source, Consumer<String> trace) {
        this.source = source == null ? "" : source;
        this.trace = trace;
    }

    /** Consulta caracteres restantes, inclusive espaços e comentários, não tokens. */
    public boolean hasNext() {
        return !isAtEnd();
    }

    public List<ErrorLexico> errors() {
        return Collections.unmodifiableList(errors);
    }

    public Token nextToken() {
        // Recuperação iterativa: não chama nextToken() recursivamente após um erro.
        while (true) {
            skipWhitespaceAndComments();
            if (isAtEnd()) {
                return new Token(TipoToken.EOF, "", line, column);
            }
            int startLine = line;
            int startColumn = column;
            char current = peek();
            Token token;
            if (isIdentifierStart(current)) {
                token = scanIdentifierOrKeyword(startLine, startColumn);
            } else if (isDigit(current)) {
                token = scanNumber(startLine, startColumn);
            } else if (current == '"') {
                token = scanString(startLine, startColumn);
            } else if (current == '\'') {
                token = scanChar(startLine, startColumn);
            } else if (OPERATORS.containsKey(String.valueOf(current)) || current == '&' || current == '|') {
                token = scanOperator(startLine, startColumn);
            } else if (DELIMITERS.containsKey(current)) {
                token = new Token(DELIMITERS.get(current), String.valueOf(advance()), startLine, startColumn);
            } else {
                report("caractere não reconhecido/fora do alfabeto: '" + Token.escape(String.valueOf(current)) + "'",
                        startLine, startColumn);
                advance();
                token = null;
            }
            if (token != null) {
                return token;
            }
        }
    }

    private Token scanIdentifierOrKeyword(int startLine, int startColumn) {
        int start = index;
        IdentifierState state = IdentifierState.START;
        while (true) {
            trace("ID", state);
            switch (state) {
                case START -> {
                    advance(); // O despacho garante uma letra inicial ou '_'.
                    state = IdentifierState.ID_BODY;
                }
                case ID_BODY -> {
                    if (hasNext() && isIdentifierPart(peek())) {
                        advance();
                    } else {
                        state = IdentifierState.ACCEPT_IDENTIFIER;
                    }
                }
                case ACCEPT_IDENTIFIER -> {
                    String lexeme = source.substring(start, index);
                    return new Token(KEYWORDS.getOrDefault(lexeme, TipoToken.ID), lexeme, startLine, startColumn);
                }
            }
        }
    }

    private Token scanNumber(int startLine, int startColumn) {
        int start = index;
        NumberState state = NumberState.NUM_START;
        while (true) {
            trace("NUM", state);
            switch (state) {
                case NUM_START -> {
                    advance();
                    state = NumberState.INT_BODY;
                }
                case INT_BODY -> {
                    if (hasNext() && isDigit(peek())) {
                        advance();
                    } else if (hasNext() && peek() == '.' && isDigit(peekNext())) {
                        state = NumberState.DECIMAL_POINT;
                    } else {
                        state = NumberState.ACCEPT_INT;
                    }
                }
                case DECIMAL_POINT -> {
                    advance(); // Só entra aqui se há um dígito depois do ponto.
                    state = NumberState.FRACTION_BODY;
                }
                case FRACTION_BODY -> {
                    if (hasNext() && isDigit(peek())) {
                        advance();
                    } else {
                        state = NumberState.ACCEPT_DOUBLE;
                    }
                }
                case ACCEPT_INT -> { return emit(TipoToken.INT_LIT, start, startLine, startColumn); }
                case ACCEPT_DOUBLE -> { return emit(TipoToken.DOUBLE_LIT, start, startLine, startColumn); }
            }
        }
    }

    private Token scanString(int startLine, int startColumn) {
        int start = index;
        int escapeLine = startLine;
        int escapeColumn = startColumn;
        StringState state = StringState.STRING_START;
        while (true) {
            trace("STRING", state);
            switch (state) {
                case STRING_START -> {
                    advance();
                    state = StringState.STRING_BODY;
                }
                case STRING_BODY, STRING_RECOVERY -> {
                    boolean recovering = state == StringState.STRING_RECOVERY;
                    if (atLineEndOrEof()) {
                        report("string não fechada até " + (isAtEnd() ? "EOF" : "o fim da linha"),
                                startLine, startColumn);
                        state = StringState.ERROR_STRING;
                    } else if (peek() == '"') {
                        advance();
                        state = recovering ? StringState.ERROR_STRING : StringState.ACCEPT_STRING;
                    } else if (peek() == '\\') {
                        escapeLine = line;
                        escapeColumn = column;
                        advance();
                        state = recovering ? StringState.RECOVERY_ESCAPE : StringState.ESCAPE;
                    } else {
                        if (!isPermittedInputCharacter(peek())) {
                            report("caractere fora do alfabeto dentro da string", line, column);
                            state = StringState.STRING_RECOVERY;
                        }
                        advance();
                    }
                }
                case ESCAPE, RECOVERY_ESCAPE -> {
                    boolean recovering = state == StringState.RECOVERY_ESCAPE;
                    if (atLineEndOrEof()) {
                        report("string não fechada após barra invertida até " + (isAtEnd() ? "EOF" : "o fim da linha"),
                                startLine, startColumn);
                        state = StringState.ERROR_STRING;
                    } else {
                        boolean validEscape = STRING_ESCAPES.contains(peek());
                        if (!validEscape) {
                            report("escape inválido em string: '\\" + Token.escape(String.valueOf(peek())) + "'",
                                    escapeLine, escapeColumn);
                        }
                        advance();
                        state = recovering || !validEscape ? StringState.STRING_RECOVERY : StringState.STRING_BODY;
                    }
                }
                case ACCEPT_STRING -> { return emit(TipoToken.STRING_LIT, start, startLine, startColumn); }
                case ERROR_STRING -> { return null; }
            }
        }
    }

    private Token scanChar(int startLine, int startColumn) {
        int start = index;
        boolean valid = true;
        CharState state = CharState.CHAR_START;
        while (true) {
            trace("CHAR", state);
            switch (state) {
                case CHAR_START -> {
                    advance();
                    state = CharState.CHAR_BODY;
                }
                case CHAR_BODY -> {
                    if (atLineEndOrEof()) {
                        report("literal char não fechado", startLine, startColumn);
                        state = CharState.ERROR_CHAR;
                    } else if (peek() == '\'') {
                        report("literal char vazio", startLine, startColumn);
                        advance();
                        state = CharState.ERROR_CHAR;
                    } else if (peek() == '\\') {
                        advance();
                        state = CharState.CHAR_ESCAPE;
                    } else {
                        if (peek() < 32 || peek() > 126) {
                            report("literal char exige um caractere ASCII imprimível", line, column);
                            valid = false;
                        }
                        advance();
                        state = CharState.CHAR_CLOSE;
                    }
                }
                case CHAR_ESCAPE -> {
                    if (atLineEndOrEof()) {
                        report("literal char não fechado após barra invertida", startLine, startColumn);
                        state = CharState.ERROR_CHAR;
                    } else {
                        if (!CHAR_ESCAPES.contains(peek())) {
                            report("escape inválido em literal char", line, column - 1);
                            valid = false;
                        }
                        advance();
                        state = CharState.CHAR_CLOSE;
                    }
                }
                case CHAR_CLOSE -> {
                    if (hasNext() && peek() == '\'') {
                        advance();
                        state = valid ? CharState.ACCEPT_CHAR : CharState.ERROR_CHAR;
                    } else {
                        report("literal char não fechado ou com mais de um caractere", startLine, startColumn);
                        state = CharState.CHAR_RECOVERY;
                    }
                }
                case CHAR_RECOVERY -> {
                    if (atLineEndOrEof()) {
                        state = CharState.ERROR_CHAR;
                    } else if (peek() == '\\') {
                        advance();
                        state = CharState.CHAR_RECOVERY_ESCAPE;
                    } else if (advance() == '\'') {
                        state = CharState.ERROR_CHAR;
                    }
                }
                case CHAR_RECOVERY_ESCAPE -> {
                    if (atLineEndOrEof()) {
                        state = CharState.ERROR_CHAR;
                    } else {
                        advance();
                        state = CharState.CHAR_RECOVERY;
                    }
                }
                case ACCEPT_CHAR -> { return emit(TipoToken.CHAR_LIT, start, startLine, startColumn); }
                case ERROR_CHAR -> { return null; }
            }
        }
    }

    private Token scanOperator(int startLine, int startColumn) {
        int start = index;
        OperatorState state = OperatorState.OP_START;
        while (true) {
            trace("OP", state);
            switch (state) {
                case OP_START -> {
                    advance();
                    state = OperatorState.OP_LOOKAHEAD;
                }
                case OP_LOOKAHEAD -> {
                    String first = source.substring(start, index);
                    if (hasNext() && OPERATORS.containsKey(first + peek())) {
                        advance();
                        state = OperatorState.ACCEPT_OPERATOR;
                    } else if (OPERATORS.containsKey(first)) {
                        state = OperatorState.ACCEPT_OPERATOR;
                    } else {
                        report("operador '" + first + "' deve ser duplicado", startLine, startColumn);
                        state = OperatorState.ERROR_OPERATOR;
                    }
                }
                case ACCEPT_OPERATOR -> {
                    return emit(OPERATORS.get(source.substring(start, index)), start, startLine, startColumn);
                }
                case ERROR_OPERATOR -> { return null; }
            }
        }
    }

    private void skipWhitespaceAndComments() {
        CommentState state = CommentState.NORMAL;
        int startLine = line;
        int startColumn = column;
        while (true) {
            trace("COMMENT", state);
            switch (state) {
                case NORMAL -> {
                    while (hasNext() && isWhitespace(peek())) {
                        advance();
                    }
                    if (source.startsWith("//", index) || source.startsWith("/*", index)) {
                        startLine = line;
                        startColumn = column;
                        state = peekNext() == '/' ? CommentState.LINE_COMMENT : CommentState.BLOCK_COMMENT;
                        advance();
                        advance();
                    } else {
                        return;
                    }
                }
                case LINE_COMMENT -> {
                    if (atLineEndOrEof()) {
                        state = CommentState.NORMAL;
                    } else {
                        consumeCommentCharacter();
                    }
                }
                case BLOCK_COMMENT -> {
                    if (isAtEnd()) {
                        report("comentário de bloco não fechado até EOF", startLine, startColumn);
                        return;
                    } else if (source.startsWith("*/", index)) {
                        advance();
                        advance();
                        state = CommentState.NORMAL;
                    } else {
                        consumeCommentCharacter();
                    }
                }
            }
        }
    }

    private void consumeCommentCharacter() {
        if (!isPermittedInputCharacter(peek())) {
            report("caractere fora do alfabeto dentro de comentário", line, column);
        }
        advance();
    }

    private Token emit(TipoToken type, int start, int startLine, int startColumn) {
        return new Token(type, source.substring(start, index), startLine, startColumn);
    }

    private void trace(String automaton, Enum<?> state) {
        if (trace != null) {
            String lookahead = isAtEnd() ? "EOF" : "'" + Token.escape(String.valueOf(peek())) + "'";
            trace.accept(automaton + " " + state.name() + " @ " + line + ":" + column + " lookahead=" + lookahead);
        }
    }

    private char peek() {
        return source.charAt(index);
    }

    private char peekNext() {
        return index + 1 < source.length() ? source.charAt(index + 1) : '\0';
    }

    private char advance() {
        char current = source.charAt(index++);
        if (current == '\r') {
            if (hasNext() && peek() == '\n') {
                index++;
            }
            line++;
            column = 1;
        } else if (current == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return current;
    }

    private boolean isAtEnd() {
        return index >= source.length();
    }

    private boolean atLineEndOrEof() {
        return isAtEnd() || peek() == '\n' || peek() == '\r';
    }

    private void report(String message, int errorLine, int errorColumn) {
        errors.add(new ErrorLexico(message, errorLine, errorColumn));
    }

    private static boolean isPermittedInputCharacter(char value) {
        return value == '\t' || value == '\n' || value == '\r' || (value >= 32 && value <= 126);
    }

    private static boolean isIdentifierStart(char value) {
        return (value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z') || value == '_';
    }

    private static boolean isIdentifierPart(char value) {
        return isIdentifierStart(value) || isDigit(value);
    }

    private static boolean isDigit(char value) {
        return value >= '0' && value <= '9';
    }

    private static boolean isWhitespace(char value) {
        return value == ' ' || value == '\t' || value == '\n' || value == '\r';
    }
}
