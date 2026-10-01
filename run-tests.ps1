<#
.SYNOPSIS
    Compiles the Cave Explorer project and runs the public tests.

.DESCRIPTION
    1. Checks the test name you asked for (if any) so a typo fails immediately.
    2. Deletes old compiled classes from bin\ so nothing stale can survive a rename or delete.
    3. Compiles every .java file under src\ into bin\ (the checker and JUnit jars are on the classpath).
    4. Runs the JUnit tests and prints a short result.

    It always works on the project folder it lives in, so you can run it from any folder.

    Exit codes (handy for scripts):
        0  every selected test passed
        1  at least one test failed
        2  no test matched the name you asked for
        3  compile error, missing Java, or the test run did not finish normally

.PARAMETER Test
    Optional. One test to run, either the short name (uc1) or the full method name (test_plan_uc1).

.PARAMETER Full
    Also print the long failure details (stack traces) that are hidden by default.

.EXAMPLE
    .\run-tests.ps1                 compile, then run all 21 public tests
.EXAMPLE
    .\run-tests.ps1 uc1             compile, then run only test_plan_uc1
.EXAMPLE
    .\run-tests.ps1 as_cost2 -Full  run one test and show the full failure details
#>
param(
    [string]$Test = "",
    [switch]$Full
)

# Plain-text only in this file: Windows PowerShell 5.1 misreads non-ASCII characters in scripts.

function Invoke-CompileAndTest {
    # --- 0. Make sure Java is available ------------------------------------------------------
    foreach ($tool in @("java", "javac")) {
        if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
            Write-Host "ERROR: '$tool' was not found. Install a JDK (17 or newer) and add it to your PATH." -ForegroundColor Red
            return 3
        }
    }

    # --- 1. Choose which tests to run (and reject typos before doing any work) ---------------
    if ($Test -eq "") {
        $selector = @("--select-class", "tests.PublicTests")
        $what = "all public tests"
    } else {
        $method = if ($Test.StartsWith("test_")) { $Test } else { "test_plan_$Test" }
        # The list of real test names is read straight from the test file.
        $known = @(Select-String -Path "src\tests\PublicTests.java" -Pattern 'public void (test_\w+)\(' |
                   ForEach-Object { $_.Matches[0].Groups[1].Value })
        if ($known -notcontains $method) {
            Write-Host "RESULT: there is no test called '$method'. Available tests:" -ForegroundColor Red
            Write-Host ("  " + (($known -replace '^test_plan_', '') -join ", "))
            return 2
        }
        # PowerShell ignores letter case in the check above, but JUnit does not: use the exact spelling from the file.
        $method = @($known | Where-Object { $_ -ieq $method })[0]
        $selector = @("--select-method", "tests.PublicTests#$method")
        $what = $method
    }

    # --- 2. Clean build: remove old .class files ---------------------------------------------
    # Only compiled files inside bin\ are touched. bin\ is listed in .gitignore, so it is never committed.
    if (Test-Path "bin") {
        Get-ChildItem -Path "bin" -Recurse -Filter "*.class" | Remove-Item -Force
    }
    New-Item -ItemType Directory -Force -Path "bin" | Out-Null

    # --- 3. Compile --------------------------------------------------------------------------
    # "lib\*" puts every jar in lib\ on the classpath (the checker and JUnit).
    Write-Host "== Compiling src\ into bin\ ..." -ForegroundColor Cyan
    $sources = Get-ChildItem -Path "src" -Recurse -Filter "*.java" | ForEach-Object { Resolve-Path -Relative $_.FullName }
    # "| Out-Host" prints the compiler messages straight to the screen so they cannot leak into this function's return value.
    & javac -encoding UTF-8 -cp "lib\*" -d "bin" $sources | Out-Host
    if ($LASTEXITCODE -ne 0) {
        Write-Host "COMPILE FAILED: fix the errors above, then run this script again." -ForegroundColor Red
        return 3
    }
    Write-Host "Compiled $(@($sources).Count) source files." -ForegroundColor Green

    # --- 4. Run JUnit ------------------------------------------------------------------------
    Write-Host "== Running $what ..." -ForegroundColor Cyan
    # --fail-if-no-tests makes JUnit exit with code 2 if it ever finds nothing to run.
    $junitArgs = @(
        "-jar", "lib\junit-platform-console-standalone.jar", "execute",
        "--class-path", "bin$([IO.Path]::PathSeparator)lib\checker-obf.jar"
    ) + $selector + @(
        "--details=tree", "--details-theme=ascii", "--disable-ansi-colors", "--disable-banner", "--fail-if-no-tests"
    )

    # Stream the output live. After the "Failures (N):" heading JUnit prints one long stack
    # trace per failed test; hide those (unless -Full) until the "Test run finished" line.
    $found = $null; $passed = $null; $failed = $null; $hidden = $false; $showing = $true
    & java @junitArgs | ForEach-Object {
        $line = [string]$_
        if ($line -match '^Failures \(\d+\)') {
            if (-not $Full) { $showing = $false; $hidden = $true }
        } elseif ($line -match '^Test run finished') {
            $showing = $true
        }
        if ($line -match '^\[\s*(\d+) tests found') { $found = [int]$Matches[1] }
        if ($line -match '^\[\s*(\d+) tests successful') { $passed = [int]$Matches[1] }
        if ($line -match '^\[\s*(\d+) tests failed') { $failed = [int]$Matches[1] }
        if ($showing) { Write-Host $line }
    }
    $junitExit = $LASTEXITCODE

    # --- 5. Short verdict --------------------------------------------------------------------
    Write-Host ""
    if ($null -eq $found) {
        Write-Host "RESULT: the test run did not finish normally. Read the output above." -ForegroundColor Red
        return 3
    }
    if ($junitExit -eq 0) {
        Write-Host "RESULT: PASSED - $passed of $found tests passed." -ForegroundColor Green
        return 0
    }
    Write-Host "RESULT: FAILED - $passed passed, $failed failed (of $found)." -ForegroundColor Red
    if ($hidden) { Write-Host "(Long failure details are hidden. Add -Full to see them.)" -ForegroundColor DarkGray }
    return 1
}

# Always work inside the project folder (the folder this script is in), then go back afterwards.
$root = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
Push-Location $root
$code = 3
try {
    $code = Invoke-CompileAndTest
} finally {
    Pop-Location
}
exit $code
