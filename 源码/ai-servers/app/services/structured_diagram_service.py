"""Structured diagram generation aligned with App AI Studio routes."""

from __future__ import annotations

from typing import Any, Dict

from fastapi import HTTPException

from app.services.java_backend import java_backend_retriever

MIND_MAP_AGENT_NAME = "diagram_mind_map_agent"
FLOWCHART_AGENT_NAME = "diagram_flowchart_agent"
ARCHITECTURE_AGENT_NAME = "diagram_architecture_agent"

# Keep above Java LLM timeout (120s) so BusinessException responses can propagate.
_DIAGRAM_JAVA_TIMEOUT_SECONDS = 140


def _unwrap_java_result(payload: Any) -> Dict[str, Any]:
    if not isinstance(payload, dict):
        return {}
    code = payload.get("code")
    data = payload.get("data")
    if code in (200, "200") and isinstance(data, dict):
        return data
    if code not in (None, 200, "200") and payload.get("msg"):
        raise HTTPException(status_code=502, detail=str(payload.get("msg")))
    return payload if isinstance(payload, dict) else {}


def _require_java_payload(result: Any, empty_detail: str) -> Dict[str, Any]:
    if not isinstance(result, dict) or not result:
        raise HTTPException(status_code=502, detail=empty_detail)
    return result


def generate_mind_map(authorization: str, input_text: str) -> Dict[str, Any]:
    text = str(input_text or "").strip()
    if not text:
        raise HTTPException(status_code=400, detail="思维导图生成缺少输入内容")
    payload = {
        "topic": text[:4000],
        "centerTopicMode": "AUTO",
        "depth": "AUTO",
        "structure": "AUTO",
        "detail": "AUTO",
    }
    result = java_backend_retriever.post_json(
        "/api/ai/mindmap/generate",
        authorization,
        payload,
        timeout_seconds=_DIAGRAM_JAVA_TIMEOUT_SECONDS,
    )
    result = _require_java_payload(
        result,
        "思维导图生成超时或后端暂时不可用，请稍后重试",
    )
    diagram = _unwrap_java_result(result)
    if not diagram.get("nodes"):
        raise HTTPException(status_code=502, detail="思维导图生成失败，未返回有效节点结构")
    return diagram


def generate_flowchart(authorization: str, input_text: str) -> Dict[str, Any]:
    text = str(input_text or "").strip()
    if not text:
        raise HTTPException(status_code=400, detail="流程图生成缺少输入内容")
    payload = {
        "description": text[:4000],
        "content": text[:4000],
        "sceneType": "AUTO",
        "processType": "AUTO",
        "diagramType": "AUTO",
        "nodeGranularity": "AUTO",
        "nodeLevel": "AUTO",
        "layoutDirection": "AUTO",
        "decisionMode": "AUTO",
        "swimlaneMode": "AUTO",
        "swimlane": "AUTO",
    }
    result = java_backend_retriever.post_json(
        "/api/ai/flowchart/generate",
        authorization,
        payload,
        timeout_seconds=_DIAGRAM_JAVA_TIMEOUT_SECONDS,
    )
    result = _require_java_payload(
        result,
        "流程图生成超时或后端暂时不可用，请稍后重试",
    )
    diagram = _unwrap_java_result(result)
    if not diagram.get("nodes"):
        raise HTTPException(status_code=502, detail="流程图生成失败，未返回有效节点结构")
    return diagram


def generate_architecture(authorization: str, input_text: str) -> Dict[str, Any]:
    text = str(input_text or "").strip()
    if not text:
        raise HTTPException(status_code=400, detail="架构图生成缺少输入内容")
    payload = {
        "description": text[:8000],
        "content": text[:8000],
        "systemType": "WEB",
        "architectureStyle": "AUTO",
        "autoArchitectureLayers": True,
        "architectureLayers": [],
        "layers": [],
        "focusContents": [],
        "displayContent": [],
        "relationMode": "AUTO",
        "relationType": "AUTO",
        "hierarchyMode": "STRUCTURED",
    }
    result = java_backend_retriever.post_json(
        "/api/ai/architecture/generate",
        authorization,
        payload,
        timeout_seconds=_DIAGRAM_JAVA_TIMEOUT_SECONDS,
    )
    result = _require_java_payload(
        result,
        "架构图生成超时或后端暂时不可用，请稍后重试",
    )
    diagram = _unwrap_java_result(result)
    layers = diagram.get("layers")
    if not isinstance(layers, list) or not layers:
        raise HTTPException(status_code=502, detail="架构图生成失败，未返回有效层级结构")
    return diagram
