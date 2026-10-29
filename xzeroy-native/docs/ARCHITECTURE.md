# XZeroy Native — arquitetura após o rework

## Objetivo e referências

O rework toma como referência a separação de responsabilidades visível nos projetos públicos [ProtoHax](https://github.com/hax0r31337/ProtoHax) e [Project Lumina / LuminaClient](https://github.com/TheProjectLumina/LuminaClient): catálogo de módulos por categoria, valores/configuração separados da UI, eventos de estado e uma camada de interface que pode evoluir sem se misturar ao motor de jogo.

Este repositório mantém implementação própria nesta etapa; não copia arquivos-fonte, recursos visuais ou código dos projetos de referência. Inspiração arquitetural não significa equivalência funcional.

## Componentes atuais

- **MainActivity** — painel Android, conta Microsoft e preferências do usuário.
- **XzeroyModuleSystem.kt** — registro central de módulos, categorias Combat/Movement/Visual/Misc, definições de valores, loja de estados e eventos entre o painel e a sobreposição.
- **XzeroyAccessibilityService** — sobreposição nativa e backend Android de gestos. Usa o mesmo registro do painel.
- **XzeroyProfileStore** — perfis locais versionados em JSON; restaura apenas chaves conhecidas e valida limites numéricos.
- **server-bridge** — contrato HTTP do protótipo; ainda não constitui integração com o protocolo Bedrock.

## O que funciona nesta versão

- Estado persistido de Auto Clicker e Auto Jump.
- Configuração de coordenadas e intervalos com limites validados.
- Categorias de módulos compartilhadas entre o painel e a sobreposição.
- Configuração por toque duplo no nome do módulo funcional.
- Ativar/desativar módulos funcionais a partir do painel ou sobreposição e atualizar a interface via eventos.
- Salvar, listar, carregar e excluir perfis locais.
- Módulos sem backend aparecem como não implementados e não executam ações fictícias.

## O que ainda não está integrado

O motor de relay de pacotes, a sessão Bedrock real, os codecs por versão, autenticação Xbox/XSTS/Minecraft, o estado de entidades/chunks e renderização de ESP ainda precisam ser integrados e testados separadamente. O login Microsoft básico já presente nesta app não equivale a uma sessão válida no Xbox Live ou no Minecraft. Não se deve tratar o XZeroy atual como equivalente ao ProtoHax/Lumina em funcionalidades de rede.

## Próxima camada de engenharia

1. Adicionar um módulo de engine separado da UI e do serviço Android.
2. Implementar uma sessão Bedrock version-aware sobre uma biblioteca de protocolo mantida, com estados de conexão explícitos.
3. Introduzir GameSession, eventos tipados de pacotes e um ciclo de vida que feche canais e loops de rede corretamente.
4. Conectar cada módulo a eventos/capacidades reais da sessão; manter indisponíveis os módulos que precisem de estado ainda inexistente.
5. Testar Android em dispositivo e validar autenticação e compatibilidade de protocolos antes de marcar qualquer função como funcional.
