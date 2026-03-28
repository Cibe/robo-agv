const DEFAULT_BASE_URL = "http://192.168.1.100:8000";

let baseUrl = DEFAULT_BASE_URL;

export function setBaseUrl(url: string) {
  baseUrl = url.replace(/\/$/, "");
}

export function getBaseUrl() {
  return baseUrl;
}

export interface NavigateResponse {
  direction: "forward" | "back" | "left" | "right" | "stop";
  speech_text: string;
  reasoning: string;
}

export interface RecordRoomResponse {
  status: string;
  descriptions: string[];
  memory_count: number;
}

async function post<T>(path: string, body: object): Promise<T> {
  const res = await fetch(`${baseUrl}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

export async function navigate(
  voiceCommand: string,
  currentFrameBase64: string
): Promise<NavigateResponse> {
  return post("/navigate", {
    voice_command: voiceCommand,
    current_frame: currentFrameBase64,
  });
}

export async function recordRoom(
  imagesBase64: string[]
): Promise<RecordRoomResponse> {
  return post("/record-room", { images: imagesBase64 });
}

export async function clearMemory(): Promise<void> {
  const res = await fetch(`${baseUrl}/memory`, { method: "DELETE" });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
}
