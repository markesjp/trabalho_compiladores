# Log de uso de IA

**Projeto:** Analisador léxico da linguagem Lumen  
**Autores:** João Pedro de Oliveira Marques e Victor Hugo Borges  
**Ferramenta:** Assistente de IA no Codex  
**Entrega:** `markesjp/trabalho_compiladores`, a partir da revisão `analyzerLEXICO`.

## Escopo da participação

A IA participou da elaboração da versão de base e da revisão desta entrega, incluindo implementação do scanner, classes de suporte, AFDs, testes e documentação. A assistência foi abrangente: não se limitou a dúvidas de sintaxe, boilerplate ou componentes isolados.

Os registros abaixo descrevem solicitações efetivamente presentes na conversa e sua aplicação no projeto. Não incluem prompts inventados nem atribuem ao grupo autoria exclusivamente humana do código. A aprovação dos testes não comprova domínio individual para a defesa oral; os autores continuam responsáveis por estudar e explicar a implementação.

## 1. Desenvolvimento da versão de base

**Tipo de interação:** Implementação assistida e documentação.

**Pedido registrado:**  
> Faa o trbalho respeitando as regas  e depois gere um dc explicanxo o que foi feito e o checklist de tsrefas

**Resultado e aplicação:**  
Foi solicitado o desenvolvimento do trabalho a partir do enunciado. A versão de base reunia especificação de Lumen, scanner manual Java, classes de tokens e diagnósticos, diagramas, exemplos e testes. Esse conjunto foi o ponto de partida da cópia revisada.

**Justificativa:**  
Organizar os entregáveis do checkpoint e separar leitura do arquivo, reconhecimento de tokens e registro de erros. A participação no desenvolvimento do scanner é declarada, sem classificá-la apenas como suporte de sintaxe.

## 2. Preservação e análise das referências

**Tipo de interação:** Revisão técnica e organização do projeto.

**Pedido registrado:**  
> guarde o progresso atual desse trabalho, crie uma nova pasta "analyzerLEXICO" clone o trabalho atual para dentro dela utilize esse material zip de referencia para revisar e deixar mais realista a implementação do trabalho utilizando o powerpoints como referencia de código e de exploração no momento de implementação alem de conteudo

**Resultado e aplicação:**  
Backup do estado anterior, cópia local sem pasta Git e leitura das seis apresentações fornecidas. A Aula 2 orientou o scanner manual; as outras aulas contextualizaram tokens, estados léxicos e a futura integração com o parser.

**Justificativa:**  
Preservar o trabalho anterior e relacionar implementação e conteúdo da disciplina. A linguagem Lumen foi mantida; JFlex e parser não foram acrescentados. A referência principal foi a Aula 2, slides 15–28 e 31–37: reconhecedores, tabela de palavras reservadas, estados, cursor e testes.

## 3. Revisão da implementação

**Tipo de interação:** Implementação assistida do scanner e classes de suporte.

**Pedido registrado:**  
> Para deixar o melhor possivel assim que analisar pode modificar o analyzerLEXICO para corroborar com o conteudo

**Resultado e aplicação:**  
A IA reestruturou os reconhecedores com enums e `switches` para identificadores, números, strings, caracteres, operadores e comentários. Atualizou os tipos específicos de token, criou `CategoriaToken` e manteve lexema original e posição de abertura.

A classificação de palavras reservadas usa `Map<String, TipoToken>` e `KEYWORDS.getOrDefault(lexeme, TipoToken.ID)`, após reconhecer o identificador completo. A leitura usa `Files.readString` em UTF-8, com tratamento de `IOException` e `InvalidPathException` em `try/catch`.

Também foram incorporados descarte de strings/chars inválidos, recuperação iterativa, controle de CRLF, EOF explícito e modo opcional `--trace`. Os diagramas foram ajustados aos nomes dos estados do código.

**Justificativa:**  
Tornar os estados identificáveis, aplicar maximal munch sem consumir lookahead indevido e fornecer tipos específicos para as próximas etapas. A intervenção abrange o conjunto do scanner, não apenas uma rotina isolada.

## 4. Testes e documentação da revisão

**Tipo de interação:** Validação automatizada e documentação técnica.

**Pedido de origem:**  
A solicitação do registro 3. Não houve um prompt independente para cada teste ou arquivo.

**Resultado e aplicação:**  
A IA ampliou a suíte para 35 testes, incluindo exploração determinística de 2.000 entradas e sequências longas. A compilação usa `javac -encoding UTF-8 -Xlint:all --release 17`. Os testes passaram; os comandos e resultados estão em [test/evidencia-execucao.txt](test/evidencia-execucao.txt).

Foram atualizados especificação, desenhos e README, além de exemplos de exploração e erro e roteiro para estudo da defesa oral. O projeto também foi compilado e testado a partir de uma extração independente do ZIP. Os documentos auxiliares e exemplos separados não integram o pacote final; as instruções de execução estão na especificação e o trecho realista está na suíte.

**Justificativa:**  
Conferir categorias, tipos específicos, maximal munch, posições, comentários, escapes, recuperação e EOF. A exploração amplia a cobertura, mas não prova ausência de todos os defeitos nem capacidade de defesa dos autores.

## 5. Conferência e correção do log

**Tipo de interação:** Revisão documental.

**Pedidos registrados:**  
> o log de ia esta de acordo com o projeto?

> corrija o log de ia, o analyzerLEXICO ficou muito bom quero ele no jeito, coerente para envio

**Resultado e aplicação:**  
Comparação do log com a implementação e a evidência de testes. Foram retirados nove exemplos fictícios e descrições desatualizadas sobre `Set<String>`, tokens genéricos e `main throws IOException`. O log passou a conter somente registros verificáveis, com descrições correspondentes à versão atual.

**Justificativa:**  
Evitar que modelos ilustrativos sejam apresentados como interações realizadas ou confundidos com o código entregue.

## 6. Correção visual dos autômatos

**Tipo de interação:** Correção dos diagramas e validação visual.

**Pedido registrado:**  
> as imagens dos automatos estão com labels ruins sobrepondo o desenho, corrija as labels, e so aceite se estiverem corretas

**Resultado e aplicação:**  
A IA reorganizou os dois SVGs para separar labels, curvas e estados, preservando os nomes e as transições do código. A primeira renderização ainda apresentou conflito e foi rejeitada. As posições foram ajustadas até a verificação automática aprovar as 41 labels, com margem de 10 px para linhas, bordas e setas no tamanho original.

As duas imagens foram inspecionadas visualmente, e os PNGs foram gerados dos SVGs aprovados. A entrega mantém apenas os SVGs para evitar duplicação; as capturas e a conferência visual ficaram fora do repositório. Não houve alteração nos fontes Java.

**Justificativa:**  
Garantir leitura dos rótulos sem esconder transições ou alterar o comportamento representado pelos AFDs.

## Responsabilidade dos autores

Antes da entrega, cada autor deve conseguir explicar os estados e transições, o cursor, a consulta à tabela de palavras reservadas, maximal munch, posições, recuperação de erros e os testes. Este registro não comprova uma defesa já realizada.

O enunciado, seção 7, exige declarar a geração assistida do scanner; a Aula 2, slide 37, a apresenta como vedada. Este log declara o uso efetivo de IA. Essa diferença de orientação deve ser confirmada com o docente; a declaração não equivale a aprovação acadêmica.
