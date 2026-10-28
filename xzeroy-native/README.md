# XZeroy Native — XZeroy cheat client para Android (em desenvolvimento)

Este diretório inicia uma versão Android própria do XZeroy cheat client, separada do UserScript ProtoHax que existe na raiz do repositório. A proposta é manter módulos de cheat/client-side independentes, com interface própria; os recursos que dependem do estado interno do jogo ainda precisam de uma integração técnica específica.

## Estado atual

O app inclui uma interface Android e um serviço de Acessibilidade opcional para toques configuráveis. O serviço só tenta executar Auto Clicker/Auto Jump se o app em primeiro plano for Minecraft Bedrock para Android (com.mojang.minecraftpe). O usuário precisa ativar o serviço manualmente nas configurações do Android e definir coordenadas que correspondam ao layout de controles do jogo.

Isto não é um client de injeção de memória. ESP, leitura real de coordenadas, extensão de alcance, alteração de atributos e privilégios administrativos ainda não estão implementados. O projeto continua sendo tratado como um cheat client; isso não significa que o APK, sozinho, consiga impor permissões ao servidor. As rotas administrativas do bridge continuam retornando HTTP 501 até haver um adaptador de servidor compatível e testado.

## Estrutura
- android/: app Android nativo em Kotlin.
- server-bridge/: API local autenticada, com operações administrativas bloqueadas por padrão.
- docs/ARCHITECTURE.md: arquitetura e limites de segurança.

## Compilar e obter APK

O workflow XZeroy Native Android Build compila app-debug.apk e publica como artefato do GitHub Actions sempre que a branch xzeroy-native-app-bridge recebe alterações. Abra a aba Actions, escolha uma execução concluída e baixe o artefato XZeroy-debug-apk.

O APK de debug não é assinado para publicação. Instale apenas no seu aparelho e desligue a Acessibilidade quando não estiver usando o XZeroy.

## Uso do Auto Clicker/Auto Jump
1. Instale e abra o app.
2. Configure X/Y em percentagem da tela (0–100) e os intervalos.
3. Habilite o serviço XZeroy em Configurações do Android → Acessibilidade.
4. Volte ao Minecraft Bedrock. Use o controle flutuante: toque curto alterna Auto Clicker; toque longo alterna Auto Jump.
5. Desative ambos ao terminar ou desligue o serviço nas configurações de Acessibilidade.

O app envia toques em posições fixas; não detecta alvos nem lê estado interno do jogo. Se o layout mudar, ajuste as percentagens.

## Bridge
Requer Node.js 20+. Configure XZEROY_ADMIN_TOKEN com um segredo aleatório de pelo menos 32 caracteres e execute npm start dentro de server-bridge. O serviço escuta somente em 127.0.0.1 por padrão. As rotas administrativas retornam HTTP 501 até um adaptador ser implementado. Não embuta segredos no APK nem exponha a API publicamente sem autenticação forte e TLS.
