package scanner;

/** Agrupamento didático; o parser deve consultar o tipo específico do token. */
public enum CategoriaToken {
    IDENTIFIER, KEYWORD, STRING_LITERAL, CHAR_LITERAL,
    INTEGER_LITERAL, DOUBLE_LITERAL, OPERATOR, DELIMITER, EOF
}
