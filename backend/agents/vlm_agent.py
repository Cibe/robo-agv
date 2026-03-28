"""VLM agent using Gemini for image understanding."""
import base64
import os
import google.generativeai as genai
from dotenv import load_dotenv

load_dotenv()
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))

_model = genai.GenerativeModel("gemini-3.0-flash")


async def describe_room_image(base64_image: str, image_index: int = 0) -> str:
    """Analyze a room image and return a spatial description for navigation memory."""
    image_bytes = base64.b64decode(base64_image)
    response = await _model.generate_content_async([
        {"mime_type": "image/jpeg", "data": image_bytes},
        f"""Analyze this room image (scan {image_index + 1}) for robot navigation memory.

Describe in detail:
1. All visible objects and their exact positions (left/center/right, near/far, high/low)
2. Furniture layout and spatial relationships (e.g., "microwave is on the counter to the right of the refrigerator")
3. Key navigation landmarks (doors, windows, appliances, distinct furniture)
4. Clear paths and any obstacles
5. Room type and overall layout

Be precise about spatial positions — this will be stored in memory for robot navigation."""
    ])
    return response.text


async def describe_current_scene(base64_image: str, voice_command: str) -> str:
    """Analyze the current camera frame in context of the voice command."""
    image_bytes = base64.b64decode(base64_image)
    response = await _model.generate_content_async([
        {"mime_type": "image/jpeg", "data": image_bytes},
        f"""User navigation command: "{voice_command}"

Analyze the current camera frame for robot navigation:
1. What objects are visible and where (left/center/right, near/far)
2. Is the target object from the command visible? If yes, where exactly?
3. What is directly ahead — clear path or obstacle?
4. Any spatial reference points (walls, furniture edges, open spaces)

Keep concise and focused on navigation-relevant information."""
    ])
    return response.text
