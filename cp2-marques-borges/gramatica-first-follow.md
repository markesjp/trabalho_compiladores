# CP2 — Gramática e reconhecedor de Lumen

Autores: João Pedro de Oliveira Marques e Victor Hugo Borges.

## 1. Linguagem e contrato do CP1

Programa contém uma ou mais declarações globais de variável ou função. Funções usam
`fn tipo nome(parametros) { comandos }`, com `void` para procedimentos. Variáveis
globais e locais têm um dos cinco tipos básicos, com inicialização opcional. Parâmetros
usam `tipo nome`, argumentos são expressões e as listas podem ser vazias. Chamadas
podem aparecer em expressões ou como comandos. Retorno aceita valor ou apenas `;`.
Blocos de `if`, `else` e `while` exigem chaves. Bloco isolado também é comando.

O scanner e o Token são cópias inalteradas do CP1. O parser consulta `tipo()` para
identificadores/literais/EOF e `lexema()` para KEYWORD, OPERATOR e DELIMITER. Assim,
`true` e `false` continuam KEYWORD e `fn` não exige uma nova categoria léxica.
Não há arrays, acesso por índice, `:`, funções aninhadas, comandos vazios ou atribuição
dentro de expressão. `[` e `]` serem tokens válidos não torna arrays sintaticamente válidos.
O CP2 não verifica declaração de nomes, tipos, quantidade de argumentos ou compatibilidade
de retorno. Também não constrói AST nem executa o programa.

## 2. BNF final

Símbolo inicial: `Programa`. Terminais concretos estão entre aspas. `id`, `inteiro`,
`decimal`, `caractere` e `texto` representam as categorias IDENTIFIER, INTEGER_LITERAL,
DOUBLE_LITERAL, CHAR_LITERAL e STRING_LITERAL do CP1. `EOF` é o fim explícito do scanner,
`$` é o marcador teórico de fim na tabela e `ε` representa a sequência vazia.
`Prime` corresponde ao apóstrofo dos exemplos das aulas.

```bnf
Programa -> Declaracao ListaDeclaracoes EOF
ListaDeclaracoes -> Declaracao ListaDeclaracoes | ε
Declaracao -> Funcao | Variavel
Funcao -> 'fn' TipoRetorno id '(' ParametrosOpt ')' Bloco
TipoRetorno -> Tipo | 'void'
Tipo -> 'int' | 'double' | 'bool' | 'char' | 'string'
ParametrosOpt -> Parametro MaisParametros | ε
Parametro -> Tipo id
MaisParametros -> ',' Parametro MaisParametros | ε
Variavel -> Tipo id InicializacaoOpt ';'
InicializacaoOpt -> '=' Expr | ε
Bloco -> '{' ListaComandos '}'
ListaComandos -> Comando ListaComandos | ε
Comando -> Variavel | Se | Enquanto | Retorno | ComandoId | Bloco
Se -> 'if' '(' Expr ')' Bloco SenaoOpt
SenaoOpt -> 'else' Bloco | ε
Enquanto -> 'while' '(' Expr ')' Bloco
Retorno -> 'return' RetornoOpt ';'
RetornoOpt -> Expr | ε
ComandoId -> id ComandoIdResto
ComandoIdResto -> '=' Expr ';' | '(' ArgumentosOpt ')' ';'
ArgumentosOpt -> Expr MaisArgumentos | ε
MaisArgumentos -> ',' Expr MaisArgumentos | ε
Expr -> Conjuncao ExprPrime
ExprPrime -> '||' Conjuncao ExprPrime | ε
Conjuncao -> Igualdade ConjuncaoPrime
ConjuncaoPrime -> '&&' Igualdade ConjuncaoPrime | ε
Igualdade -> Relacao IgualdadePrime
IgualdadePrime -> '==' Relacao IgualdadePrime | '!=' Relacao IgualdadePrime | ε
Relacao -> E RelacaoPrime
RelacaoPrime -> '<' E RelacaoPrime | '<=' E RelacaoPrime | '>' E RelacaoPrime | '>=' E RelacaoPrime | ε
E -> T EPrime
EPrime -> '+' T EPrime | '-' T EPrime | ε
T -> Unaria TPrime
TPrime -> '*' Unaria TPrime | '/' Unaria TPrime | '%' Unaria TPrime | ε
Unaria -> '!' Unaria | '-' Unaria | F
F -> '(' Expr ')' | id ChamadaOpt | inteiro | decimal | caractere | texto | 'true' | 'false'
ChamadaOpt -> '(' ArgumentosOpt ')' | ε
```

