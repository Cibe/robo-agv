import { useState, useRef } from "react";
import * as api from "./api";

export type Direction = "forward" | "back" | "left" | "right" | "stop";

export type AppState =
  | { kind: "idle" }
  | { kind: "listening" }
  | { kind: "processing" }
  | { kind: "capturing"; count: number }
  | { kind: "navigationResult"; direction: Direction; speechText: string }
  | { kind: "roomRecorded"; count: number }
  | { kind: "error"; message: string };

export function useRobotState() {
  const [state, setState] = useState<AppState>({ kind: "idle" });
  const capturedImages = useRef<string[]>([]);

  function setListening() {
    setState({ kind: "listening" });
  }

  function addRoomImage(base64: string) {
    capturedImages.current.push(base64);
    setState({ kind: "capturing", count: capturedImages.current.length });
  }

  async function navigate(voiceCommand: string, frameBase64: string) {
    setState({ kind: "processing" });
    try {
      const res = await api.navigate(voiceCommand, frameBase64);
      setState({
        kind: "navigationResult",
        direction: res.direction,
        speechText: res.speech_text,
      });
    } catch (e: any) {
      setState({ kind: "error", message: e.message ?? "Navigation failed" });
    }
  }

  async function submitRoomRecording() {
    if (capturedImages.current.length === 0) {
      setState({ kind: "error", message: "No images captured. Tap Capture first." });
      return;
    }
    setState({ kind: "processing" });
    try {
      const res = await api.recordRoom(capturedImages.current);
      capturedImages.current = [];
      setState({ kind: "roomRecorded", count: res.memory_count });
    } catch (e: any) {
      setState({ kind: "error", message: e.message ?? "Failed to save room" });
    }
  }

  function reset() {
    capturedImages.current = [];
    setState({ kind: "idle" });
  }

  return { state, setListening, addRoomImage, navigate, submitRoomRecording, reset };
}
