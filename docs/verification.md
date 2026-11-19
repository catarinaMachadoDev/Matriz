# Matriz ao vivo — do pedido à tarefa concluída

## Guião da demonstração · 6 minutos

**Antes de partilhar o ecrã.** Abre a [aplicação local](http://localhost:8081) com os dados de demonstração. Deixa uma janela normal no início de sessão e, numa janela privada, entra como Sam. Confirma que ambas as contas têm acesso a *Website launch*. Partilha uma janela de cada vez e mantém este guião nas tuas notas. As credenciais e as instruções de arranque estão no [README](../README.md).

### 0:00–0:45 · Apresentar o problema

**Diz:** «Imaginem que vamos lançar um site amanhã. Falta confirmar se o formulário de contacto funciona. Vou mostrar-vos como a Catarina pede essa verificação, como o Sam lhe responde e como sabemos, no fim, que o trabalho ficou feito.»

**Mostra:** Entra como Catarina. No cartão *Website launch*, aponta para a indicação *Managing*. Abre o projeto e mostra os nomes da Catarina e do Sam em *People*.

### 0:45–2:00 · Criar a tarefa e mostrar uma dificuldade real

**Mostra:** Em *Status*, escolhe *Done* e carrega em *Apply filters*. Mantém esse filtro ativo enquanto preenches *Create task*.

- **Title:** Testar o formulário de contacto
- **Description:** Enviar uma mensagem de teste e confirmar que chega à caixa de correio da equipa.

Carrega em *Create task*. Faz uma pausa para que se veja o título na lista, com o estado *To do* e o filtro novamente em *All statuses*.

**Diz:** «Durante o desenvolvimento, este filtro deu-nos uma surpresa. A tarefa ficava guardada, mas quem a criava continuava a ver apenas as tarefas concluídas. Parecia que o botão tinha falhado. Agora, quando a criação é confirmada, a lista volta à primeira página e mostra a tarefa que acabei de criar.»

### 2:00–3:00 · Passar o trabalho ao Sam

**Mostra:** Abre a nova tarefa. Em *Assignees*, seleciona o Sam e carrega em *Assign member*. Muda para a janela privada. Na lista de projetos do Sam, aponta para *Team project*, abre *Website launch* e seleciona a tarefa pelo título.

**Diz:** «Agora estão a ver a sessão do Sam. A Catarina gere este projeto e o Sam participa nele. O Sam consegue abrir a tarefa, ler o pedido e ver que lhe foi atribuída. É aqui que vai deixar o resultado.»

### 3:00–4:30 · Registar o resultado

**Mostra:** Na sessão do Sam, muda *Status* para *In progress* e carrega em *Save changes*. Em *Add a comment*, escreve «Mensagem de teste recebida na caixa da equipa.» e carrega em *Post comment*. Aponta para o nome do autor. Por fim, muda o estado para *Done* e guarda.

**Diz:** «Para esta demonstração, vamos supor que a mensagem de teste chegou. O comentário explica o que o Sam verificou. Ao marcar a tarefa como concluída, ele atualiza também o progresso do projeto.»

### 4:30–6:00 · Fechar o circuito

**Mostra:** Volta à janela da Catarina e atualiza a página da tarefa. Mostra *Done* e o comentário do Sam. Usa a ligação *Website launch* para voltar ao projeto e aponta para a contagem de tarefas concluídas. Termina com *Sign out*.

**Diz:** «Começámos com uma dúvida concreta sobre o lançamento. Agora a Catarina consegue ver quem fez a verificação, o que confirmou e em que estado ficou a tarefa. A alteração que o Sam guardou na sessão dele aparece também na sessão da Catarina.»

**Se perguntarem como foi testado:** «Usámos testes unitários para regras como o limite do título, testes de integração com PostgreSQL para verificar o que a API guarda e quem pode consultar esses dados, e um teste em Chromium para percorrer a criação de uma tarefa pela interface.» Os resultados e as restantes dificuldades estão no anexo.

---

## Anexo técnico — verificação do projeto

A verificação local decorreu a 15 de novembro de 2026, com Java 21, Maven Wrapper 3.9.16, PostgreSQL 17, Chromium através do Playwright para Java e Docker Compose v2.

| Verificação | Resultado observado |
| --- | --- |
| Testes unitários em Java | Passaram os três testes. Confirmou-se a normalização de um título válido, a rejeição de um título em branco e o comportamento no limite de comprimento. Um título com 120 caracteres foi aceite e um com 121 foi rejeitado. |
| Testes de integração da API com PostgreSQL | Passaram os nove testes. Abrangeram a autenticação, a proteção contra CSRF, o encerramento de sessão, a criação e consulta de tarefas, a validação dos dados, o isolamento entre projetos, os perfis de acesso, os conflitos de versão, a atribuição de tarefas, os comentários, a eliminação em cascata, os filtros e as contagens. |
| Teste de ponta a ponta em Chromium | O teste passou. A Catarina iniciou sessão, abriu um projeto, criou uma tarefa e confirmou que esta aparecia na lista ao regressar ao projeto. O teste usou uma porta escolhida aleatoriamente e, no final, eliminou o projeto que tinha criado com um nome único. |
| Empacotamento da aplicação | Numa cópia temporária limpa do projeto, o Maven Wrapper gerou o ficheiro JAR executável da aplicação Spring Boot. |
| Configuração e construção da imagem Docker | A configuração do Compose passou na validação. Os dois contentores arrancaram e passaram nas verificações de estado. O Flyway estabeleceu ligação ao PostgreSQL e validou a migração. |
| Controlo de acesso com sessões independentes | A Catarina criou uma tarefa através da API da aplicação em execução no Compose. Noutra sessão, Taylor recebeu a resposta HTTP 404 tanto ao consultar a tarefa como ao tentar alterá-la. A tarefa manteve os dados originais. |
| Persistência dos dados | Depois de os dois contentores terem sido recriados, a Catarina voltou a iniciar sessão e conseguiu consultar a mesma tarefa, guardada no volume do PostgreSQL. No final, o ambiente temporário do Compose foi removido. |
| Verificação local antes da entrega | A análise do contrato OpenAPI identificou 21 operações, confirmou os parâmetros de caminho obrigatórios e resolveu as 122 referências. O Git ignora os ficheiros de ambiente e de credenciais locais. Estes também ficam excluídos do contexto de construção da imagem Docker. |
| Integração contínua no GitHub Actions | O fluxo de trabalho está configurado para envios de alterações e pedidos de integração. Para o commit `92ddfdf`, os 13 testes passaram e a imagem foi construída. A configuração de integração contínua inclui também um teste básico de funcionamento do contentor com ligação à base de dados. [Execução 43774443610](https://github.com/catarinaMachadoDev/Matriz/actions/runs/43774443610) |

## Como reproduzir a verificação

1. Preparar o Java e o Docker de acordo com o [README](../README.md). Usar a base de dados PostgreSQL dedicada aos testes, instalar o Chromium, definir a variável de ambiente `MATRIZ_E2E=true` e executar a fase `verify` com o Maven Wrapper.
2. Num ambiente temporário do Docker Compose, iniciar sessão como Catarina e criar uma tarefa no projeto `Website launch`. Numa sessão independente, entrar como Taylor e confirmar que o acesso à tarefa devolve HTTP 404.
3. Recriar os dois contentores, preservando o volume do PostgreSQL. Voltar a iniciar sessão como Catarina e confirmar que a tarefa mantém os dados originais. Consultar o estado dos contentores e os registos recentes conforme descrito no README.

## Dificuldades encontradas durante o projeto

- **Tarefa criada mas invisível na lista.** Um filtro de estado ou prioridade podia ocultar a nova tarefa. O mesmo acontecia se o utilizador estivesse noutra página da lista. Depois de a API confirmar a criação, a interface limpa estes filtros, volta à primeira página e atualiza a lista.
- **Encerramento de uma sessão já expirada.** Quando a sessão já tinha expirado no servidor, a tentativa de terminar sessão devolvia HTTP 401 e o utilizador via uma mensagem de erro. A interface passou a reconhecer essa resposta, a limpar a sessão local e a regressar ao ecrã de início de sessão.
- **Dois campos de descrição no teste de navegador.** Na primeira execução, o Playwright encontrou tanto a descrição do projeto como a da tarefa. O teste passou a identificar o campo da tarefa pelo texto exato da respetiva etiqueta.
- **Falha na construção da imagem em Linux.** O Maven Wrapper selecionava o arquivo `.tar.gz`, mas a soma de verificação configurada correspondia ao ficheiro ZIP. A instalação de `unzip` na etapa de construção permitiu ao Wrapper descarregar e validar o ZIP previsto.
- **Ficheiro JAR bloqueado no Windows.** A aplicação em execução mantinha o JAR aberto e impedia a sua substituição, mesmo depois de os 13 testes terem passado. O empacotamento foi confirmado numa cópia limpa. Para repetir a operação na mesma pasta, é preciso encerrar primeiro a instância que está a usar esse JAR.

## Utilização de IA e decisões humanas

A responsável pelo projeto escolheu Java com Spring Boot e PostgreSQL, aprovou o alargamento do âmbito do produto mínimo viável e manteve os limites de validação acordados. A IA ajudou a transformar essas decisões em testes, incluindo os casos de 120 e 121 caracteres no título. A execução em Chromium revelou a ambiguidade na seleção do campo de descrição e levou à correção do teste.

A IA também ajudou a preparar o contentor da aplicação e o serviço PostgreSQL. As verificações locais com Docker Compose confirmaram o arranque dos serviços, o controlo de acesso e a persistência dos dados.

A [revisão de qualidade e segurança](quality-security-review.md) complementa este relatório. A execução do GitHub Actions indicada na tabela documenta os resultados do commit identificado. As alterações posteriores exigem uma nova execução antes da entrega.
