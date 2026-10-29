# XZeroy — plano de rework estrutural

## Objetivo

Reconstruir o XZeroy como um client Android modular para Minecraft Bedrock, inspirado nos conceitos arquiteturais públicos do ProtoHax e do Lumina, sem copiar a identidade visual, o código proprietário de terceiros ou recursos que não possam ser testados corretamente. O alvo de validação inicial é um servidor próprio/de teste ou um ambiente em que o operador tenha autorizado o uso.

## Diagnóstico do protótipo atual

- A UI está concentrada em uma Activity Android tradicional, com navegação e componentes criados manualmente.
- O relay está encapsulado em `XzeroyRelayController`, mas a UI e o motor ainda têm acoplamento por preferências e eventos.
- Os módulos Android de Acessibilidade (Auto Clicker/Auto Jump) são automações de toque por coordenadas; não leem entidades nem estado interno do jogo.
- O relay usa a dependência ProtoHax publicada separadamente. Compatibilidade de protocolo com versões atuais do Bedrock ainda precisa ser demonstrada em testes reais.
- A tela de conta Microsoft, por si só, não autentica uma sessão Xbox Live/XSTS/Minecraft.
- Funções que não têm implementação real devem continuar identificadas como indisponíveis; não devem ser simuladas.

## Arquitetura-alvo

1. **app-ui** — interface nativa em Compose, navegação por Início, Módulos, Sessão/Relay, Conta e Configurações; tema próprio XZeroy, cards consistentes, busca, favoritos e painel de detalhes de cada módulo.
2. **core-api** — contratos de módulo, categorias, estado, configuração tipada, ciclo de vida e eventos. A UI não deve importar classes internas do relay.
3. **module-runtime** — registro único de módulos, validação de configurações, ativação/desativação, permissões necessárias e indicação explícita de disponibilidade.
4. **bedrock-transport** — relay UDP/RakNet, ciclo de sessão, codecs e encaminhamento de pacotes isolados atrás de interfaces. Versões de protocolo devem ser detectadas/registradas e testadas; falha de negociação deve parar a sessão com diagnóstico claro.
5. **game-state** — projeção de estado apenas a partir de dados realmente observados e decodificados; não afirmar coordenadas, entidades, inventário ou atributos se o pacote não fornecer esses dados.
6. **overlay** — controles flutuantes e painéis que respeitem o ciclo de vida Android e possam ser desligados sem deixar serviços pendurados.
7. **auth** — fluxo de conta separado do relay; nenhum token ou segredo deve ser gravado em logs ou incluído no APK.
8. **server-bridge** — integração opcional com servidor de teste próprio, com autenticação e autorização no servidor. Um segredo no cliente nunca deve ser tratado como prova de privilégio administrativo.
9. **tests** — testes unitários para configurações/módulos e testes de integração do relay com servidor local controlado, além de build APK em CI.

## Regras para a reconstrução

- Trabalhar na branch `xzeroy-total-rework`; não substituir a branch existente até haver build e revisão.
- Uma alteração por commit, com mensagens que descrevam a mudança.
- Não declarar um módulo funcional sem implementação e teste reproduzível.
- Não implementar bypass de anticheat, roubo de credenciais ou alteração não autorizada de servidores. Recursos de gameplay devem ser validados em ambiente controlado/autorizado.
- Verificar a licença e manter os avisos/atribuições de qualquer dependência ou código derivado.
- Não usar uma cópia literal da UI, marca ou ativos do Lumina/ProtoHax; usar os padrões arquiteturais como referência.

## Fases

### Fase 1 — base confiável
- Fixar uma branch de trabalho.
- Auditar build, dependências, manifest, relay, módulos e workflows.
- Separar claramente funcionalidades implementadas, experimentais e ausentes.

### Fase 2 — contratos e runtime de módulos
- Definir contratos tipados para módulos e suas configurações.
- Remover a dependência direta da UI em classes internas do relay.
- Adicionar testes para ativação, persistência e validação.

### Fase 3 — interface reconstruída
- Migrar para uma UI declarativa com navegação, pesquisa, categorias, favoritos e configuração por módulo.
- Implementar painel flutuante com ciclo de vida seguro.

### Fase 4 — transporte e compatibilidade
- Isolar a implementação de relay.
- Testar handshake, desconexão, timeouts, erros de versão e encaminhamento em servidor controlado.
- Não anunciar compatibilidade com uma versão do Bedrock sem teste.

### Fase 5 — autenticação e integração
- Separar identidade Microsoft de autenticação Xbox/Minecraft.
- Implementar bridge somente com validação no lado do servidor e permissões explícitas.

### Fase 6 — validação de release
- Build limpo no CI, testes automatizados, instalação em Android e teste de sessão controlada.
- Publicar artefato somente após confirmar o workflow e os testes.
