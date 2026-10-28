// Build settings shared by Rollup, build.mjs, and scripts/deploy.mjs.
export default {
    entry: "src/index.ts",
    outFile: "dist/XZeroy.js",
    sourcemap: "inline",
    minify: false,
    banner: {
        namespace: "https://github.com/mevisnascimento495-cmd/Script-Bleach",
    },
};
