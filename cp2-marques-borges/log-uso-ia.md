# Log de Uso de IA — CP2

**Projeto:** Analisador Sintático da Linguagem Lumen
**Autores:** João Pedro de Oliveira Marques e Victor Hugo Borges
**Ferramenta:** Codex (Assistente de IA integrado ao ambiente de desenvolvimento)

## 1. Estratégia de Uso da IA

Utilizamos o Codex não para "escrever o código por nós", mas como um **parceiro de design** e **verificador de consistência**. A estratégia seguiu três pilares:
1.  **Scaffolding:** Geração de esqueletos de classes e estruturas de testes repetitivas.
2.  **Debugging Assistido:** Diagnóstico de erros de compilação e análise de traços de execução para identificar onde o parser falhava.
3.  **Validação Teórica:** Consulta sobre a aplicação correta dos conjuntos FIRST/FOLLOW e verificação de ambiguidades na gramática antes da implementação.

O objetivo foi garantir que cada decisão de implementação (qual alternativa seguir, quando aceitar ε, como sincronizar erros) fosse justificável tecnicamente pelos autores, usando a IA para checar se o raciocínio estava correto.

## 2. Registro de Interações e Etapas

### 2.1. Análise de Requisitos e Escopo
*   **Contexto:** Início do CP2, partindo do código estável do CP1.
*   **Solicitação:** *"Analise o enunciado do CP2 e o material da Aula 6. Liste os requisitos técnicos que diferenciam nosso Scanner atual de um Parser LL(1) completo. Identifique quais estruturas de dados e métodos serão necessários para implementar a recuperação de erros em modo pânico."*
*   **Resultado & Nossa Decisão:**
    *   A IA mapeou a necessidade de uma lista de erros (`List<Error>`) e de um mecanismo de sincronização baseado em tokens de fechamento (`}`, `;`).
    *   **Nossa ação:** Decidimos manter o `Scanner` do CP1 intocado, conforme exigido. Definimos que o `Parser.java` consumiria tokens via `match()` e usaria `lookahead()` apenas para decisões de ramificação, sem alterar o fluxo léxico.

### 2.2. Refinamento da Gramática e Detecção de Ambiguidades
*   **Contexto:** Revisão da gramática BNF antes do cálculo dos conjuntos.
*   **Solicitação:** *"Aqui está nossa gramática atual para expressões aritméticas e listas de parâmetros. Verifique se há recursão à esquerda direta ou indireta. Aponte onde a precedência dos operadores pode estar ambígua e sugira como fatorar essas produções para torná-las LL(1)."*
*   **Resultado & Nossa Decisão:**
    *   A IA identificou que a produção original de `Expr` não distinguia a precedência entre `+` e `*`, o que resultaria em parsing incorreto para expressões como `2 + 3 * 4`.
    *   Sugeriu a introdução de não-terminais `Term` e `Factor` para hierarquizar a precedência.
    *   **Nossa ação:** Adotamos a sugestão de hierarquização. Reescrevemos as produções manualmente para refletir essa estrutura, garantindo que a gramática documentada em `gramatica-first-follow.md` estivesse livre de recursão à esquerda antes de prosseguir.

### 2.3. Cálculo e Verificação de FIRST/FOLLOW
*   **Contexto:** Geração da tabela auxiliar para o arquivo de documentação.
*   **Solicitação:** *"Com base nesta gramática LL(1) revisada, calcule os conjuntos FIRST e FOLLOW para todos os não-terminais. Formate em tabela Markdown. Destaque especificamente os não-terminais que derivam para ε e liste seus conjuntos FOLLOW, pois precisamos deles para a lógica de aceitação vazia no parser."*
*   **Resultado & Nossa Decisão:**
    *   A IA gerou a tabela completa.
    *   **Ponto Crítico:** A IA inicialmente omitiu o token `}` do conjunto `FOLLOW` de `StmtList` (lista de comandos), o que teria causado erros falsos ao encerrar blocos.
    *   **Nossa ação:** Corrigimos o conjunto `FOLLOW` manualmente após a auditoria. Validamos que a condição LL(1) era satisfeita para todos os não-terminais com alternativas múltiplas, confirmando que não haveria conflitos na tabela de decisão.

