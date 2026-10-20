#!/usr/bin/env python3
"""Parse Maven Surefire XML and optionally report mapped cases to SpiraPlan."""

from __future__ import annotations

import argparse
import configparser
import datetime
import errno
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


class ReportingError(Exception):
    """An error safe to show without including configuration or request data."""


SPIRA_API_BASE_URL = "https://rmit.spiraservice.net"
ADDITIONAL_MAPPINGS_FILENAME = "spira-additional-mappings.json"


def classify_request_exception(exc: Exception, requests_module) -> str:
    """Return a safe transport category without rendering exception text."""
    exceptions = requests_module.exceptions
    if isinstance(exc, exceptions.ProxyError):
        return "proxy"
    if isinstance(exc, exceptions.SSLError):
        return "TLS/certificate"
    if isinstance(exc, exceptions.Timeout):
        return "timeout"

    pending = [exc]
    seen: set[int] = set()
    types: set[str] = set()
    errnos: set[int] = set()
    while pending:
        current = pending.pop()
        if id(current) in seen:
            continue
        seen.add(id(current))
        types.add(type(current).__name__.lower())
        code = getattr(current, "errno", None)
        winerror = getattr(current, "winerror", None)
        if isinstance(code, int):
            errnos.add(code)
        if isinstance(winerror, int):
            errnos.add(winerror)
        for attr in ("reason", "__cause__", "__context__"):
            nested = getattr(current, attr, None)
            if isinstance(nested, BaseException):
                pending.append(nested)
        for arg in getattr(current, "args", ()):
            if isinstance(arg, BaseException):
                pending.append(arg)

    if types.intersection({"nameresolutionerror", "gaierror"}):
        return "DNS resolution"
    if types.intersection({"connectionrefusederror", "connectionreseterror"}) or errnos.intersection(
        {errno.ECONNREFUSED, errno.ECONNRESET, 10061, 10054}
    ):
        return "connection refused/reset"
    if isinstance(exc, exceptions.ConnectionError):
        return "unclassified connection failure"
    if isinstance(exc, exceptions.RequestException):
        return "other request exception"
    return "other request exception"


def parse_mapping_entries(entries: dict[str, object], source: str) -> dict[str, int]:
    mappings: dict[str, int] = {}
    for key, value in entries.items():
        if not isinstance(key, str) or not key.strip():
            raise ReportingError(f"{source} contains an empty test method mapping")
        normalized = key.strip().lower()
        if normalized in mappings:
            raise ReportingError(f"{source} contains a duplicate test method mapping: {key}")
        if isinstance(value, bool):
            raise ReportingError(f"Test case mapping for '{key}' in {source} must be an integer")
        try:
            case_id = int(value) if isinstance(value, int) else int(str(value).strip())
        except (TypeError, ValueError):
            raise ReportingError(f"Test case mapping for '{key}' in {source} must be an integer") from None
        if case_id < 1:
            raise ReportingError(f"Test case mapping for '{key}' in {source} must be positive")
        mappings[normalized] = case_id
    return mappings


def validate_unique_case_ids(mappings: dict[str, int]) -> None:
    methods_by_id: dict[int, str] = {}
    for method, case_id in mappings.items():
        if case_id in methods_by_id:
            raise ReportingError(
                f"Duplicate Spira test case ID {case_id} is mapped to multiple test methods"
            )
        methods_by_id[case_id] = method


def read_additional_mappings(mapping_path: Path) -> dict[str, int]:
    if not mapping_path.exists():
        return {}

    class DuplicateJSONMappingKey(ValueError):
        pass

    def reject_duplicate_json_keys(pairs: list[tuple[str, object]]) -> dict[str, object]:
        entries: dict[str, object] = {}
        normalized_keys: set[str] = set()
        for key, value in pairs:
            normalized = key.strip().lower() if isinstance(key, str) else ""
            if normalized in normalized_keys:
                raise DuplicateJSONMappingKey
            normalized_keys.add(normalized)
            entries[key] = value
        return entries

    try:
        entries = json.loads(
            mapping_path.read_text(encoding="utf-8"),
            object_pairs_hook=reject_duplicate_json_keys,
        )
    except DuplicateJSONMappingKey:
        raise ReportingError(
            "The additional Spira mapping file contains duplicate test method mappings"
        ) from None
    except Exception:
        raise ReportingError("Unable to parse the additional Spira mapping file") from None
    if not isinstance(entries, dict):
        raise ReportingError("The additional Spira mapping file must be a JSON object")
    return parse_mapping_entries(entries, "additional mapping file")


def merge_mappings(existing: dict[str, int], additional: dict[str, int]) -> dict[str, int]:
    merged = dict(existing)
    for method, case_id in additional.items():
        if method in merged:
            raise ReportingError(f"Duplicate test method mapping across mapping files: {method}")
        merged[method] = case_id
    validate_unique_case_ids(merged)
    return merged


