# RoboAGV — AI-Powered Robot Navigation

Voice-controlled robot navigation using **Gemini VLM + AI Memory + Agents** on mobile (React Native).

## How It Works

```
Phase 0 — Record Room
  App captures room images (multiple angles)
  → Gemini VLM analyzes each image → spatial descriptions stored in AI memory

Phase 1 — Navigate
  User speaks "go to microwave"
  → Current camera frame captured
  → Gemini VLM describes current scene
  → Navigation Agent (Gemini function calling):
       queries memory agent → gets room layout context
       decides direction (forward / back / left / right / stop)
  → App speaks the result (TTS) + animates robot on screen
```

## System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  React Native App (Expo)                 │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐ │
│  │ expo-camera  │  │  Voice STT   │  │  expo-speech  │ │
│  │ (live preview│  │ (voice input)│  │  (speaks cmd) │ │
│  │  + capture)  │  └──────┬───────┘ └───────────────┘ │
│  └──────┬───────┘         │                            │
│         │         ┌───────▼────────┐                   │
│         └─────────►  useRobotState │                   │
│                   │  (React hook)  │                   │
│                   └───────┬────────┘                   │
│                           │ fetch HTTP                  │
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
└── mobile/                         # React Native app (Expo)
    ├── App.tsx                     # Main UI, tabs, camera, voice, TTS
    ├── app.json                    # Expo config (permissions, bundle ID)
    ├── package.json
    └── src/
        ├── api.ts                  # Backend API client
        ├── useRobotState.ts        # State management hook
        └── RobotView.tsx           # Animated robot direction view
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

### Mobile App

**Requirements:** Node.js 18+, Android Studio (for Android), Xcode (for iOS)

#### Option A — Expo Go (fastest, no build needed)

1. Install **Expo Go** on your phone:
   - Android: [Play Store](https://play.google.com/store/apps/details?id=host.exp.exponent)
   - iPhone: [App Store](https://apps.apple.com/app/expo-go/id982107779)

2. Start the dev server:
   ```bash
   cd mobile
   npm install
   npx expo start
   ```

3. Scan the QR code with Expo Go (Android) or your Camera app (iPhone).

> Your phone and computer must be on the **same Wi-Fi network**.

#### Option B — Run on Android via Android Studio (USB)

1. **Enable Developer Mode** on your phone:
   - Settings → About Phone → tap **Build Number** 7 times
   - Settings → Developer Options → enable **USB Debugging**

2. **Connect your phone** via USB cable and allow the connection prompt.

3. **Set up Android SDK** environment variables (add to `~/.zshrc`):
   ```bash
   export ANDROID_HOME=$HOME/Library/Android/sdk
   export PATH=$PATH:$ANDROID_HOME/emulator
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   ```
   Then run: `source ~/.zshrc`

4. **Verify your device is detected:**
   ```bash
   adb devices
   ```

5. **Build and install the app:**
   ```bash
   cd mobile
   npm install
   npx expo run:android
   ```
   The app will be built, installed, and launched on your phone automatically.

6. **Set the backend URL** — tap **⚙️** (top right) and enter your computer's local IP:
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
| VLM (vision) | Google Gemini 3.0 Flash |
| LLM + Agents | Google Gemini 3.0 Flash (function calling) |
| AI Memory | Gemini semantic search over JSON store |
| Backend | Python FastAPI + Uvicorn |
| Mobile | React Native (Expo) |
| Voice Input | @react-native-voice/voice |
| TTS | expo-speech |
| Camera | expo-camera |
