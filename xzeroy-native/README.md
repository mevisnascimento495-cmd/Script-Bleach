# XZeroy Native — protótipo inicial

Este diretório inicia uma versão Android própria do XZeroy, separada do UserScript ProtoHax na raiz do repositório.

**Estado:** código inicial, não um APK pronto. Os botões do app são demonstrativos e ainda não controlam Minecraft. As rotas administrativas ficam bloqueadas até existir um adaptador compatível e testado para o software e a versão do servidor.

## Estrutura
- `android/`: app Android nativo em Kotlin.
- `server-bridge/`: API local autenticada, com operações administrativas fail-closed.
- `docs/ARCHITECTURE.md`: arquitetura e limites de segurança.

## App
Abra a pasta `android` no Android Studio com JDK 17, sincronize o Gradle e execute a configuração `app`.

## Bridge
Requer Node.js 20+. Configure `XZEROY_ADMIN_TOKEN` com um segredo aleatório de pelo menos 32 caracteres e execute:
```sh
cd server-bridge
XZEROY_ADMIN_TOKEN='coloque-um-segredo-longo-aqui' npm start
```
O serviço escuta somente em `127.0.0.1` por padrão. As rotas administrativas retornam HTTP 501 até um adaptador ser implementado. Não embuta segredos no APK nem exponha a API publicamente sem autenticação forte e TLS.
