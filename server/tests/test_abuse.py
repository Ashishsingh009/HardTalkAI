from app.abuse import SlidingWindowLimiter, VOICE_CLIENT_HEADER, VOICE_CLIENT_VALUE


def test_sliding_window_limiter_caps_requests():
    limiter = SlidingWindowLimiter(max_requests=2, window_seconds=60)
    assert limiter.allow("1.1.1.1", now=100.0) is True
    assert limiter.allow("1.1.1.1", now=101.0) is True
    assert limiter.allow("1.1.1.1", now=102.0) is False
    assert limiter.allow("2.2.2.2", now=102.0) is True


def test_sliding_window_limiter_expires_hits():
    limiter = SlidingWindowLimiter(max_requests=1, window_seconds=10)
    assert limiter.allow("ip", now=0.0) is True
    assert limiter.allow("ip", now=9.0) is False
    assert limiter.allow("ip", now=10.1) is True


def test_client_header_constants_match_app():
    assert VOICE_CLIENT_HEADER == "X-HardTalk-Client"
    assert VOICE_CLIENT_VALUE == "hardtalk-app"
