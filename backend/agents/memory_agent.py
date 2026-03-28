"""Memory agent — stores room descriptions and answers spatial queries via Gemini."""
import os
import google.generativeai as genai
from dotenv import load_dotenv
from memory.store import MemoryStore

load_dotenv()
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))

_model = genai.GenerativeModel("gemini-2.0-flash")
_store = MemoryStore()


def add_room_description(description: str, image_index: int = 0) -> None:
    _store.add(description, {"image_index": image_index})


async def query_memory(query: str) -> str:
    """Use Gemini to semantically search stored room descriptions and answer a spatial query."""
    all_descriptions = _store.get_combined_description()
    if all_descriptions == "No room information stored.":
        return "No room information stored. Please record the room first using the Record Room feature."

    response = await _model.generate_content_async(
        f"""You are a spatial memory retrieval system for a robot.

STORED ROOM MEMORY (from multiple camera scans):
{all_descriptions}

SPATIAL QUERY: {query}

Answer concisely with specific directional information (left/right/forward/back, near/far).
If the object is not found in memory, say so clearly."""
    )
    return response.text


def clear_memory() -> None:
    _store.clear()


def get_all_descriptions() -> list:
    return _store.get_all()