Precedência, da menor para a maior: `||`, `&&`, `== !=`, `< <= > >=`, `+ -`,
`* / %`, unários `! -`, fator/chamada/parênteses. Cadeias binárias de um mesmo nível
são reconhecidas da esquerda para a direita. Não há cálculo de valores ou árvore neste
checkpoint. A futura construção da AST deverá preservar essa associatividade.

## 3. Transformações e decisões

As formas abaixo são alternativas didáticas de projeto, não um histórico presumido de
versões escritas pelo grupo.

Recursão à esquerda (Aula 4, slides 16–19):

```text
Antes:  E -> E '+' T | E '-' T | T
Depois: E -> T EPrime
        EPrime -> '+' T EPrime | '-' T EPrime | ε
```

A mesma transformação se aplica aos demais níveis binários. As listas usam recursão
à direita e são reconhecidas por laços equivalentes. Nenhum não-terminal alcança a si
mesmo por um prefixo de símbolos anuláveis: as recorrências de listas consomem um item,
as de Prime consomem um operador, `Unaria` consome `!`/`-` e a volta de `F` para `Expr`
passa antes por `(`. Não há recursão à esquerda direta ou indireta.

Fatoração (Aula 5, slides 5–6):

```text
Antes:  ComandoId -> id '=' Expr ';' | id '(' ArgumentosOpt ')' ';'
Depois: ComandoId -> id ComandoIdResto
        ComandoIdResto -> '=' Expr ';' | '(' ArgumentosOpt ')' ';'

Antes:  F -> id | id '(' ArgumentosOpt ')' | outras formas
Depois: F -> id ChamadaOpt | outras formas
        ChamadaOpt -> '(' ArgumentosOpt ')' | ε
```

O `:=` ilustrativo da aula é adaptado ao `=` já definido no CP1.

Conflito do `else` (Aula 4, slide 12; Aula 5, slide 7): com corpo livre `Comando`,
fatorar em `SenaoOpt -> 'else' Comando | ε` não basta. Em um `if` aninhado,
`else` entra em FOLLOW(SenaoOpt), conflitando com FIRST da alternativa não vazia.
A correção da linguagem é exigir `Bloco` delimitado em ambos os braços. Agora `else`
segue o bloco do `if`, mas não segue o `Se` inteiro nem SenaoOpt. O teste de interseção
abaixo fica vazio. Essa escolha restringe a sintaxe e já corresponde ao exemplo do CP1.

## 4. FIRST/FOLLOW e verificação LL(1)

Para manter a tabela curta, as abreviações abaixo representam conjuntos completos:

```text
V  = {int, double, bool, char, string}
D  = V ∪ {fn}
C  = V ∪ {if, while, return, id, '{'}
DG = D ∪ {EOF}
CG = C ∪ {'}'}
A  = {'(', id, inteiro, decimal, caractere, texto, true, false}
X  = A ∪ {'!', '-'}
R0 = {')', ',', ';'}
R1 = R0 ∪ {'||'}
R2 = R1 ∪ {'&&'}
R3 = R2 ∪ {'==', '!='}
R4 = R3 ∪ {'<', '<=', '>', '>='}
R5 = R4 ∪ {'+', '-'}
R6 = R5 ∪ {'*', '/', '%'}
```

O cálculo aplica as regras da Aula 5: FIRST de uma sequência percorre símbolos enquanto
forem anuláveis. FOLLOW recebe FIRST do sufixo sem ε e, quando o sufixo é anulável,
FOLLOW do lado esquerdo. Repetir até não acrescentar elementos. FOLLOW(Programa) inicia
com `$`; como a produção contém EOF explícito, FOLLOW(ListaDeclaracoes) é `{EOF}`.

