package scanner;

public enum TipoToken {
    ID(CategoriaToken.IDENTIFIER),
    INT_LIT(CategoriaToken.INTEGER_LITERAL),
    DOUBLE_LIT(CategoriaToken.DOUBLE_LITERAL),
    STRING_LIT(CategoriaToken.STRING_LITERAL),
    CHAR_LIT(CategoriaToken.CHAR_LITERAL),

    INT(CategoriaToken.KEYWORD), DOUBLE(CategoriaToken.KEYWORD),
    BOOL(CategoriaToken.KEYWORD), CHAR(CategoriaToken.KEYWORD),
    STRING(CategoriaToken.KEYWORD), VOID(CategoriaToken.KEYWORD),
    IF(CategoriaToken.KEYWORD), ELSE(CategoriaToken.KEYWORD),
    WHILE(CategoriaToken.KEYWORD), FN(CategoriaToken.KEYWORD),
    RETURN(CategoriaToken.KEYWORD), TRUE(CategoriaToken.KEYWORD), FALSE(CategoriaToken.KEYWORD),

    ASSIGN(CategoriaToken.OPERATOR), EQEQ(CategoriaToken.OPERATOR),
    NE(CategoriaToken.OPERATOR), LT(CategoriaToken.OPERATOR), LE(CategoriaToken.OPERATOR),
    GT(CategoriaToken.OPERATOR), GE(CategoriaToken.OPERATOR),
    AND(CategoriaToken.OPERATOR), OR(CategoriaToken.OPERATOR), NOT(CategoriaToken.OPERATOR),
    PLUS(CategoriaToken.OPERATOR), MINUS(CategoriaToken.OPERATOR),
    STAR(CategoriaToken.OPERATOR), SLASH(CategoriaToken.OPERATOR), MOD(CategoriaToken.OPERATOR),

    LPAREN(CategoriaToken.DELIMITER), RPAREN(CategoriaToken.DELIMITER),
    LBRACE(CategoriaToken.DELIMITER), RBRACE(CategoriaToken.DELIMITER),
    LBRACKET(CategoriaToken.DELIMITER), RBRACKET(CategoriaToken.DELIMITER),
    COMMA(CategoriaToken.DELIMITER), SEMI(CategoriaToken.DELIMITER), COLON(CategoriaToken.DELIMITER),
    EOF(CategoriaToken.EOF);

    private final CategoriaToken categoria;

    TipoToken(CategoriaToken categoria) {
        this.categoria = categoria;
    }

    public CategoriaToken categoria() {
        return categoria;
    }
}