def read_config(
    config_path: Path, additional_mappings_path: Path | None = None
) -> tuple[configparser.ConfigParser, dict[str, int]]:
    config = configparser.ConfigParser()
    try:
        loaded = config.read(config_path, encoding="utf-8")
    except configparser.DuplicateOptionError as exc:
        if exc.section.lower() == "test_cases":
            raise ReportingError(
                f"spira.cfg contains a duplicate test method mapping: {exc.option}"
            ) from None
        raise ReportingError("Unable to parse the Spira configuration; details suppressed.") from None
    except Exception:
        raise ReportingError("Unable to parse the Spira configuration; details suppressed.") from None
    if not loaded:
        raise ReportingError(f"Unable to read configuration file: {config_path}")
    if not config.has_section("credentials") or not config.has_section("test_cases"):
        raise ReportingError("spira.cfg must contain [credentials] and [test_cases] sections")
    if not config.get("credentials", "url", fallback="").strip():
        raise ReportingError("[credentials] url is required")
    for key in ("username", "token", "project_id", "release_id"):
        if not config.get("credentials", key, fallback="").strip():
            raise ReportingError(f"[credentials] {key} is required")
    for key in ("project_id", "release_id"):
        if not config.get("credentials", key).strip().isdigit():
            raise ReportingError(f"[credentials] {key} must be an integer")

    mappings = parse_mapping_entries(dict(config.items("test_cases")), "spira.cfg")
    if not mappings:
        raise ReportingError("[test_cases] must contain at least one mapping")
    mapping_path = additional_mappings_path or config_path.with_name(ADDITIONAL_MAPPINGS_FILENAME)
    mappings = merge_mappings(mappings, read_additional_mappings(mapping_path))
    return config, mappings


def validate_api_key_format(config: configparser.ConfigParser) -> None:
    """Check Spira's required key delimiters without ever displaying the key."""
    token = config.get("credentials", "token", fallback="")
    if len(token) < 3 or not (token.startswith("{") and token.endswith("}")):
        raise ReportingError(
            "[credentials] token must include the full Spira API key, including its outer curly braces; value not shown"
        )


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def parse_reports(report_paths: list[Path], mappings: dict[str, int]) -> tuple[list[dict], set[str]]:
    results: list[dict] = []
    seen: set[str] = set()

    for report_path in report_paths:
        root = ET.parse(report_path).getroot()
        # The installed add-on searches only descendants and misses a
        # document-root <testsuite>, which is the Maven Surefire layout here.
        suites = [root] if local_name(root.tag) == "testsuite" else [
            node for node in root.iter() if local_name(node.tag) == "testsuite"
        ]

        for suite in suites:
            suite_name = suite.get("name", "")
            for testcase in suite:
                if local_name(testcase.tag) != "testcase":
                    continue
                classname = testcase.get("classname", "")
                method = testcase.get("name", "")
                fullname = f"{classname}.{method}"
                normalized = fullname.lower()
                test_case_id = mappings.get(normalized)
                if test_case_id is None:
                    continue
                if normalized in seen:
                    raise ReportingError(f"Duplicate mapped testcase found: {fullname}")
                seen.add(normalized)

                try:
                    duration = float(testcase.get("time", "0"))
                    assertion_count = int(testcase.get("assertions", "0") or "0")
                except ValueError:
                    raise ReportingError(f"Invalid duration/assertion count for {fullname}") from None
                if duration < 0 or assertion_count < 0:
                    raise ReportingError(f"Invalid duration/assertion count for {fullname}")

                message = "Success"
                details = "Nothing Reported\n"
                status_id = 2  # Passed, per the installed add-on's status mapping.
                # Priority matches the installed parser: failure, warning, error, skipped.
                for tag, mapped_status, default_message in (
                    ("failure", 1, "Failure"),
                    ("warning", 6, "Warning"),
                    ("error", 5, "Error"),
                    ("skipped", 4, "Skipped"),
                ):
                    node = next((child for child in testcase
                                 if local_name(child.tag) == tag), None)
                    if node is not None:
                        status_id = mapped_status
                        message = node.get("message") or default_message
                        details = node.text or message
                        if assertion_count == 0:
                            assertion_count = 1
                        break

                results.append({
                    "test_case_id": test_case_id,
                    "name": fullname,
                    "execution_status_id": status_id,
                    "stack_trace": details,
                    "message": message,
                    "duration_seconds": duration,
                    "assert_count": assertion_count,
                    "suite_name": suite_name,
                    "source_file": str(report_path),
                })

    return results, seen


def validate_mappings(results: list[dict], seen: set[str], mappings: dict[str, int]) -> None:
    missing = sorted(set(mappings) - seen)
    if missing:
        raise ReportingError("Mapped test cases were not found in the reports: " + ", ".join(missing))
    if len(results) != len(mappings):
        raise ReportingError(f"Expected {len(mappings)} mapped test cases, parsed {len(results)}")
    ids = [result["test_case_id"] for result in results]
    if len(ids) != len(set(ids)):
        raise ReportingError("Multiple configured test methods map to the same Spira test case ID")