Na coluna LL(1), “única” significa uma alternativa não anulável, portanto não há pares
de alternativas a comparar. Nas linhas com ε, há exatamente uma alternativa anulável,
e a interseção escrita é entre FIRST das outras alternativas e FOLLOW do não-terminal.
Nas demais linhas, os conjuntos separados indicam FIRST de cada alternativa, todos
disjuntos. Assim as três cláusulas são verificadas para cada produção.

| Não-terminal | FIRST | FOLLOW | Verificação LL(1) |
|---|---|---|---|
| Programa | D | {$} | Única, não anulável. |
| ListaDeclaracoes | D ∪ {ε} | {EOF} | D e ε; D ∩ {EOF} = ∅. |
| Declaracao | D | DG | {fn} e V disjuntos; nenhuma anulável. |
| Funcao | {fn} | DG | Única, não anulável. |
| TipoRetorno | V ∪ {void} | {id} | V e {void} disjuntos; nenhuma anulável. |
| Tipo | V | {id} | Cinco singletons distintos; nenhuma anulável. |
| ParametrosOpt | V ∪ {ε} | {')'} | V e ε; V ∩ {')'} = ∅. |
| Parametro | V | {',', ')'} | Única, não anulável. |
| MaisParametros | {',', ε} | {')'} | {','} e ε; {','} ∩ {')'} = ∅. |
| Variavel | V | DG ∪ CG | Única, não anulável. |
| InicializacaoOpt | {'=', ε} | {';'} | {'='} e ε; {'='} ∩ {';'} = ∅. |
| Bloco | {'{'} | DG ∪ CG ∪ {else} | Única, não anulável. |
| ListaComandos | C ∪ {ε} | {'}'} | C e ε; C ∩ {'}'} = ∅. |
| Comando | C | CG | V, {if}, {while}, {return}, {id}, {'{'} disjuntos; nenhuma anulável. |
| Se | {if} | CG | Única, não anulável. |
| SenaoOpt | {else, ε} | CG | {else} e ε; {else} ∩ CG = ∅. |
| Enquanto | {while} | CG | Única, não anulável. |
| Retorno | {return} | CG | Única, não anulável. |
| RetornoOpt | X ∪ {ε} | {';'} | X e ε; X ∩ {';'} = ∅. |
| ComandoId | {id} | CG | Única, não anulável. |
| ComandoIdResto | {'=', '('} | CG | {'='} e {'('} disjuntos; nenhuma anulável. |
| ArgumentosOpt | X ∪ {ε} | {')'} | X e ε; X ∩ {')'} = ∅. |
| MaisArgumentos | {',', ε} | {')'} | {','} e ε; {','} ∩ {')'} = ∅. |
| Expr | X | R0 | Única, não anulável. |
| ExprPrime | {'\|\|', ε} | R0 | {'\|\|'} e ε; {'\|\|'} ∩ R0 = ∅. |
| Conjuncao | X | R1 | Única, não anulável. |
| ConjuncaoPrime | {'&&', ε} | R1 | {'&&'} e ε; {'&&'} ∩ R1 = ∅. |
| Igualdade | X | R2 | Única, não anulável. |
| IgualdadePrime | {'==', '!=', ε} | R2 | Dois singletons disjuntos e ε; {'==', '!='} ∩ R2 = ∅. |
| Relacao | X | R3 | Única, não anulável. |
| RelacaoPrime | {'<', '<=', '>', '>=', ε} | R3 | Quatro singletons disjuntos e ε; {'<', '<=', '>', '>='} ∩ R3 = ∅. |
| E | X | R4 | Única, não anulável. |
| EPrime | {'+', '-', ε} | R4 | Dois singletons disjuntos e ε; {'+', '-'} ∩ R4 = ∅. |
| T | X | R5 | Única, não anulável. |
| TPrime | {'*', '/', '%', ε} | R5 | Três singletons disjuntos e ε; {'*', '/', '%'} ∩ R5 = ∅. |
| Unaria | X | R6 | {'!'}, {'-'} e A disjuntos; nenhuma anulável. |
| F | A | R6 | Oito singletons distintos; nenhuma anulável. |
| ChamadaOpt | {'(', ε} | R6 | {'('} e ε; {'('} ∩ R6 = ∅. |

