import pytest

from app import analytics


@pytest.fixture(autouse=True)
def reset_funnel_counts():
    analytics.reset()
    yield
    analytics.reset()
