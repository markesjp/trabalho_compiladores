# Especificação léxica da linguagem Lumen

Checkpoint 1 de Construção de Compiladores. Autores: João Pedro de Oliveira Marques e Victor Hugo Borges.

## 1. Objetivo e decisões globais

Lumen é uma linguagem didática para programas com variáveis dos tipos `int`, `double`, `bool`, `char` e `string`, seleção `if/else`, repetição `while` e funções/procedimentos. Esta especificação é o contrato do scanner e define exatamente os tokens aceitos nesta etapa.

O alfabeto de entrada é ASCII imprimível (`U+0020`–`U+007E`) mais tabulação (`U+0009`), LF (`U+000A`) e CR (`U+000D`). Fora de strings e comentários, os caracteres reconhecidos são letras `A-Z` e `a-z`, dígitos `0-9`, sublinhado, espaços, aspas simples e duplas, barra invertida em escapes, os caracteres dos operadores `= ! < > + - * / % & |`, ponto e os delimitadores `(` `)` `{` `}` `[` `]` `,` `;` `:`. Outros caracteres geram erro léxico. A linguagem é case-sensitive: `int`, `Int` e `INT` são lexemas diferentes, e apenas o primeiro é palavra reservada.

## 2. Tokens

| Categoria | Notação | Exemplos válidos | Decisão da linguagem |
|---|---|---|---|
| Identificador | `[A-Za-z_][A-Za-z0-9_]*` | `total`, `_aux`, `contaItens2` | Sublinhado é permitido no início e no corpo. Não há tamanho máximo imposto pelo scanner. |
| Palavra reservada | Identificador pertencente à lista fechada abaixo | `int`, `while`, `return` | A comparação é case-sensitive. O tipo é específico: `INT`, `WHILE`, `RETURN` etc.; a categoria é `KEYWORD`. |
| String | `" (caractere permitido ou escape válido)* "` | `"ok"`, `"linha\n2"`, `"ele disse \"oi\""` | Strings são delimitadas por aspas duplas, não atravessam linha e aceitam `\"`, `\\`, `\n`, `\r` e `\t`. String sem fechamento até fim da linha ou EOF é erro. |
| Caractere | `' (caractere ASCII imprimível ou escape válido) '` | `'a'`, `'\n'` | Exatamente um caractere ou escape. Aceita `\'`, `\"`, `\\`, `\n`, `\r` e `\t`. |
| Operador | Um ou dois caracteres da tabela de operadores | `=`, `==`, `<=`, `!=`, `&&` | O scanner aplica maximal munch: tenta a forma composta antes da simples. `&` e `|` isolados são inválidos. |
| Literal numérico | `[0-9]+(\.[0-9]+)?` | `0`, `10`, `3.14` | Inteiro e decimal são tokens distintos. Notação científica, hexadecimal e sinal no literal não fazem parte desta etapa. |
| Delimitador | Um caractere de `(){}[],;:` | `(`, `{`, `;`, `:` | Delimitadores são reconhecidos individualmente. |

Na notação de string, caractere comum é ASCII imprimível, exceto `"` e `\`, ou tabulação; CR/LF são proibidos. Em char, o caractere comum exclui `'` e `\` e não aceita tabulação literal. Aspas e barras nesses contextos precisam dos escapes indicados. A notação é documental; o reconhecimento não usa regex de biblioteca.

### Palavras reservadas

```text
int double bool char string void
if else while
fn return
true false
```

`true` e `false` geram os tipos `TRUE` e `FALSE`, ambos na categoria `KEYWORD`.

`Token.tipo()` identifica terminais específicos: `ID`, `INT_LIT`, `DOUBLE_LIT`, `STRING_LIT`, `CHAR_LIT`, palavras reservadas, operadores e delimitadores. `Token.categoria()` fornece o agrupamento didático. Por exemplo, `+` é `PLUS` na categoria `OPERATOR` e `;` é `SEMI` na categoria `DELIMITER`.

### Operadores

