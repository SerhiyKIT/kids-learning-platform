import { apiFetchBlob } from "./api";

/**
 * Downloads a JSON export through apiFetchBlob (same session cookie and error handling as every
 * API call) and saves it via a temporary blob: link — the data never touches browser storage.
 */
export async function downloadExport(path: string, filename: string): Promise<void> {
  const blob = await apiFetchBlob(path);
  const url = URL.createObjectURL(blob);
  try {
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
  } finally {
    // Give the browser a moment to start the download before the URL goes away.
    setTimeout(() => URL.revokeObjectURL(url), 10_000);
  }
}