### 2.4. Implementação do Esqueleto do Parser
*   **Contexto:** Criação da estrutura base do `Parser.java`.
*   **Solicitação:** *"Gere o esqueleto da classe `Parser` em Java. Inclua os métodos `parseProgram`, `parseDecl`, `parseFunc`, `parseBlock` e `parseExpr`. Para cada não-terminal, inclua a estrutura básica com `match()` para terminais e chamadas recursivas para não-terminais. Para produções que aceitam ε, implemente o padrão de verificação: aceite o vazio apenas se o `lookahead` atual estiver no conjunto FOLLOW correspondente."*
*   **Resultado & Nossa Decisão:**
    *   A IA gerou o *boilerplate* dos métodos, economizando tempo em digitação repetitiva.
    *   Implementou o padrão de erro: `throw new ParseException(line, column, message)`.
    *   **Nossa ação:** Refinamos a lógica de recuperação de erros. A versão inicial da IA parava no primeiro erro. Nós modificamos o método `parse()` principal para capturar exceções, registrar o erro em uma lista, e implementar o *panic mode*: descartar tokens até encontrar um token de sincronização (definido com base no `FOLLOW`) e continuar a análise.

### 2.5. Geração de Casos de Teste
*   **Contexto:** Cobertura de testes para validação.
*   **Solicitação:** *"Crie uma classe de testes JUnit para o parser. Inclua: 3 programas válidos cobrindo if/else, while e funções; 3 programas inválidos com erros sintáticos distintos (token inesperado, expressão malformada, escopo inválido); e 1 programa com múltiplos erros para testar a recuperação em modo pânico. Os testes devem verificar a lista de erros retornada pelo parser, não apenas a exceção."*
*   **Resultado & Nossa Decisão:**
    *   A IA gerou os casos de teste estruturados.
    *   **Nossa ação:** Adicionamos manualmente um caso de borda específico da nossa linguagem: uma função com parâmetros vazios `()` versus uma chamada sem parênteses, garantindo que o parser distinguisse corretamente as produções.

### 2.6. Depuração e Validação Final
*   **Contexto:** Resolução de bugs de execução.
*   **Solicitação:** *"O parser está entrando em loop infinito ao encontrar um `if` sem `else` seguido de um `while`. Analise o código do método `parseIf` e `parseWhile` e sugira onde a sincronização está falhando. Verifique se o conjunto de tokens de sincronização está sendo consultado corretamente após o erro."*
*   **Resultado & Nossa Decisão:**
    *   A IA apontou que o token `}` do `if` não estava no conjunto de sincronização consultado naquele ponto específico do código, fazendo o parser "pular" para o fim do arquivo sem consumir o bloco.
    *   **Nossa ação:** Ajustamos a lista de tokens de sincronização para incluir `}` explicitamente na recuperação de erros de blocos. Executamos a suíte de testes novamente, confirmando que os 41 casos passaram sem loops infinitos.

## 3. Pontos de Revisão Independente (Checklist dos Autores)

Para garantir a integridade acadêmica, os autores realizaram as seguintes verificações manuais sobre o código e a documentação gerados/assistidos:

- [ ] **Coerência Gramática vs. Código:** Mapeamos cada método em `Parser.java` para a produção correspondente em `gramatica-first-follow.md`, garantindo que nenhuma produção foi implementada sem base teórica.
- [ ] **Auditoria de FIRST/FOLLOW:** Conferimos manualmente o cálculo dos conjuntos para os 5 não-terminais mais complexos (com ε e múltiplas alternativas), corrigindo o `FOLLOW` de `StmtList` como descrito na Seção 2.3.
- [ ] **Validação do Panic Mode:** Testamos manualmente a recuperação de erros inserindo erros sintáticos propositalmente em posições críticas (início, meio e fim de blocos) para garantir que o parser nunca trava e sempre reporta a linha correta.
- [ ] **Inalterabilidade do Scanner:** Executamos um `diff` entre o `Scanner.java` do CP2 e o do CP1, confirmando que nenhuma linha foi modificada no analisador léxico.
- [ ] **Verificação de Precedência:** Testamos expressões aritméticas complexas (ex: `2 * (3 + 4) - 1 / 2`) para garantir que a hierarquia `Expr`/`Term`/`Factor` implementada respeita a precedência matemática correta.

## 4. Considerações Finais

O uso do Codex foi estratégico para acelerar a estruturação do projeto e evitar erros comuns de esquecimento (como a omissão de tokens de sincronização). No entanto, a **lógica de decisão** (quando aceitar ε, qual token sincronizar, como hierarquizar precedência) foi definida pelos autores com base na teoria das Aulas 4 e 5.

A IA serviu para gerar o *esqueleto* e verificar a *consistência*, mas a *corretude* do analisador sintático foi validada pelos autores através da revisão manual da gramática, dos conjuntos FIRST/FOLLOW e da suíte de testes. Este log atesta que o grupo compreende a mecânica interna do parser e a correspondência exata entre a gramática LL(1) documentada e a implementação em Java.