| Forma | Lexemas |
|---|---|
| Com dois caracteres | `==`, `!=`, `<=`, `>=`, `&&`, `||` |
| Com um caractere | `=`, `!`, `<`, `>`, `+`, `-`, `*`, `/`, `%` |

O ponto só é aceito como parte de um literal decimal com pelo menos um dígito antes e depois dele. Portanto, `3.14` é um literal; `3.` produz o literal `3` e depois um erro para `.`.

O sinal é operador separado. Sequências como `12abc` e `1e3` são número seguido de identificador, não literais únicos; sua admissibilidade sintática fica para o parser.

## 3. Espaços, comentários e EOF

Espaço, tabulação, `\n` e `\r` fora de tokens são ignorados. Comentários de linha começam com `//` e terminam antes de CR, LF ou EOF. Comentários de bloco começam com `/*` e terminam no primeiro `*/`; não são aninhados. Comentário de bloco não fechado até EOF é erro léxico.

Comentários não geram tokens. O scanner conserva linha e coluna do início de cada token. A posição começa em linha 1, coluna 1; `\n` incrementa a linha e reinicia a coluna em 1, e `\r\n` conta como uma única quebra de linha.

CR isolado também é quebra de linha; tabulação ocupa uma coluna. O lexema conserva o texto de origem, incluindo aspas e escapes. EOF tem lexema vazio e pode ser consultado novamente sem mudar a posição.

## 4. Recuperação de erros

Caractere fora do alfabeto, escape desconhecido, string ou caractere não fechado até fim de linha/EOF, `&` isolado, `|` isolado e comentário de bloco não fechado são erros léxicos. Cada erro registra mensagem, linha e coluna e o scanner continua procurando o próximo token quando há texto restante. Exceções não tratadas não são usadas para sinalizar erros esperados.

Caractere isolado inválido é consumido uma vez. String/char com conteúdo inválido é descartado até fechamento não escapado, quebra de linha ou EOF, sem emitir token válido. Na falta de fechamento, o erro aponta para a abertura; no escape inválido, para a barra. A quebra de linha fica para a rotina de espaços. O avanço e a recuperação são iterativos.

## 5. Correspondência com o AFD

Os desenhos [identificador/palavra reservada](automatos/afd-identificador.svg) e [string](automatos/afd-string.svg) usam os mesmos nomes de estados empregados no código:

- identificadores e palavras reservadas: `START`, `ID_BODY`, `ACCEPT_IDENTIFIER`;
- strings: `STRING_START`, `STRING_BODY`, `ESCAPE`, `STRING_RECOVERY`, `RECOVERY_ESCAPE`, `ACCEPT_STRING`, `ERROR_STRING`.

Cada estado é um case do switch de seu enum em `Scanner.java`. Números, caracteres, operadores e comentários usam `NumberState`, `CharState`, `OperatorState` e `CommentState`. `--trace` mostra os estados visitados antes da ação. Os desenhos devem acompanhar qualquer mudança da linguagem.

## 6. Compilação e verificação

Requer JDK 17 ou superior, sem dependências externas. Na raiz do projeto, no PowerShell:

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -encoding UTF-8 -Xlint:all --release 17 -d out src/scanner/*.java test/ScannerTest.java
java -cp out ScannerTest
java -cp out scanner.Main caminho/do/arquivo.lumen
```

No Bash, substitua o primeiro comando por `mkdir -p out`. O arquivo de entrada é fornecido pelo usuário. `--trace` antes do caminho habilita o rastreamento opcional de estados. A saída contém `TIPO('lexema') @ linha:coluna`; diagnósticos vão para stderr. Códigos de saída: 0 sem erros, 1 com erros léxicos, 2 para uso incorreto ou falha de leitura.

Os 35 testes incluem as cinco categorias, os três erros mínimos, comentários, posições, recuperação, funções e controle, além de 2.000 entradas determinísticas. A [evidência de execução](test/evidencia-execucao.txt) acompanha a suíte. `out/` é apenas o resultado local da compilação e não integra a entrega. O [log de IA](log-uso-ia.md) declara a assistência utilizada.
