// Copy the built bundle into the ProtoHax scripts folder.
// Set PROTOHAX_SCRIPTS_DIR for desktop/manual paths, or pass --adb for Android.

import { copyFileSync, mkdirSync } from "node:fs";
import { spawnSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import config from "../script.config.mjs";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const ANDROID_SCRIPTS_DIR =
    process.env.PROTOHAX_ANDROID_DIR ??
    "/storage/emulated/0/Android/data/net.protohax.prod/files/protohax/scripts";

export function scriptsDir() {
    if (process.env.PROTOHAX_SCRIPTS_DIR) return process.env.PROTOHAX_SCRIPTS_DIR;
    if (process.platform === "win32" && process.env.APPDATA) {
        return path.join(process.env.APPDATA, "ProtoHax", "scripts");
    }
    return null;
}

function pushWithAdb(file) {
    const remote = `${ANDROID_SCRIPTS_DIR}/${path.basename(file)}`;
    const mkdir = spawnSync("adb", ["shell", "mkdir", "-p", ANDROID_SCRIPTS_DIR], { stdio: "inherit" });
    if (mkdir.error) throw new Error(`ADB não encontrado no PATH: ${mkdir.error.message}`);
    if (mkdir.status !== 0) throw new Error(`Falha ao criar a pasta no Android (código ${mkdir.status}).`);
    const push = spawnSync("adb", ["push", file, remote], { stdio: "inherit" });
    if (push.error) throw new Error(`Falha ao executar adb push: ${push.error.message}`);
    if (push.status !== 0) throw new Error(`adb push falhou (código ${push.status}).`);
    return remote;
}

export function deploy(file = path.join(ROOT, config.outFile), { adb = false } = {}) {
    if (adb || process.env.PROTOHAX_ADB === "1") return pushWithAdb(file);
    const dir = scriptsDir();
    if (!dir) {
        throw new Error(
            "A pasta de scripts do ProtoHax não foi detectada neste sistema. " +
            "Defina PROTOHAX_SCRIPTS_DIR ou use --adb com um dispositivo Android conectado.",
        );
    }
    const dest = path.join(dir, path.basename(file));
    mkdirSync(dir, { recursive: true });
    copyFileSync(file, dest);
    return dest;
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
    const dest = deploy(undefined, { adb: process.argv.includes("--adb") });
    console.log(`[deploy] ${path.basename(config.outFile)} -> ${dest}`);
}
