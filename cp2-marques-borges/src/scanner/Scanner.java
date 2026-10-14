package scanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class Scanner {
    private static final Set<String> KEYWORDS = Set.of(
            "int", "double", "bool", "char", "string",
            "if", "else", "while", "fn", "return", "true", "false", "void");

    private static final Set<String> TWO_CHARACTER_OPERATORS = Set.of(
            "==", "!=", "<=", ">=", "&&", "||");

    private static final Set<Character> ONE_CHARACTER_OPERATORS = Set.of(
            '=', '!', '<', '>', '+', '-', '*', '/', '%');

    private static final Set<Character> DELIMITERS = Set.of(
            '(', ')', '{', '}', '[', ']', ',', ';', ':');

    private static final Set<Character> STRING_ESCAPES = Set.of(
            '"', '\\', 'n', 'r', 't');

    private static final Set<Character> CHAR_ESCAPES = Set.of(
            '"', '\'', '\\', 'n', 'r', 't');

    private final String source;
    private final List<ErrorLexico> errors = new ArrayList<>();
    private int index;
    private int line = 1;
    private int column = 1;

    public Scanner(String source) {
        this.source = source == null ? "" : source;
    }

    public boolean hasNext() {
        return !isAtEnd();
    }

    public List<ErrorLexico> errors() {
        return Collections.unmodifiableList(errors);
    }

    public Token nextToken() {
        while (true) {
            skipWhitespaceAndComments();
            if (isAtEnd()) {
                return new Token(TipoToken.EOF, "", line, column);
            }

            int startLine = line;
            int startColumn = column;
            char current = peek();

            if (isIdentifierStart(current)) {
                return scanIdentifierOrKeyword(startLine, startColumn);
            }
            if (isDigit(current)) {
                return scanNumber(startLine, startColumn);
            }
            if (current == '"') {
                Token string = scanString(startLine, startColumn);
                if (string != null) {
                    return string;
                }
                continue;
            }
            if (current == '\'') {
                Token character = scanChar(startLine, startColumn);
                if (character != null) {
                    return character;
                }
                continue;
            }
            if (ONE_CHARACTER_OPERATORS.contains(current) || current == '&' || current == '|') {
                Token operator = scanOperator(startLine, startColumn);
                if (operator != null) {
                    return operator;
                }
                continue;
            }
            if (DELIMITERS.contains(current)) {
                return new Token(TipoToken.DELIMITER, String.valueOf(advance()), startLine, startColumn);
            }

            report("caractere fora do alfabeto: '" + current + "'", startLine, startColumn);
            advance();
        }
    }

    private Token scanIdentifierOrKeyword(int startLine, int startColumn) {
        // AFD: START -> ID_BODY -> ACCEPT_IDENTIFIER.
        StringBuilder lexeme = new StringBuilder();
        lexeme.append(advance());
        while (!isAtEnd() && isIdentifierPart(peek())) {
            lexeme.append(advance());
        }
        String value = lexeme.toString();
        TipoToken type = KEYWORDS.contains(value) ? TipoToken.KEYWORD : TipoToken.IDENTIFIER;
        return new Token(type, value, startLine, startColumn);
    }

    private Token scanNumber(int startLine, int startColumn) {
        // AFD manual para INTEGER_LITERAL e DOUBLE_LITERAL.
        StringBuilder lexeme = new StringBuilder();
        while (!isAtEnd() && isDigit(peek())) {
            lexeme.append(advance());
        }

        boolean decimal = false;
        if (!isAtEnd() && peek() == '.' && peekNextIsDigit()) {
            decimal = true;
            lexeme.append(advance());
            while (!isAtEnd() && isDigit(peek())) {
                lexeme.append(advance());
            }
        }

        return new Token(decimal ? TipoToken.DOUBLE_LITERAL : TipoToken.INTEGER_LITERAL,
                lexeme.toString(), startLine, startColumn);
    }

    private Token scanString(int startLine, int startColumn) {
        // AFD: STRING_BODY -> ESCAPE -> STRING_BODY; aspas finais -> ACCEPT_STRING.
        StringBuilder lexeme = new StringBuilder();
        lexeme.append(advance());

        while (!isAtEnd()) {
            char current = peek();
            if (current == '"') {
                lexeme.append(advance());
                return new Token(TipoToken.STRING_LITERAL, lexeme.toString(), startLine, startColumn);
            }
            if (current == '\n' || current == '\r') {
                report("string não fechada até o fim da linha", startLine, startColumn);
                return null;
            }
            if (current == '\\') {
                int escapeLine = line;
                int escapeColumn = column;
                lexeme.append(advance());
                if (isAtEnd() || peek() == '\n' || peek() == '\r') {
                    report("string não fechada após barra invertida", startLine, startColumn);
                    return null;
                }
                char escaped = peek();
                if (!STRING_ESCAPES.contains(escaped)) {
                    report("escape inválido: \\" + escaped + "'", escapeLine, escapeColumn);
                }
                lexeme.append(advance());
                continue;
            }
            if (!isPermittedInputCharacter(current)) {
                report("caractere fora do alfabeto dentro da string: '" + current + "'", line, column);
            }
            lexeme.append(advance());
        }

        report("string não fechada até EOF", startLine, startColumn);
        return null;
    }

    private Token scanOperator(int startLine, int startColumn) {
        // Maximal munch: tenta primeiro uma forma de dois caracteres.
        char first = peek();
        char second = peekNext();
        String pair = "" + first + second;
        if (TWO_CHARACTER_OPERATORS.contains(pair)) {
            advance();
            advance();
            return new Token(TipoToken.OPERATOR, pair, startLine, startColumn);
        }

        if (first == '&' || first == '|') {
            report("operador '" + first + "' deve ser duplicado", startLine, startColumn);
            advance();
            return null;
        }

        advance();
        return new Token(TipoToken.OPERATOR, String.valueOf(first), startLine, startColumn);
    }

    private Token scanChar(int startLine, int startColumn) {
        // AFD: CHAR_BODY -> CHAR_ESCAPE opcional -> ACCEPT_CHAR.
        StringBuilder lexeme = new StringBuilder();
        lexeme.append(advance());
        if (isAtEnd() || peek() == '\n' || peek() == '\r') {
            report("literal char não fechado", startLine, startColumn);
            return null;
        }

        boolean valid = true;
        if (peek() == '\\') {
            lexeme.append(advance());
            if (isAtEnd() || peek() == '\n' || peek() == '\r') {
                report("literal char não fechado", startLine, startColumn);
                return null;
            }
            if (!CHAR_ESCAPES.contains(peek())) {
                report("escape inválido em literal char", line, column);
                valid = false;
            }
            lexeme.append(advance());
        } else if (peek() != '\'' && peek() >= 32 && peek() <= 126) {
            lexeme.append(advance());
        } else {
            report("literal char exige um caractere ASCII", line, column);
            valid = false;
        }

        if (!isAtEnd() && peek() == '\'') {
            lexeme.append(advance());
            return valid ? new Token(TipoToken.CHAR_LITERAL, lexeme.toString(), startLine, startColumn) : null;
        }

        report("literal char não fechado ou com mais de um caractere", startLine, startColumn);
        while (!isAtEnd() && peek() != '\'' && peek() != '\n' && peek() != '\r') {
            advance();
        }
        if (!isAtEnd() && peek() == '\'') {
            advance();
        }
        return null;
    }

    private void skipWhitespaceAndComments() {
        boolean repeat;
        do {
            repeat = false;
            while (!isAtEnd() && isWhitespace(peek())) {
                advance();
            }
            if (startsWith("//")) {
                repeat = true;
                advance();
                advance();
                while (!isAtEnd() && peek() != '\n' && peek() != '\r') {
                    reportInvalidCommentCharacter();
                    advance();
                }
            } else if (startsWith("/*")) {
                repeat = true;
                skipBlockComment();
            }
        } while (repeat);
    }

    private void skipBlockComment() {
        int startLine = line;
        int startColumn = column;
        advance();
        advance();
        while (!isAtEnd() && !startsWith("*/")) {
            reportInvalidCommentCharacter();
            advance();
        }
        if (isAtEnd()) {
            report("comentário de bloco não fechado até EOF", startLine, startColumn);
            return;
        }
        advance();
        advance();
    }

    private boolean startsWith(String value) {
        return source.startsWith(value, index);
    }

    private char peek() {
        return source.charAt(index);
    }

    private char peekNext() {
        return index + 1 < source.length() ? source.charAt(index + 1) : '\0';
    }

    private boolean peekNextIsDigit() {
        return index + 1 < source.length() && isDigit(source.charAt(index + 1));
    }

    private char advance() {
        char current = source.charAt(index++);
        if (current == '\r') {
            if (index < source.length() && source.charAt(index) == '\n') {
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

    private void report(String message, int errorLine, int errorColumn) {
        errors.add(new ErrorLexico(message, errorLine, errorColumn));
    }

    private void reportInvalidCommentCharacter() {
        if (!isPermittedInputCharacter(peek())) {
            report("caractere fora do alfabeto dentro de comentário: '" + peek() + "'", line, column);
        }
    }

    private static boolean isPermittedInputCharacter(char value) {
        return value == '\t' || value == '\n' || value == '\r' || (value >= 32 && value <= 126);
    }

    private static boolean isIdentifierStart(char value) {
        return isAsciiLetter(value) || value == '_';
    }

    private static boolean isIdentifierPart(char value) {
        return isIdentifierStart(value) || isDigit(value);
    }

    private static boolean isAsciiLetter(char value) {
        return (value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z');
    }

    private static boolean isDigit(char value) {
        return value >= '0' && value <= '9';
    }

    private static boolean isWhitespace(char value) {
        return value == ' ' || value == '\t' || value == '\n' || value == '\r';
    }
}
