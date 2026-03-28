import os
import json
from typing import List, Dict
from datetime import datetime

_BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
_DATA_DIR = os.path.join(_BASE_DIR, "data")
_MEMORY_FILE = os.path.join(_DATA_DIR, "room_memory.json")


class MemoryStore:
    def __init__(self):
        os.makedirs(_DATA_DIR, exist_ok=True)
        if not os.path.exists(_MEMORY_FILE):
            self._save([])

    def add(self, description: str, metadata: Dict = None) -> None:
        entries = self.get_all()
        entries.append({
            "id": len(entries),
            "description": description,
            "metadata": metadata or {},
            "timestamp": datetime.now().isoformat()
        })
        self._save(entries)

    def get_all(self) -> List[Dict]:
        try:
            with open(_MEMORY_FILE, "r") as f:
                return json.load(f)
        except Exception:
            return []

    def clear(self) -> None:
        self._save([])

    def get_combined_description(self) -> str:
        entries = self.get_all()
        if not entries:
            return "No room information stored."
        return "\n\n".join(
            f"[Scene {e['id'] + 1}]\n{e['description']}" for e in entries
        )

    def _save(self, entries: List[Dict]) -> None:
        with open(_MEMORY_FILE, "w") as f:
            json.dump(entries, f, indent=2)
