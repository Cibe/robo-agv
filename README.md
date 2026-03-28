# RoboAGV — AI-Powered Robot Navigation

Voice-controlled robot navigation using **Gemini VLM + AI Memory + Agents** on Android.

## How It Works

```
Phase 0 — Record Room
  Android captures room images (multiple angles)
  → Gemini VLM analyzes each image → spatial descriptions stored in AI memory

Phase 1 — Navigate
  User speaks "go to microwave" on Android
  → Current camera frame captured
  → Gemini VLM describes current scene
  → Navigation Agent (Gemini function calling):
       queries memory agent → gets room layout context
       decides direction (forward / back / left / right / stop)
  → Android speaks the result (TTS) + animates robot on screen
```

## System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Android App                         │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐ │
│  │ CameraX      │  │SpeechRecognizer│ │  TextToSpeech │ │
│  │ (live preview│  │ (voice input) │ │  (speaks cmd) │ │
│  │  + capture)  │  └──────┬───────┘ └───────────────┘ │
│  └──────┬───────┘         │                            │
│         │         ┌───────▼────────┐                   │
│         └─────────►  RobotViewModel│                   │
│                   │  (StateFlow)   │                   │
│                   └───────┬────────┘                   │
│                           │ Retrofit HTTP               │
└───────────────────────────┼────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                  FastAPI Backend                        │
│                                                         │
│  POST /record-room         POST /navigate               │
│       │                         │                       │
│  Gemini VLM              Gemini VLM                     │
│  (describe room)         (describe scene)               │
│       │                         │                       │
│  Memory Store            Navigation Agent               │
│  (JSON)                  (Gemini function calling)      │
│                           ├── query_room_memory tool    │
│                           │    └── Gemini semantic search│
│                           └── navigate tool             │
│                                └── direction + speech   │
└────────────────────────────────────────────────────────┘
```

## Project Structure

```
robo-agv/
├── backend/                        # Python FastAPI server
│   ├── main.py                     # API routes
│   ├── requirements.txt
│   ├── .env.example
│   ├── agents/
│   │   ├── vlm_agent.py            # Gemini vision (room scan + scene description)
│   │   ├── memory_agent.py         # AI memory store + semantic query
│   │   └── navigation_agent.py     # Gemini agentic loop (function calling)
│   └── memory/
│       └── store.py                # JSON-based room memory store
└── android/                        # Android app (Kotlin)
    └── app/src/main/java/com/roboagv/
        ├── MainActivity.kt         # Main UI, camera, tab modes
        ├── RobotViewModel.kt       # Business logic, API calls
        ├── RobotView.kt            # Custom animated robot view
        ├── VoiceController.kt      # SpeechRecognizer + TTS
        └── ApiService.kt           # Retrofit API client
```

## Setup

### Backend

**Requirements:** Python 3.10+, a [Gemini API key](https://aistudio.google.com/)

```bash
cd backend
cp .env.example .env
# Edit .env and add your GEMINI_API_KEY
pip install -r requirements.txt
python main.py
# Server starts at http://0.0.0.0:8000
```

### Android App

**Requirements:** Android Studio, Android device (API 26+)

1. Open the `android/` folder in Android Studio
2. Build and run on your device
3. Tap the **settings icon** (top right) and enter your computer's local IP:
   ```
   http://192.168.x.x:8000
   ```
   *(Find your IP with `ifconfig` on Mac/Linux or `ipconfig` on Windows)*

## Usage

### Step 1 — Record the Room
1. Switch to the **Record Room** tab
2. Walk around pointing the camera at different areas
3. Tap **Capture** from multiple angles (kitchen, living room, desk, etc.)
4. Tap **Save Room** — Gemini analyzes each photo and stores spatial descriptions in AI memory

### Step 2 — Navigate
1. Switch to the **Navigate** tab
2. Tap **Hold to Speak** and say a command:
   - *"go to the microwave"*
   - *"move toward the sofa"*
   - *"find the door"*
3. The app:
   - Captures the current camera frame
   - Sends voice command + frame to Gemini VLM
   - Navigation agent queries memory and decides direction
   - Speaks the result aloud
   - Animates the robot on screen

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/record-room` | Send base64 room images → stored in AI memory |
| `POST` | `/navigate` | Send voice command + frame → get direction |
| `GET`  | `/memory` | View all stored room descriptions |
| `DELETE` | `/memory` | Clear room memory |
| `GET`  | `/health` | Health check |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| VLM (vision) | Google Gemini 2.0 Flash |
| LLM + Agents | Google Gemini 2.0 Flash (function calling) |
| AI Memory | Gemini semantic search over JSON store |
| Backend | Python FastAPI + Uvicorn |
| Android | Kotlin + CameraX + SpeechRecognizer + TTS |
| Networking | Retrofit2 + OkHttp |
