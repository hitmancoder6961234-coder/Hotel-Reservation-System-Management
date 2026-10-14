import path from "node:path";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

// The original Next.js app used the "@/" alias for ./src - kept so the
// component source stays byte-for-byte comparable with the original.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: { alias: { "@": path.resolve(import.meta.dirname, "src") } },
  server: {
    port: 3000,
    // During local development the Java backend (Tomcat) listens on :8080.
    proxy: { "/api": { target: process.env.VITE_API_PROXY ?? "http://localhost:8080", changeOrigin: true } },
  },
  build: { outDir: "dist", sourcemap: false },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.js"],
    css: false,
  },
});
