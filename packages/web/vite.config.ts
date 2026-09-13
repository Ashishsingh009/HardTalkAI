import { copyFileSync, existsSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const API_TARGET = process.env.VITE_API_TARGET ?? "http://localhost:3001";
const here = dirname(fileURLToPath(import.meta.url));
const privacySrc = resolve(here, "../../docs/privacy-policy.html");
const publicDir = resolve(here, "public");

mkdirSync(publicDir, { recursive: true });
if (existsSync(privacySrc)) {
  copyFileSync(privacySrc, resolve(publicDir, "privacy.html"));
}

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
    proxy: {
      "/api": {
        target: API_TARGET,
        changeOrigin: true,
      },
    },
  },
});
