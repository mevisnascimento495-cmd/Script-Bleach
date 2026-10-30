# XZeroy Native engine notes

The ProtoHax engine is included as a Git submodule at `protohax/`, pinned to
`afef78c3883ee53fad180ceb6acfae02cf1a302a` (the upstream `stable` commit used for this integration).

Upstream project: https://github.com/hax0r31337/ProtoHax

The engine is licensed under GNU GPL version 3 or later. Its source and license
are kept in the submodule. The XZeroy Android app uses the published shaded
`dev.sora.relay:ProtoHax:1.4.0` artifact from the package repository. The source
submodule is retained for audit and license compliance, not built as a composite
project. Review `../LICENSE-GPL-3.0.txt` and the upstream `LICENSE` before
redistributing a combined APK.

Important: the upstream relay's offline login listener does not provide Xbox
Live/XSTS/Minecraft authentication for authenticated public servers. Protocol
compatibility must be tested against the actual Bedrock version before any
feature is claimed to work.
