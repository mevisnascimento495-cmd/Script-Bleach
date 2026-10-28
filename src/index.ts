import { defineModule, SprintIntent } from "@protohax/userscript";

/**
 * XZeroy is a collection of ProtoHax UserScript modules. It intentionally uses
 * the host's public API and leaves server-side permissions/validation intact.
 */

// Automatically request a sprint while the player is moving.
defineModule(
    { name: "XZeroy | AutoSprint" },
    {
        direction: {
            type: "enum",
            name: "Direction",
            values: ["Forward only", "Any direction"],
            def: "Forward only",
        },
        respectSneak: {
            type: "boolean",
            name: "Respect sneak input",
            def: true,
        },
    },
    (ctx) => {
        ctx.on("input_tick", (input) => {
            if (ctx.options.respectSneak.value && input.userSneak) return;

            const move = input.userMove;
            const isMoving = move.x !== 0 || move.y !== 0;
            if (!isMoving) return;

            if (ctx.options.direction.value === "Forward only" && move.y <= 0) return;

            input.requestSprint(SprintIntent.Sprint);
        });
    },
);

// Repeatedly jump on landing, using the movement API's server-delay checks.
defineModule(
    { name: "XZeroy | AutoJump" },
    {
        whileMoving: {
            type: "boolean",
            name: "Only jump while moving",
            def: true,
        },
    },
    (ctx) => {
        ctx.on("movement_tick", (state) => {
            if (ctx.options.whileMoving.value && !state.isMoving) return;
            if (state.onGround) state.jump();
        });
    },
);

// Uses ProtoHax's click scheduler. It clicks at the player's current server-side
// view rotation: no automatic target selection or reach extension is applied.
defineModule(
    { name: "XZeroy | AutoClicker" },
    {
        click: { type: "click-scheduler" },
    },
    (ctx) => {
        let disposeClicker: (() => void) | undefined;

        ctx.onEnable(() => {
            const scheduler = ctx.options.click.getClickScheduler(() => {
                const player = ctx.session.entityState.localPlayer;
                player.click(player.serverSideRotation);
            });
            disposeClicker = () => scheduler.dispose();
        });

        ctx.onDisable(() => {
            disposeClicker?.();
            disposeClicker = undefined;
        });
    },
);