Exemplos de propagação: FOLLOW(T) contém `+` e `-` de FIRST(EPrime), além de
FOLLOW(E), porque EPrime é anulável. FOLLOW(F) reúne todos os operadores binários
e R0. ChamadaOpt só aceita vazio nesses seguidores, não diante de outro literal.
O `(` que inicia uma chamada não pertence a R6, por isso não há conflito nessa escolha.
Todos os não-terminais são alcançáveis a partir de Programa e derivam alguma sequência
de terminais. Não há duas alternativas anuláveis em nenhuma produção.

## 5. Tradução e recuperação

`Parser` mantém `scanner` e `lookahead`, inicializa o primeiro token no construtor e
`parse()` chama `parsePrograma()` e devolve boolean. Cada não-terminal tem seu método
`parseX()`. Terminais usam `match()` e escolhas usam FIRST. Métodos opcionais e Prime
só retornam normalmente por ε quando o lookahead pertence a FOLLOW. Erro seguido de
sincronização não equivale a aceitar ε e sempre faz o resultado final ser rejeitado.

Os conjuntos de `Conjuntos.java` são constantes da gramática, não uma tabela que dirige
o parser. `$` teórico é convertido para EOF nas consultas da implementação.
O modelo adapta os métodos `match`, `parseE`, `parseT`, `parseF`, `parseEPrime`,
`parseTPrime` e `sincronizar` da Aula 6 (slides 5–11 e 14–16).

O conjunto usado é `SYNC(A) = FOLLOW(A) ∪ {';', '}', EOF, fn} ∪ FIRST(Comando)`.
As fronteiras de comandos e de função são âncoras de recuperação, para preservar o
próximo comando quando faltar `;` e a próxima função quando faltar `}`. `match()` também
inclui o terminal esperado, para poder consumir esse terminal se for reencontrado.
Quando há uma fronteira próxima conhecida, o chamador acrescenta uma âncora, por
exemplo `{` ao esperar `)` de um `if` ou `while`, `(` ao esperar o nome de uma função,
e `;` ao esperar `)` de uma chamada usada como comando. Isso não muda FIRST/FOLLOW.

`sincronizar()` descarta até SYNC ou EOF e preserva a âncora. O chamador consome um
terminal recuperado, retorna ao contexto externo ou prossegue para o próximo comando.
As listas têm uma guarda de progresso baseada na quantidade de tokens consumidos:
se uma iteração de erro termina na mesma posição, o token não aproveitado é descartado.
ListaComandos devolve EOF ou `fn` ao contexto externo para não engolir a próxima função
quando faltar `}`. Não retorna por ε nesses casos: registra um erro antes de devolver.
O parser pode reportar erros em cascata em entradas muito danificadas, mas não aceita
um programa que precisou de recuperação. Os erros léxicos do scanner também rejeitam
o programa, inclusive quando o CP1 entrega um literal com escape inválido.
Ao retornar por vários níveis sobre o mesmo token, registra-se apenas o primeiro erro
nessa posição. A comparação usa a identidade do Token apenas para essa deduplicação,
não para comparar lexemas: lexemas são comparados com `equals()`.

O caso `04-multiplos-erros.lumen` demonstra duas recuperações independentes:

```text
2:13  int x = ;    -> falta operando, sincroniza em ;, o declarador consome ;
3     x = 1;       -> reconhecido normalmente
4:13  x = 2 + ;    -> falta operando, sincroniza em ;, a atribuicao consome ;
5     x = 3;       -> reconhecido normalmente
6     return;      -> reconhecido normalmente
9–11  outra funcao -> reconhecida na mesma rodada; resultado final rejeitado
```

