from app.image_generation.qwen_provider import QwenImageProvider
from app.models.image_generation import ImageGenerationRequest


def _request() -> ImageGenerationRequest:
    return ImageGenerationRequest(
        prompt="Python 循环思维导图",
        model="qwen-image-plus",
        baseUrl="https://example.com",
        apiKey="test-key",
    )


def _response_for(result):
    provider = QwenImageProvider()
    return provider._build_response(
        "provider-task",
        {"output": {"task_status": "SUCCEEDED", "results": [result]}},
        request=_request(),
        task_id="local-task",
        mode="single",
    )


def test_image_item_propagates_explicit_content_type():
    response = _response_for({
        "url": "https://example.com/generated/image",
        "content_type": "image/webp; charset=binary",
    })

    assert response.images[0].contentType == "image/webp"


def test_image_item_infers_content_type_from_known_url_extension():
    response = _response_for({
        "url": "https://example.com/generated/image.webp?signature=redacted",
    })

    assert response.images[0].contentType == "image/webp"


def test_image_item_does_not_fabricate_png_for_unknown_url():
    response = _response_for({
        "url": "https://example.com/generated/image",
    })

    assert response.images[0].contentType == ""


def test_wan_size_keeps_width_before_height():
    # DashScope 的 size 是 ``宽*高``；曾经把两者写反，导致横版请求出成竖版图。
    assert QwenImageProvider._normalize_wan_size("1664x928", "wan2.7-image") == "1664*928"


def test_wan_size_clamps_oversized_canvas():
    assert QwenImageProvider._normalize_wan_size("4096x4096", "wan2.7-image") == "1024*1024"


def test_wan_size_falls_back_to_default_square():
    assert QwenImageProvider._normalize_wan_size("", "wan2.7-image") == "1328*1328"
