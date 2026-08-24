import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const NO_STORAGE_MESSAGE =
  "No browser storage for auth (docs/CONVENTIONS.md) — auth lives in the httpOnly session cookie; nothing client-readable should hold it.";
const NO_RAW_FETCH_MESSAGE =
  "All network calls go through apiFetch (src/lib/api.ts) — it handles credentials, the CSRF header, and typed ApiError. Don't call fetch() directly.";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
  {
    rules: {
      "no-restricted-globals": [
        "error",
        { name: "localStorage", message: NO_STORAGE_MESSAGE },
        { name: "sessionStorage", message: NO_STORAGE_MESSAGE },
      ],
      "no-restricted-properties": [
        "error",
        { object: "window", property: "localStorage", message: NO_STORAGE_MESSAGE },
        { object: "window", property: "sessionStorage", message: NO_STORAGE_MESSAGE },
      ],
      "no-restricted-syntax": [
        "error",
        {
          selector: "CallExpression[callee.name='fetch']",
          message: NO_RAW_FETCH_MESSAGE,
        },
        {
          selector: "CallExpression[callee.object.name='window'][callee.property.name='fetch']",
          message: NO_RAW_FETCH_MESSAGE,
        },
      ],
    },
  },
  {
    // The one file allowed to touch fetch() directly — everything else routes through it.
    files: ["src/lib/api.ts"],
    rules: {
      "no-restricted-syntax": "off",
    },
  },
]);

export default eslintConfig;