def upload_results(results: list[dict], config_path: Path) -> None:
    """Use the add-on's request model, but report each accepted run explicitly."""
    try:
        import requests
        import spira_xunit_reader
    except ImportError:
        raise ReportingError("The installed spira-addons-xunit package and requests are required") from None

    try:
        package_config = spira_xunit_reader.getConfig(str(config_path))
    except Exception:
        raise ReportingError("Unable to load Spira configuration for the installed add-on") from None
    token = package_config.get("token", "")
    if len(token) < 3 or not (token.startswith("{") and token.endswith("}")):
        raise ReportingError(
            "[credentials] token must include the full Spira API key, including its outer curly braces; value not shown"
        )
    # The RMIT REST API is served at the host root; /1197 is the UI route and
    # redirects API requests to Login.aspx. Keep spira.cfg unchanged.
    package_config["url"] = SPIRA_API_BASE_URL
    package_config["create_build"] = False
    original_post = requests.post
    last_http_status: int | None = None

    def sanitized_post(*post_args, **post_kwargs):
        nonlocal last_http_status
        # The add-on does not set a timeout. Bound the wait while retaining
        # its exact URL, authentication parameters, headers, and request body.
        post_kwargs.setdefault("timeout", 30)
        # Do not let Requests turn a login redirect into a follow-up response
        # (often HTML/200) that obscures the original API status.
        post_kwargs["allow_redirects"] = False
        try:
            response = original_post(*post_args, **post_kwargs)
        except requests.RequestException as exc:
            category = classify_request_exception(exc, requests)
            raise ReportingError(
                f"Spira request failed ({category}); acceptance is unknown. "
                "Check SpiraPlan before retrying."
            ) from None
        last_http_status = response.status_code
        if response.status_code != 200:
            # The add-on prints response.reason for non-200 responses. Keep any
            # server-supplied diagnostic out of console output.
            response.reason = "HTTP response"
        return response

    requests.post = sanitized_post
    accepted_ids: list[int] = []
    try:
        for result in results:
            last_http_status = None
            started = datetime.datetime.now(datetime.UTC)
            ended = started + datetime.timedelta(seconds=result["duration_seconds"])
            try:
                test_run = spira_xunit_reader.SpiraTestRun(
                    package_config["project_id"],
                    result["test_case_id"],
                    result["name"],
                    result["stack_trace"],
                    result["execution_status_id"],
                    started,
                    ended,
                    message=result["message"],
                    release_id=package_config["release_id"],
                    test_set_id=-1,
                    assert_count=result["assert_count"],
                    build_id=-1,
                )
                test_run_id = test_run.post(
                    package_config["url"], package_config["username"], package_config["token"]
                )
            except ReportingError:
                raise
            except Exception:
                status = last_http_status
                if status in (401, 403):
                    raise ReportingError(
                        f"Spira rejected authentication (HTTP {status}); no result was confirmed."
                    ) from None
                raise ReportingError(
                    "Spira response could not be validated; acceptance is unknown. "
                    "Check SpiraPlan before retrying."
                ) from None

            if (not isinstance(test_run_id, int) or isinstance(test_run_id, bool)
                    or test_run_id < 1):
                status = last_http_status
                if status in (401, 403):
                    raise ReportingError(
                        f"Spira rejected authentication (HTTP {status}); no result was confirmed."
                    )
                if status is not None and status >= 500:
                    raise ReportingError(
                        f"Spira returned HTTP {status}; acceptance is unknown. Check SpiraPlan before retrying."
                    )
                if status is not None:
                    raise ReportingError(f"Spira rejected the test result (HTTP {status}).")
                raise ReportingError("Spira did not return a valid TestRunId; acceptance was not confirmed.")
            accepted_ids.append(test_run_id)
            print(f"Accepted Spira test run {test_run_id} for test case {result['test_case_id']}.")
    finally:
        requests.post = original_post

    if len(accepted_ids) != len(results):
        raise ReportingError("Spira did not confirm every test run; check SpiraPlan before retrying.")
    print(f"Spira confirmed {len(accepted_ids)} test run(s).")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", type=Path, default=Path("spira.cfg"))
    parser.add_argument("--reports", type=Path, default=Path("target/surefire-reports"))
    parser.add_argument("--upload", action="store_true",
                        help="send validated results once; no automatic retries")
    args = parser.parse_args(argv)

    try:
        config, mappings = read_config(args.config)
        report_paths = sorted(args.reports.glob("TEST-*.xml"))
        if not report_paths:
            raise ReportingError(f"No Surefire XML reports found in {args.reports}")

        results, seen = parse_reports(report_paths, mappings)
        validate_mappings(results, seen, mappings)
        print(f"Validated {len(results)} mapped Surefire test case(s):")
        for result in results:
            print(f"  {result['name']} -> Spira test case {result['test_case_id']} "
                  f"(status {result['execution_status_id']}, {result['duration_seconds']:.3f}s)")

        if not args.upload:
            print("Dry run only; no results were sent. Add --upload after confirming the rotated token is configured.")
            return 0

        validate_api_key_format(config)
        upload_results(results, args.config)
        return 0
    except ReportingError as exc:
        print(f"SpiraPlan reporting failed: {exc}", file=sys.stderr)
        return 2
    except Exception:
        # Unexpected parser/config/package errors may contain credential data.
        print("SpiraPlan reporting failed; details suppressed to protect credentials.", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
