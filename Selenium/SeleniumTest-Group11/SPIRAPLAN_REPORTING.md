# SpiraPlan reporting for Maven Surefire

The project uses the locally installed `spira-addons-xunit` package to submit
test runs. Its bundled XML parser searches for descendant `<testsuite>` nodes;
Maven Surefire reports commonly use `<testsuite>` as the document root, so that
parser can skip every testcase. `scripts/report_surefire_to_spira.py` handles
both a `<testsuite>` root and a `<testsuites>` container, then hands mapped
results to the add-on's Spira REST client.

Keep `spira.cfg` in the project directory and out of Git. It uses this format:

```ini
[credentials]
url = https://<Spira-host>/<tenant-path>
username = <Spira username>
token = <Spira API key>
project_id = <project ID>
release_id = <release ID>

[test_cases]
fully.qualified.TestClass.testMethod = <Spira test case ID>
```

Keep existing mappings in `spira.cfg`. Additional non-secret mappings can be
stored in `spira-additional-mappings.json` beside that file, using a JSON object
whose keys are fully qualified test method names and whose values are Spira test
case IDs. The reporting script merges this file when it exists and rejects
duplicate method mappings or duplicate Spira test case IDs across both files.
For example:

```json
{
  "tests.BillPayTest.billPayRejectsMissingAmount": 55142,
  "tests.OpenNewAccountTest.customerCanVerifyNewAccountDetails": 55144,
  "tests.BillPayTest.billPayRejectsMissingPayee": 55147
}
```

The URL is normally the Spira base URL, including an application path when
required. For this RMIT instance, the uploader uses
`https://rmit.spiraservice.net` as its REST API base and omits the `/1197` UI
path. It applies this base in memory and does not rewrite `spira.cfg`. The
installed package appends `/Services/v6_0/RestService.svc/` and the REST resource
path. The token must be copied with its outer curly braces. The uploader checks
for those delimiters before any POST and never prints the token. Do not put
credentials in command arguments or source files.

After running the Maven tests, first validate locally:

```powershell
python scripts/report_surefire_to_spira.py
```

The default is a dry run: it parses `target/surefire-reports/TEST-*.xml`, checks
that every configured test case is present exactly once, and shows the test
method to Spira ID mapping. Once that output is correct, upload once with:

```powershell
python scripts/report_surefire_to_spira.py --upload
```

The uploader deliberately does not create a Spira build. Raw network exception
details are suppressed because the installed add-on puts the API key in the
request query string. The wrapper reports only a safe category (DNS, TLS,
proxy, refused/reset connection, timeout, or other request failure). Requests
have a 30-second timeout and are never retried. If an upload is not confirmed,
check SpiraPlan for created test runs before deciding what to do next, since a
timeout, connection reset, or server error may happen after acceptance.
The uploader does not follow HTTP redirects, so login redirects remain visible
as their original HTTP status instead of being mistaken for a final API response.
