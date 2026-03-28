"""RoboAGV Navigation Backend — FastAPI + Gemini VLM + Agents."""
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List
import uvicorn

from agents import vlm_agent, memory_agent, navigation_agent

app = FastAPI(title="RoboAGV Navigation API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ── Request / Response models ────────────────────────────────────────────────

class RecordRoomRequest(BaseModel):
    images: List[str]  # Base64-encoded JPEG images


class RecordRoomResponse(BaseModel):
    status: str
    descriptions: List[str]
    memory_count: int


class NavigateRequest(BaseModel):
    voice_command: str
    current_frame: str  # Base64-encoded JPEG


class NavigateResponse(BaseModel):
    direction: str       # forward | back | left | right | stop
    speech_text: str
    reasoning: str


# ── Routes ───────────────────────────────────────────────────────────────────

@app.post("/record-room", response_model=RecordRoomResponse)
async def record_room(request: RecordRoomRequest):
    """
    Phase 0: Scan room images with Gemini VLM and store descriptions in AI memory.
    Clears previous memory before storing new room data.
    """
    if not request.images:
        raise HTTPException(status_code=400, detail="No images provided")

    memory_agent.clear_memory()
    descriptions = []

    for i, image_b64 in enumerate(request.images):
        description = await vlm_agent.describe_room_image(image_b64, i)
        memory_agent.add_room_description(description, i)
        descriptions.append(description)

    return RecordRoomResponse(
        status="ok",
        descriptions=descriptions,
        memory_count=len(descriptions)
    )


@app.post("/navigate", response_model=NavigateResponse)
async def navigate(request: NavigateRequest):
    """
    Phase 1: Voice command + current frame → VLM scene description
             → Navigation agent (queries memory, decides direction).
    """
    # Step 1: VLM describes current camera view
    scene_description = await vlm_agent.describe_current_scene(
        request.current_frame,
        request.voice_command
    )

    # Step 2: Navigation agent queries memory and decides movement
    result = await navigation_agent.decide_navigation(
        request.voice_command,
        scene_description
    )

    return NavigateResponse(
        direction=result.get("direction", "stop"),
        speech_text=result.get("speech_text", "Navigation complete"),
        reasoning=result.get("reasoning", "")
    )


@app.get("/memory")
async def get_memory():
    """Return all stored room descriptions."""
    return {"descriptions": memory_agent.get_all_descriptions()}


@app.delete("/memory")
async def clear_memory():
    """Clear all stored room memory."""
    memory_agent.clear_memory()
    return {"status": "cleared"}


@app.get("/health")
async def health():
    return {"status": "ok", "version": "1.0.0"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000, reload=True)
