import React, { useRef, useState } from "react";
import {
  Alert,
  Platform,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";
import { CameraView, useCameraPermissions } from "expo-camera";
import * as Speech from "expo-speech";
import Voice from "@react-native-voice/voice";
import { useRobotState } from "./src/useRobotState";
import RobotView from "./src/RobotView";
import { setBaseUrl, getBaseUrl } from "./src/api";

type Tab = "navigate" | "record";

export default function App() {
  const [tab, setTab] = useState<Tab>("navigate");
  const [permission, requestPermission] = useCameraPermissions();
  const cameraRef = useRef<CameraView>(null);
  const { state, setListening, addRoomImage, navigate, submitRoomRecording, reset } =
    useRobotState();

  // ── Voice ────────────────────────────────────────────────────────────────────

  async function startVoiceCommand() {
    try {
      setListening();
      Voice.onSpeechResults = async (e) => {
        const command = e.value?.[0];
        if (!command) {
          return;
        }
        Voice.destroy();
        const frame = await captureFrame();
        if (frame) navigate(command, frame);
      };
      Voice.onSpeechError = (e) => {
        Voice.destroy();
        Alert.alert("Voice error", e.error?.message ?? "Unknown error");
      };
      await Voice.start("en-US");
    } catch (e: any) {
      Alert.alert("Voice error", e.message);
    }
  }

  // ── Camera ───────────────────────────────────────────────────────────────────

  async function captureFrame(): Promise<string | null> {
    if (!cameraRef.current) return null;
    const photo = await cameraRef.current.takePictureAsync({ base64: true, quality: 0.8 });
    return photo?.base64 ?? null;
  }

  async function captureRoomPhoto() {
    const base64 = await captureFrame();
    if (base64) addRoomImage(base64);
  }

  // ── TTS ──────────────────────────────────────────────────────────────────────

  React.useEffect(() => {
    if (state.kind === "navigationResult") {
      Speech.speak(state.speechText, { language: "en-US", rate: 0.95 });
    } else if (state.kind === "roomRecorded") {
      Speech.speak(`Room recorded. ${state.count} scenes stored in memory.`);
    }
  }, [state]);

  // ── Settings dialog ───────────────────────────────────────────────────────────

  function showSettings() {
    Alert.prompt(
      "Backend Server URL",
      "Enter the IP and port of your Python server:",
      (url) => { if (url) setBaseUrl(url); },
      "plain-text",
      getBaseUrl()
    );
  }

  // ── Permissions ───────────────────────────────────────────────────────────────

  if (!permission) return <View />;
  if (!permission.granted) {
    return (
      <SafeAreaView style={styles.center}>
        <Text style={styles.permText}>Camera permission is required.</Text>
        <TouchableOpacity style={styles.btn} onPress={requestPermission}>
          <Text style={styles.btnText}>Grant Permission</Text>
        </TouchableOpacity>
      </SafeAreaView>
    );
  }

  // ── Status text ───────────────────────────────────────────────────────────────

  function statusText(): string {
    switch (state.kind) {
      case "idle":             return "Ready";
      case "listening":        return "Listening... speak your command";
      case "processing":       return "Processing with AI...";
      case "capturing":        return `${state.count} photo(s) captured. Capture more or Save Room.`;
      case "navigationResult": return `${arrowFor(state.direction)} ${state.direction.toUpperCase()}\n"${state.speechText}"`;
      case "roomRecorded":     return `Room saved! ${state.count} scene(s) stored in AI memory.`;
      case "error":            return `Error: ${state.message}`;
    }
  }

  function arrowFor(d: string) {
    return ({ forward: "▲", back: "▼", left: "◀", right: "▶" } as any)[d] ?? "■";
  }

  const busy = state.kind === "listening" || state.kind === "processing";

  // ── Render ────────────────────────────────────────────────────────────────────

  return (
    <SafeAreaView style={styles.root}>
      {/* Tabs */}
      <View style={styles.tabs}>
        {(["navigate", "record"] as Tab[]).map((t) => (
          <TouchableOpacity
            key={t}
            style={[styles.tab, tab === t && styles.tabActive]}
            onPress={() => { setTab(t); reset(); }}
          >
            <Text style={[styles.tabText, tab === t && styles.tabTextActive]}>
              {t === "navigate" ? "Navigate" : "Record Room"}
            </Text>
          </TouchableOpacity>
        ))}
        <TouchableOpacity style={styles.settingsBtn} onPress={showSettings}>
          <Text style={styles.settingsIcon}>⚙️</Text>
        </TouchableOpacity>
      </View>

      {/* Camera preview */}
      <CameraView ref={cameraRef} style={styles.camera} facing="back" />

      {/* Status */}
      <View style={styles.statusBox}>
        <Text style={styles.statusText}>{statusText()}</Text>
      </View>

      {/* Navigate panel */}
      {tab === "navigate" && (
        <View style={styles.panel}>
          <RobotView
            direction={
              state.kind === "navigationResult" ? state.direction : "stop"
            }
          />
          <TouchableOpacity
            style={[styles.btn, busy && styles.btnDisabled]}
            disabled={busy}
            onPress={startVoiceCommand}
          >
            <Text style={styles.btnText}>
              {state.kind === "listening" ? "Listening..." : "Hold to Speak"}
            </Text>
          </TouchableOpacity>
        </View>
      )}

      {/* Record Room panel */}
      {tab === "record" && (
        <View style={styles.panel}>
          <Text style={styles.countText}>
            {state.kind === "capturing" ? `${state.count} photos` : "0 photos"}
          </Text>
          <View style={styles.row}>
            <TouchableOpacity
              style={[styles.btn, styles.btnHalf, busy && styles.btnDisabled]}
              disabled={busy}
              onPress={captureRoomPhoto}
            >
              <Text style={styles.btnText}>📷 Capture</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.btn, styles.btnHalf, styles.btnGreen, busy && styles.btnDisabled]}
              disabled={busy}
              onPress={submitRoomRecording}
            >
              <Text style={styles.btnText}>💾 Save Room</Text>
            </TouchableOpacity>
          </View>
        </View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root:         { flex: 1, backgroundColor: "#F5F5F5" },
  center:       { flex: 1, alignItems: "center", justifyContent: "center", padding: 24 },
  permText:     { fontSize: 16, marginBottom: 16, textAlign: "center" },
  tabs:         { flexDirection: "row", backgroundColor: "#1565C0" },
  tab:          { flex: 1, paddingVertical: 14, alignItems: "center" },
  tabActive:    { borderBottomWidth: 3, borderBottomColor: "#FFF" },
  tabText:      { color: "#90CAF9", fontWeight: "600", fontSize: 15 },
  tabTextActive:{ color: "#FFF" },
  settingsBtn:  { paddingHorizontal: 16, justifyContent: "center" },
  settingsIcon: { fontSize: 20 },
  camera:       { height: 220 },
  statusBox:    { backgroundColor: "#E3F2FD", padding: 12, minHeight: 60 },
  statusText:   { fontSize: 14, color: "#1565C0", textAlign: "center", fontWeight: "500" },
  panel:        { flex: 1, alignItems: "center", justifyContent: "center", padding: 20, gap: 16 },
  countText:    { fontSize: 18, fontWeight: "bold", color: "#37474F" },
  row:          { flexDirection: "row", gap: 12 },
  btn: {
    backgroundColor: "#1565C0",
    paddingVertical: 14,
    paddingHorizontal: 32,
    borderRadius: 10,
    elevation: 3,
  },
  btnHalf:      { flex: 1, paddingHorizontal: 0, alignItems: "center" },
  btnGreen:     { backgroundColor: "#2E7D32" },
  btnDisabled:  { backgroundColor: "#90A4AE" },
  btnText:      { color: "#FFF", fontWeight: "700", fontSize: 15, textAlign: "center" },
});
