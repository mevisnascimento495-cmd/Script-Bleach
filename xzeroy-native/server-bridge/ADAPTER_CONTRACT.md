# Contrato do adaptador administrativo (servidor próprio)

Este documento define os requisitos para uma futura integração administrativa. Não existe aqui um mecanismo de invasão, bypass de permissões ou elevação de privilégios feita pelo APK.

## Requisito de autoridade

O servidor deve ser configurado pelo proprietário para reconhecer uma identidade autorizada. A decisão final precisa acontecer no lado do servidor, usando um identificador estável verificado pela autenticação do próprio servidor (por exemplo, XUID de uma sessão autenticada), não um nome que o cliente afirma ser seu.

## Operações futuras

Um adaptador para o software exato do servidor pode implementar ações estreitas e auditáveis, como:
- consultar se a conta permitida está autenticada;
- executar comandos administrativos aceitos pelo software do servidor somente para a conta em lista de permissão;
- revogar a permissão, registrar data/hora, identidade e ação, e recusar por padrão qualquer erro.

O adaptador não deve oferecer um endpoint genérico de execução de comandos arbitrários. Segredos de servidor devem ficar no servidor e nunca no APK. Todo pedido precisa ser autenticado, limitado por taxa e registrado.

## Por que ainda está pendente

Bedrock Dedicated Server oficial, PocketMine-MP, Nukkit e outros forks têm APIs e mecanismos de extensão diferentes. Sem alvo específico não há como afirmar que um plugin funcione, nem é seguro inventar uma API universal. O bridge atual responde HTTP 501 para ações administrativas até a implementação e os testes contra o servidor autorizado.

## Testes de aceitação

1. Uma identidade não listada recebe negação.
2. Uma sessão não autenticada recebe negação.
3. Uma identidade permitida pode executar somente as ações autorizadas pelo proprietário.
4. Reinício, desconexão ou troca de identidade não deixa a autorização aberta.
5. Falha do adaptador, segredo ausente ou configuração inválida resulta em negação.
6. Todos os eventos administrativos ficam auditáveis.
