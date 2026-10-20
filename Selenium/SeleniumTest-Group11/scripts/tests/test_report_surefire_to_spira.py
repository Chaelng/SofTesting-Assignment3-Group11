import contextlib
import io
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
import errno
import socket
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import report_surefire_to_spira as reporter


FAKE_TOKEN = "{offline-fixture-token-never-used-for-network}"


class ReportParserTests(unittest.TestCase):
    def test_root_testsuite_and_case_insensitive_mapping(self):
        xml = ET.fromstring(
            '<testsuite name="suite"><testcase classname="TESTS.LoginTest" '
            'name="validLogin" time="1.25"/></testsuite>'
        )
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "TEST.xml"
            ET.ElementTree(xml).write(path)
            mappings = {"tests.logintest.validlogin": 54353}
            results, seen = reporter.parse_reports([path], mappings)
        reporter.validate_mappings(results, seen, mappings)
        self.assertEqual(results[0]["test_case_id"], 54353)
        self.assertEqual(results[0]["execution_status_id"], 2)

    def test_testsuites_container_and_status_mapping(self):
        cases = {
            "passed": ("", 2),
            "failed": ('<failure message="assertion">details</failure>', 1),
            "errored": ('<error message="exception">details</error>', 5),
            "skipped": ('<skipped message="not run"/>', 4),
        }
        with tempfile.TemporaryDirectory() as directory:
            paths = []
            mappings = {}
            for index, (name, (child, _)) in enumerate(cases.items(), start=1):
                case_key = f"tests.Case{name}.{name}".lower()
                mappings[case_key] = 50000 + index
                root = ET.fromstring(
                    '<testsuites><testsuite name="suite">'
                    f'<testcase classname="tests.Case{name}" name="{name}" time="0">'
                    f'{child}</testcase></testsuite></testsuites>'
                )
                path = Path(directory) / f"{name}.xml"
                ET.ElementTree(root).write(path)
                paths.append(path)
            results, seen = reporter.parse_reports(paths, mappings)
        reporter.validate_mappings(results, seen, mappings)
        actual = {result["name"].rsplit(".", 1)[1]: result["execution_status_id"]
                  for result in results}
        self.assertEqual(actual, {name: value[1] for name, value in cases.items()})

    def test_configparser_lowercased_mapping_matches_case_insensitively(self):
        with tempfile.TemporaryDirectory() as directory:
            cfg = Path(directory) / "spira.cfg"
            cfg.write_text(
                "[credentials]\nurl=https://example.test/1197\nusername=local\n"
                f"token={FAKE_TOKEN}\nproject_id=4131735\nrelease_id=2992\n"
                "[test_cases]\nTests.LoginTest.ValidLogin=54353\n",
                encoding="utf-8",
            )
            _, mapping = reporter.read_config(cfg)
        self.assertEqual(mapping, {"tests.logintest.validlogin": 54353})

    def test_additional_json_mapping_merges_with_existing_mappings(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            cfg = root / "spira.cfg"
            cfg.write_text(
                "[credentials]\nurl=https://example.test\nusername=local\n"
                f"token={FAKE_TOKEN}\nproject_id=4131735\nrelease_id=2992\n"
                "[test_cases]\n"
                "tests.BillPayTest.customerCanPayBill=54355\n"
                "tests.OpenNewAccountTest.customerCanOpenCheckingAccount=54353\n",
                encoding="utf-8",
            )
            additional = root / reporter.ADDITIONAL_MAPPINGS_FILENAME
            additional.write_text(
                '{"tests.BillPayTest.billPayRejectsMissingAmount": 55142, '
                '"tests.OpenNewAccountTest.customerCanVerifyNewAccountDetails": 55144, '
                '"tests.BillPayTest.billPayRejectsMissingPayee": 55147}',
                encoding="utf-8",
            )

            _, mappings = reporter.read_config(cfg)

        self.assertEqual(mappings, {
            "tests.billpaytest.customercanpaybill": 54355,
            "tests.opennewaccounttest.customercanopencheckingaccount": 54353,
            "tests.billpaytest.billpayrejectsmissingamount": 55142,
            "tests.opennewaccounttest.customercanverifynewaccountdetails": 55144,
            "tests.billpaytest.billpayrejectsmissingpayee": 55147,
        })

    def test_duplicate_method_mapping_across_files_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            cfg = root / "spira.cfg"
            cfg.write_text(
                "[credentials]\nurl=https://example.test\nusername=local\n"
                f"token={FAKE_TOKEN}\nproject_id=4131735\nrelease_id=2992\n"
                "[test_cases]\ntests.BillPayTest.customerCanPayBill=54355\n",
                encoding="utf-8",
            )
            additional = root / reporter.ADDITIONAL_MAPPINGS_FILENAME
            additional.write_text(
                '{"Tests.BillPayTest.CustomerCanPayBill": 55142}',
                encoding="utf-8",
            )

            with self.assertRaisesRegex(reporter.ReportingError, "Duplicate test method mapping"):
                reporter.read_config(cfg)

    def test_duplicate_case_insensitive_method_in_spira_config_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            cfg = Path(directory) / "spira.cfg"
            cfg.write_text(
                "[credentials]\nurl=https://example.test\nusername=local\n"
                f"token={FAKE_TOKEN}\nproject_id=4131735\nrelease_id=2992\n"
                "[test_cases]\ntests.BillPayTest.customerCanPayBill=54355\n"
                "Tests.BillPayTest.CustomerCanPayBill=54356\n",
                encoding="utf-8",
            )

            with self.assertRaisesRegex(reporter.ReportingError, "duplicate test method mapping"):
                reporter.read_config(cfg)

    def test_duplicate_case_id_across_files_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            cfg = root / "spira.cfg"
            cfg.write_text(
                "[credentials]\nurl=https://example.test\nusername=local\n"
                f"token={FAKE_TOKEN}\nproject_id=4131735\nrelease_id=2992\n"
                "[test_cases]\ntests.BillPayTest.customerCanPayBill=54355\n",
                encoding="utf-8",
            )
            additional = root / reporter.ADDITIONAL_MAPPINGS_FILENAME
            additional.write_text(
                '{"tests.BillPayTest.billPayRejectsMissingAmount": 54355}',
                encoding="utf-8",
            )

            with self.assertRaisesRegex(reporter.ReportingError, "Duplicate Spira test case ID 54355"):
                reporter.read_config(cfg)

    def test_duplicate_case_insensitive_json_keys_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            mapping_path = Path(directory) / reporter.ADDITIONAL_MAPPINGS_FILENAME
            mapping_path.write_text(
                '{"tests.BillPayTest.billPayRejectsMissingAmount": 55142, '
                '"Tests.BillPayTest.BillPayRejectsMissingAmount": 55143}',
                encoding="utf-8",
            )

            with self.assertRaisesRegex(reporter.ReportingError, "duplicate test method mappings"):
                reporter.read_additional_mappings(mapping_path)

    def test_unbraced_token_fails_preflight_without_echoing_value(self):
        with tempfile.TemporaryDirectory() as directory:
            cfg = Path(directory) / "spira.cfg"
            cfg.write_text(
                "[credentials]\nurl=https://example.test/1197\nusername=local\n"
                "token=unbraced-offline-fixture\nproject_id=4131735\nrelease_id=2992\n"
                "[test_cases]\ntests.LoginTest.validLogin=54353\n",
                encoding="utf-8",
            )
            config, _ = reporter.read_config(cfg)
        with self.assertRaises(reporter.ReportingError) as caught:
            reporter.validate_api_key_format(config)
        self.assertIn("curly braces", str(caught.exception))
        self.assertNotIn("unbraced-offline-fixture", str(caught.exception))

    def test_braced_token_passes_preflight(self):
        with tempfile.TemporaryDirectory() as directory:
            cfg = Path(directory) / "spira.cfg"
            cfg.write_text(
                "[credentials]\nurl=https://example.test/1197\nusername=local\n"
                f"token={FAKE_TOKEN}\nproject_id=4131735\nrelease_id=2992\n"
                "[test_cases]\ntests.LoginTest.validLogin=54353\n",
                encoding="utf-8",
            )
            config, _ = reporter.read_config(cfg)
        reporter.validate_api_key_format(config)

    def test_upload_mode_stops_before_uploader_for_unbraced_token(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            cfg = root / "spira.cfg"
            cfg.write_text(
                "[credentials]\nurl=https://example.test/1197\nusername=local\n"
                "token=unbraced-offline-fixture\nproject_id=4131735\nrelease_id=2992\n"
                "[test_cases]\ntests.LoginTest.validLogin=54353\n",
                encoding="utf-8",
            )
            reports = root / "reports"
            reports.mkdir()
            (reports / "TEST-login.xml").write_text(
                '<testsuite><testcase classname="tests.LoginTest" '
                'name="validLogin" time="0"/></testsuite>',
                encoding="utf-8",
            )
            output = io.StringIO()
            errors = io.StringIO()
            with patch.object(reporter, "upload_results") as upload, \
                 contextlib.redirect_stdout(output), contextlib.redirect_stderr(errors):
                result = reporter.main(["--config", str(cfg), "--reports", str(reports), "--upload"])
            self.assertEqual(result, 2)
            upload.assert_not_called()
            self.assertIn("curly braces", errors.getvalue())
            self.assertNotIn("unbraced-offline-fixture", output.getvalue() + errors.getvalue())

    def test_missing_mapped_case_fails_validation(self):
        with self.assertRaises(reporter.ReportingError):
            reporter.validate_mappings([], set(), {"tests.login.valid": 54353})


class UploadHandlingTests(unittest.TestCase):
    def setUp(self):
        import spira_xunit_reader
        self.reader = spira_xunit_reader
        self.old_config = self.reader.config
        self.reader.config = None
        self.temp = tempfile.TemporaryDirectory()
        self.config_path = Path(self.temp.name) / "spira.cfg"
        self.config_path.write_text(
            "[credentials]\nurl=https://example.test/1197\nusername=offline\n"
            f"token={FAKE_TOKEN}\nproject_id=4131735\nrelease_id=2992\n"
            "[test_cases]\ntests.SampleTest.passes=54353\n",
            encoding="utf-8",
        )
        self.result = {
            "test_case_id": 54353,
            "name": "tests.SampleTest.passes",
            "execution_status_id": 2,
            "stack_trace": "Nothing Reported\n",
            "message": "Success",
            "duration_seconds": 0.1,
            "assert_count": 0,
        }

    def tearDown(self):
        self.reader.config = self.old_config
        self.temp.cleanup()

    @staticmethod
    def response(status, body=None, reason="fixture"):
        class FakeResponse:
            def __init__(self):
                self.status_code = status
                self.reason = reason

            def json(self):
                if body is None:
                    raise ValueError("fixture response has no JSON")
                return body
        return FakeResponse()

    def run_with_post(self, post):
        from requests import post as _unused  # ensure requests is installed
        del _unused
        stdout = io.StringIO()
        stderr = io.StringIO()
        with patch("requests.post", side_effect=post), \
             contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
            try:
                reporter.upload_results([self.result.copy()], self.config_path)
                error = None
            except reporter.ReportingError as exc:
                error = str(exc)
        return stdout.getvalue() + stderr.getvalue(), error

    def test_http_200_requires_valid_test_run_id(self):
        calls = []

        def success(*args, **kwargs):
            calls.append((args, kwargs))
            return self.response(200, {"TestRunId": 881})

        output, error = self.run_with_post(success)
        self.assertIsNone(error)
        self.assertIn("Spira confirmed 1 test run(s)", output)
        request_args, request_kwargs = calls[0]
        self.assertEqual(
            request_args[0],
            "https://rmit.spiraservice.net/Services/v6_0/RestService.svc/"
            "projects/4131735/test-runs/record",
        )
        self.assertNotIn("/1197", request_args[0])
        self.assertEqual(request_kwargs["allow_redirects"], False)

    def test_http_redirect_is_not_followed_or_treated_as_success(self):
        calls = []

        def redirect(*args, **kwargs):
            calls.append(kwargs)
            return self.response(302, {}, reason=FAKE_TOKEN)

        output, error = self.run_with_post(redirect)
        self.assertIn("HTTP 302", error)
        self.assertEqual(len(calls), 1)
        self.assertIs(calls[0]["allow_redirects"], False)
        self.assertNotIn(FAKE_TOKEN, output + error)

    def test_http_200_without_test_run_id_is_not_success(self):
        output, error = self.run_with_post(lambda *a, **k: self.response(200, {"OtherId": 881}))
        self.assertIsNotNone(error)
        self.assertNotIn("confirmed 1", output)

    def test_authentication_failure_is_reported_without_response_text(self):
        output, error = self.run_with_post(
            lambda *a, **k: self.response(401, {}, reason=FAKE_TOKEN)
        )
        self.assertIn("authentication", error.lower())
        self.assertNotIn(FAKE_TOKEN, output + error)

    def test_http_client_error_is_reported(self):
        output, error = self.run_with_post(lambda *a, **k: self.response(400, {}, reason=FAKE_TOKEN))
        self.assertIn("HTTP 400", error)
        self.assertNotIn(FAKE_TOKEN, output + error)

    def test_http_server_error_is_ambiguous_and_not_retried(self):
        calls = []

        def server_error(*args, **kwargs):
            calls.append(kwargs)
            return self.response(503, {}, reason=FAKE_TOKEN)

        output, error = self.run_with_post(server_error)
        self.assertIn("HTTP 503", error)
        self.assertIn("acceptance is unknown", error)
        self.assertEqual(len(calls), 1)
        self.assertEqual(calls[0]["timeout"], 30)
        self.assertNotIn(FAKE_TOKEN, output + error)

    def test_timeout_is_ambiguous_and_never_exposes_request_exception(self):
        import requests

        def timeout(*args, **kwargs):
            raise requests.Timeout("URL contained " + FAKE_TOKEN)

        output, error = self.run_with_post(timeout)
        self.assertIn("timeout", error)
        self.assertIn("acceptance is unknown", error)
        self.assertNotIn(FAKE_TOKEN, output + error)

    def test_connection_failure_is_ambiguous_and_sanitized(self):
        import requests

        def connection_error(*args, **kwargs):
            raise requests.ConnectionError("URL contained " + FAKE_TOKEN)

        output, error = self.run_with_post(connection_error)
        self.assertIn("connection failure", error)
        self.assertNotIn(FAKE_TOKEN, output + error)

    def test_request_exception_categories_are_sanitized(self):
        import requests

        cases = [
            (requests.exceptions.Timeout(FAKE_TOKEN), "timeout"),
            (requests.exceptions.SSLError(FAKE_TOKEN), "TLS/certificate"),
            (requests.exceptions.ProxyError(FAKE_TOKEN), "proxy"),
            (requests.exceptions.ConnectionError(socket.gaierror(socket.EAI_NONAME, FAKE_TOKEN)),
             "DNS resolution"),
            (requests.exceptions.ConnectionError(ConnectionRefusedError(errno.ECONNREFUSED, FAKE_TOKEN)),
             "connection refused/reset"),
            (requests.exceptions.ConnectionError(ConnectionResetError(errno.ECONNRESET, FAKE_TOKEN)),
             "connection refused/reset"),
            (requests.exceptions.RequestException(FAKE_TOKEN), "other request exception"),
        ]
        for exception, expected in cases:
            with self.subTest(category=expected):
                category = reporter.classify_request_exception(exception, requests)
                self.assertEqual(category, expected)
                self.assertNotIn(FAKE_TOKEN, category)

    def test_dns_failure_diagnostic_does_not_expose_exception_text(self):
        import requests

        def dns_failure(*args, **kwargs):
            raise requests.ConnectionError(socket.gaierror(socket.EAI_NONAME, FAKE_TOKEN))

        output, error = self.run_with_post(dns_failure)
        self.assertIn("DNS resolution", error)
        self.assertIn("acceptance is unknown", error)
        self.assertNotIn(FAKE_TOKEN, output + error)


if __name__ == "__main__":
    unittest.main()
