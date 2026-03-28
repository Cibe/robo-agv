"""Navigation agent — Gemini with function calling for robot movement decisions."""
import os
import google.generativeai as genai
import google.ai.generativelanguage as glm
from dotenv import load_dotenv
from agents.memory_agent import query_memory

load_dotenv()
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))

_NAV_TOOLS = [
    glm.Tool(function_declarations=[
        glm.FunctionDeclaration(
            name="query_room_memory",
            description="Query the stored room memory to find spatial information about object locations and room layout.",
            parameters=glm.Schema(
                type=glm.Type.OBJECT,
                properties={
                    "query": glm.Schema(
                        type=glm.Type.STRING,
                        description="Spatial query, e.g. 'Where is the microwave?' or 'What is to the left of the fridge?'"
                    )
                },
                required=["query"]
            )
        ),
        glm.FunctionDeclaration(
            name="navigate",
            description="Issue a navigation command to move the robot one step toward the target.",
            parameters=glm.Schema(
                type=glm.Type.OBJECT,
                properties={
                    "direction": glm.Schema(
                        type=glm.Type.STRING,
                        description="Movement direction: forward, back, left, right, or stop"
                    ),
                    "speech_text": glm.Schema(
                        type=glm.Type.STRING,
                        description="What to say to the user explaining the action"
                    ),
                    "reasoning": glm.Schema(
                        type=glm.Type.STRING,
                        description="Brief internal reasoning for this navigation decision"
                    )
                },
                required=["direction", "speech_text", "reasoning"]
            )
        )
    ])
]

_model = genai.GenerativeModel(
    model_name="gemini-3.0-flash",
    tools=_NAV_TOOLS,
    generation_config=genai.GenerationConfig(temperature=0.2)
)


async def decide_navigation(voice_command: str, scene_description: str) -> dict:
    """Run an agentic loop: query memory → reason → issue navigation command."""
    chat = _model.start_chat()

    prompt = f"""You are a robot navigation AI. Help the robot respond to the user's command.

USER COMMAND: "{voice_command}"

CURRENT CAMERA VIEW:
{scene_description}

Steps:
1. Call query_room_memory to find where the target object/destination is stored in memory
2. Compare the stored location with what is visible in the current camera view
3. Call navigate with the best single movement direction (forward/back/left/right/stop)

Be decisive. Use the navigate tool to issue exactly one movement command."""

    response = await chat.send_message_async(prompt)

    for _ in range(6):  # Max 6 iterations
        parts = response.candidates[0].content.parts
        fn_calls = [p.function_call for p in parts if p.function_call.name]

        if not fn_calls:
            # No function calls — extract text fallback
            break

        tool_responses = []
        navigate_result = None

        for fc in fn_calls:
            args = dict(fc.args)
            if fc.name == "navigate":
                direction = args.get("direction", "stop")
                # Normalize direction
                if direction not in ("forward", "back", "left", "right", "stop"):
                    direction = "stop"
                navigate_result = {
                    "direction": direction,
                    "speech_text": args.get("speech_text", "Navigating"),
                    "reasoning": args.get("reasoning", "")
                }
                tool_responses.append(glm.Part(
                    function_response=glm.FunctionResponse(
                        name=fc.name,
                        response={"result": "Navigation command issued"}
                    )
                ))
            elif fc.name == "query_room_memory":
                result = await query_memory(args.get("query", ""))
                tool_responses.append(glm.Part(
                    function_response=glm.FunctionResponse(
                        name=fc.name,
                        response={"result": result}
                    )
                ))

        if navigate_result:
            return navigate_result

        response = await chat.send_message_async(tool_responses)

    return {
        "direction": "stop",
        "speech_text": "I could not determine the navigation direction. Please try again or record the room first.",
        "reasoning": "Insufficient memory or unclear command"
    }
