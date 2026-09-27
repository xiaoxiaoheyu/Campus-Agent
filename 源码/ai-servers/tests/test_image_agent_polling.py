from types import SimpleNamespace

from app.multi_agents.image_agent import agent as image_agent_module


def _response(status: str, task_id: str = "img-local-task"):
    payload = {
        "taskId": task_id,
        "providerTaskId": "provider-task",
        "mode": "single",
        "status": status,
        "prompt": "Python 学习路径信息图",
        "images": ([{"index": 0, "url": "https://example.com/image.png", "status": "success"}]
                   if status == "success" else []),
        "message": "生成完成" if status == "success" else "图片任务仍在生成中",
    }
    return SimpleNamespace(status=status, taskId=task_id, model_dump=lambda: payload)


def test_generate_images_polls_local_task_again_when_first_window_is_still_running(monkeypatch):
    class FakeProvider:
        def __init__(self):
            self.requested_task_ids = []

        def generate(self, request):
            return _response("running")

        def get_task(self, task_id):
            self.requested_task_ids.append(task_id)
            return _response("success", task_id)

    provider = FakeProvider()
    monkeypatch.setattr(image_agent_module, "get_qwen_image_provider", lambda: provider)
    monkeypatch.setattr(image_agent_module, "get_active_llm_config", lambda: None)

    result = image_agent_module.ImageAgent().generate_images("Python 学习路径信息图", [])

    assert result["status"] == "success"
    assert result["images"][0]["status"] == "success"
    assert provider.requested_task_ids == ["img-local-task"]


def test_generate_images_does_not_poll_again_after_immediate_success(monkeypatch):
    class FakeProvider:
        def generate(self, request):
            return _response("success")

        def get_task(self, task_id):
            raise AssertionError("completed image task must not be polled again")

    monkeypatch.setattr(image_agent_module, "get_qwen_image_provider", lambda: FakeProvider())
    monkeypatch.setattr(image_agent_module, "get_active_llm_config", lambda: None)

    result = image_agent_module.ImageAgent().generate_images("Python 学习路径信息图", [])

    assert result["status"] == "success"