## 6. Compilação, testes e checklist

Requer JDK 17 ou posterior, sem Maven, JFlex, JUnit ou outras dependências. Na raiz
do projeto, em PowerShell:

```powershell
$fontesCp2 = (Get-ChildItem -Path src -Recurse -Filter *.java).FullName
$testesCp2 = (Get-ChildItem -Path test -Filter *.java).FullName
javac --release 17 -encoding UTF-8 -Xlint:all -d out $fontesCp2 $testesCp2
java -cp out ScannerTest
java -cp out ParserTest
java -cp out parser.Main test/validos/01-cp1.lumen
java -cp out parser.Main test/invalidos/04-multiplos-erros.lumen
```

`parser.Main` sai com código 0 para aceitação, 1 para erro léxico/sintático e 2 para
uso incorreto ou falha de leitura. `scanner.Main` permanece disponível, inalterado.
O diretório `out` é gerado localmente, não faz parte da entrega.

Os três arquivos válidos cobrem o exemplo original do CP1, controle aninhado e funções
com os cinco tipos, parâmetros, chamadas aninhadas e retornos com/sem valor. Os três
primeiros inválidos isolam ausência de `;`, operando e `)`. O quarto testa múltiplos
erros. `ParserTest` também cobre FOLLOW inválido, EOF, delimitadores restantes,
construções não definidas, erros léxicos e listas/expressões longas.

Validação realizada: compilação com `--release 17 -Xlint:all` sem avisos, os dez testes
do scanner preservado e 41 casos do parser aprovados. A tabela escrita e os conjuntos
Java foram comparados com o cálculo independente da BNF. Uma auditoria adicional,
fora dos arquivos da entrega, executou 7.871 entradas geradas, alteradas ou truncadas:
todas terminaram, sem divergência entre o parser e um reconhecedor preditivo da BNF.
Essa auditoria é uma verificação prática, não uma prova para entradas de tamanho ilimitado.

- [x] BNF completa com 38 não-terminais, fatorada e sem recursão à esquerda.
- [x] FIRST/FOLLOW e verificação LL(1) de todos os não-terminais.
- [x] Um método parseX por não-terminal, match e ε condicionado a FOLLOW.
- [x] Scanner e Token do CP1 preservados sem alterações.
- [x] Recuperação em modo pânico com linha/coluna e guarda de progresso.
- [x] Três programas válidos, três inválidos distintos e um com dois erros.
- [x] Reconhecedor sem AST ou análise semântica.
- [x] Log de IA com contribuição efetiva registrada.
- [ ] Revisão individual e explicação oral do código pelos dois autores.

Testes não comprovam domínio oral. Antes do envio, os autores devem conseguir explicar
as escolhas de cada método, as entradas de FIRST/FOLLOW e o retorno após sincronização.

## 7. Referências do material oficial

Números indicam a posição do slide no arquivo, incluindo a capa, não o rodapé.

- `Aula_04_AnaliseSintaticaGLC.pptx`: slides 5–6 (BNF/EBNF), 11–19 (precedência,
  ambiguidade, tradução e recursão), 21–23 (gramática própria e escopo).
- `Aula_05_GramaticasLL(1).pptx`: slides 4–10 (transformações/FIRST), 13–15 (FOLLOW),
  18–22 (condições LL(1)) e 24–26 (verificação por iteração).
- `Aula_06_ParserCompletoLL1.pptx`: slides 5–11 (código do reconhecedor), 12–19
  (erro, sincronização, CP1 inalterado e testes).
- `Aula_07_ArvoreSintaxeAbstrata.pptx`: slide 21 confirma que AST fica fora do CP2.
- `Aula_08_ParsingBottomUp.pptx`: slide 3 mantém descida recursiva no projeto.

As oito aulas foram renderizadas e revisadas slide a slide, inclusive diagramas e
imagens. Os exemplos foram adaptados aos tokens e à sintaxe de Lumen, não usados para
introduzir palavras reservadas, geradores ou etapas que o checkpoint não pede.
