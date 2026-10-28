# Arquitetura do XZeroy Native

## Componentes
1. App Android: interface e configurações locais.
2. Bridge: API autenticada para encaminhar pedidos autorizados.
3. Adaptador do servidor (pendente): extensão separada, compatível com o software/versão reais do servidor, que verifica identidade no lado do servidor e executa somente ações permitidas.

## Segurança e limites
- Um cliente não pode conceder a si próprio permissões administrativas.
- Um nome de jogador enviado pelo app não comprova identidade.
- Segredos administrativos nunca devem ser embutidos no APK.
- Sem adaptador válido, nenhuma ação de jogo é executada.
- Não usar para contornar permissões ou proteções de servidores de terceiros.

## Rotas provisórias
- `GET /health`: estado do processo.
- `GET /v1/modules`: lista módulos declarados no protótipo.
- `POST /v1/admin/creative`: atualmente HTTP 501.
- `POST /v1/admin/give`: atualmente HTTP 501.

A integração Minecraft ainda não existe. O adaptador depende de saber qual software e versão o servidor autorizado usa e deve utilizar a API suportada por esse servidor.
