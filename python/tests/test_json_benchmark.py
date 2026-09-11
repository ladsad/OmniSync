"""Benchmark comparing naive JSON processing vs OmniSync pipeline."""

import json
import time

from omnisync.jira.parser import parse_jira_search_response, stream_jira_issues


def generate_benchmark_payload(count: int = 1000) -> str:
    """Generate realistic Jira JSON response with extra unused fields."""
    issues = []
    for i in range(count):
        issues.append({
            "id": str(10000 + i),
            "key": f"PROJ-{i}",
            "fields": {
                "summary": f"Issue summary for benchmark item {i}",
                "status": {"name": "Open"},
                "issuetype": {"name": "Story"},
                "priority": {"name": "High"},
                "created": "2026-01-01T00:00:00.000Z",
                "updated": "2026-01-02T00:00:00.000Z",
                "assignee": {"displayName": f"Engineer {i % 10}"},
                "description": "Large text field containing description details that ingestion pipelines skip. " * 3,
                "customfield_10010": {"nested": f"custom value data {i}"},
                "customfield_10020": ["tag1", "tag2", "tag3"],
            },
        })
    return json.dumps({
        "startAt": 0,
        "maxResults": count,
        "total": count,
        "issues": issues,
    })


def test_json_parsing_pipeline_benchmark() -> None:
    """Benchmark Jira JSON parsing pipeline and lazy streaming."""
    issue_count = 1000
    payload = generate_benchmark_payload(issue_count)

    # Correctness verification
    result = parse_jira_search_response(payload)
    assert len(result.issues) == issue_count
    assert result.issues[0].key == "PROJ-0"
    assert result.issues[-1].key == f"PROJ-{issue_count - 1}"

    streamed = list(stream_jira_issues(payload))
    assert len(streamed) == issue_count

    # Warmup
    for _ in range(5):
        parse_jira_search_response(payload)

    # Measure batch parse
    iterations = 20
    t0 = time.perf_counter()
    for _ in range(iterations):
        parse_jira_search_response(payload)
    t_batch = (time.perf_counter() - t0) / iterations

    # Measure streaming generator
    t0 = time.perf_counter()
    for _ in range(iterations):
        for _ in stream_jira_issues(payload):
            pass
    t_stream = (time.perf_counter() - t0) / iterations

    payload_kb = len(payload.encode("utf-8")) / 1024.0

    print(
        f"\n=== PYTHON JSON PARSING PIPELINE BENCHMARK ===\n"
        f"Payload size:      {payload_kb:.2f} KB ({issue_count} issues)\n"
        f"Batch parse:       {t_batch * 1000:.2f} ms/op\n"
        f"Stream generator:  {t_stream * 1000:.2f} ms/op\n"
        f"Throughput:        {(payload_kb / 1024.0) / t_batch:.2f} MB/s\n"
        f"==============================================="
    )

    assert t_batch > 0
    assert t_stream > 0
