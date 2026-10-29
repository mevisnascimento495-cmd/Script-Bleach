# XZeroy Native — arquitetura após o rework

## Referências

- [ProtoHax](https://github.com/hax0r31337/ProtoHax): relay UDP/RakNet de Bedrock, sessões, listeners de pacotes, estado de jogo e módulos.
- [LuminaClient](https://github.com/TheProjectLumina/LuminaClient): separa rede/protocolo (Lunaris), UI nativa (Pixie) e bibliotecas do protocolo. O XZeroy usa a separação como referência, sem copiar a interface do Lumina.

## Componentes

- **MainActivity** — painel Android Início/Módulos/Relay/Conta/Ajustes.
- **XzeroyModuleSystem.kt** — registro central por categorias, preferências, perfis e eventos de estado.
- **XzeroyAccessibilityService** — sobreposição Android de gestos; Auto Clicker e Auto Jump usam Acessibilidade.
- **XzeroyRelayController.kt** — inicia/encerra o relay UDP, instala listeners de protocolo/sessão e aplica estados dos módulos.
- **engine/protohax** — submódulo Git no commit `afef78c3883ee53fad180ceb6acfae02cf1a302a`; build Gradle separado com licença upstream.
- **server-bridge** — contrato HTTP separado; não concede permissões administrativas.

## Funcionalidade integrada

- Auto Clicker e Auto Jump com coordenadas/intervalos.
- Categorias compartilhadas entre painel e sobreposição.
- Módulos com `engineClass` ligados ao `ModuleManager` ProtoHax quando uma sessão de relay é criada.
- Tela Relay com destino, porta UDP local, estado e iniciar/parar.
- Perfis JSON locais versionados.

## Limitações

- **Autenticação:** o listener cria identidade offline, não faz Xbox Live/XSTS/Minecraft. Servidores autenticados podem recusar a sessão. O login Microsoft da aba Conta consulta o perfil Graph e não fornece a cadeia Xbox/Minecraft.
- **Compatibilidade:** o ProtoHax pinado tem codecs antigos. Build bem-sucedido não prova conexão com a versão atual do Bedrock nem funcionamento em aparelho.
- **Renderização:** ESP e informação visual de entidades não estão integrados ao renderizador do Minecraft.
- **Valores internos:** os módulos usam valores padrão do ProtoHax; editor genérico de `Value<T>` ainda não foi ligado à UI.
- **Ciclo de vida:** upstream não expõe fecho explícito de todos os executores da sessão. Testes de start/stop/reconexão são necessários antes de uso prolongado.

## Licença

ProtoHax é GPL-3.0-or-later, mantido como submódulo Git, com licença em `LICENSE-GPL-3.0.txt`. Antes de redistribuir o APK combinado, revise as obrigações GPL e preserve fontes/avisos de todas as partes integradas.

## Testes restantes

1. Compilar com a engine incluída.
2. Testar relay em rede local e conferir a versão de protocolo.
3. Implementar o fluxo Microsoft → Xbox Live → XSTS → Minecraft sem expor tokens.
4. Corrigir gestão de executores/reconexões e validar Bedrock atual.
5. Ligar valores internos da engine à UI; manter ESP como não implementado.
