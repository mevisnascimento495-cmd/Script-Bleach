# XZeroy

Módulos em TypeScript para o cliente **ProtoHax** do Minecraft Bedrock, mantidos neste repositório `Script-Bleach`.

## O que já está implementado

- **AutoSprint:** solicita corrida enquanto há movimento, com opção para correr só para frente e para respeitar o botão de agachar.
- **AutoJump:** tenta saltar automaticamente ao tocar o chão, com opção para funcionar apenas enquanto o jogador se move. Usa o controle de salto do ProtoHax.
- **AutoClicker:** usa o agendador de cliques do ProtoHax, com controles de CPS/técnica fornecidos pelo cliente. Clica na rotação que o jogador está enviando; não faz seleção automática de alvos nem aumenta o alcance.

Os módulos aparecem na categoria **Script** do menu ProtoHax e podem ser ligados/desligados individualmente.

## Limites importantes

O cliente não pode conceder permissões administrativas por conta própria. Alterar o modo de jogo no servidor, adicionar XP ou aplicar efeitos que mudem o estado do servidor depende dos comandos e permissões que o servidor aceita. O servidor também pode validar, corrigir ou bloquear movimentos e cliques. O XZeroy não tenta contornar autenticação, permissões ou validação do servidor.

## Requisitos

- Uma versão do ProtoHax que suporte ProtoHax UserScript.
- Node.js compatível com as dependências do projeto (recomenda-se Node 22).

## Compilar

```bash
npm ci
npm run check
npm run build
```

O artefato fica em `dist/XZeroy.js`. O arquivo é ignorado pelo Git para que builds locais não substituam o código-fonte.

## Instalação

1. Compile com `npm run build`.
2. Copie `dist/XZeroy.js` para a pasta `scripts` que sua instalação do ProtoHax utiliza.
3. Reinicie o ProtoHax e procure os módulos com prefixo `XZeroy |` na categoria **Script**.

Para implantação automática, defina `PROTOHAX_SCRIPTS_DIR` com a pasta de scripts no computador e execute `npm run deploy`. Para copiar via ADB a um dispositivo Android conectado, configure o ADB no computador e execute `node build.mjs --deploy --adb`. Em alguns Androids, as restrições de acesso a `Android/data` podem exigir instalação manual.

## Desenvolvimento

```bash
npm run check     # verifica tipos TypeScript
npm run build     # gera um bundle único
npm run watch     # recompila quando arquivos mudam
```

A API do ProtoHax UserScript ainda está na versão 0.x e pode mudar. Consulte a documentação oficial antes de atualizar o código: <https://userscript.protohax.net/>.

## Uso responsável

Use os módulos de acordo com as regras do servidor. Automação de cliques pode ser proibida em alguns servidores. O XZeroy não inclui auto-aim, ataque automático a outros jogadores, extensão de alcance ou bypass de permissões.

## Licença

MIT. Consulte [`LICENSE`](LICENSE).
