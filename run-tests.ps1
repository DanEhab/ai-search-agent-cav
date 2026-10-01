<#
.SYNOPSIS
    Compiles the Cave Explorer project and runs the tests (the public ones by default).

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
    Optional. One test to run. For the public tests the short name works (uc1 means test_plan_uc1).

.PARAMETER Class
    Which test file to use, without .java. The default is PublicTests. Our own tests live in ModelTests and RulesTests.

.PARAMETER All
    Run every test file in src\tests (cannot be combined with -Test, -Class or -Own).

.PARAMETER Own
    Run all of our own test files, which means every file in src\tests except PublicTests
    (cannot be combined with -Test, -Class or -All).

.PARAMETER Full
    Also print the long failure details (stack traces) that are hidden by default.

.EXAMPLE
    .\run-tests.ps1                              compile, then run all 21 public tests
.EXAMPLE
    .\run-tests.ps1 uc1                          compile, then run only test_plan_uc1
.EXAMPLE
    .\run-tests.ps1 -Class ModelTests            run our own tests
.EXAMPLE
    .\run-tests.ps1 -Class ModelTests -Test pdfExampleIsReadCorrectly
.EXAMPLE
    .\run-tests.ps1 -Own                         run all of our own tests (not the public ones)
.EXAMPLE
    .\run-tests.ps1 -All                         run the public tests and our own tests together
.EXAMPLE
    .\run-tests.ps1 as_cost2 -Full               run one test and show the full failure details
#>
param(
    [string]$Test = "",
    [string]$Class = "PublicTests",
    [switch]$All,
    [switch]$Own,
    [switch]$Full
)

# Plain-text only in this file: Windows PowerShell 5.1 misreads non-ASCII characters in scripts.

# Remember if -Class was typed (a function cannot see this by itself).
$classWasGiven = $PSBoundParameters.ContainsKey("Class")

function Invoke-CompileAndTest {
    # --- 0. Make sure Java is available ------------------------------------------------------
    foreach ($tool in @("java", "javac")) {
        if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
            Write-Host "ERROR: '$tool' was not found. Install a JDK (17 or newer) and add it to your PATH." -ForegroundColor Red
            return 3
        }
    }

    # --- 1. Choose which tests to run (and reject typos before doing any work) ---------------
    if ($All -or $Own) {
        if ($Test -ne "" -or $classWasGiven -or ($All -and $Own)) {
            Write-Host "ERROR: -All and -Own cannot be combined with each other, or with -Test or -Class." -ForegroundColor Red
            return 3
        }
        if ($All) {
            $selector = @("--select-package", "tests")
            $what = "every test file in src\tests"
        } else {
            # our own tests are every test file except the public ones
            $selector = @()
            $ownFiles = @(Get-ChildItem -Path "src\tests" -Filter "*.java" | Where-Object { $_.BaseName -ne "PublicTests" })
            foreach ($file in $ownFiles) {
                $selector += @("--select-class", "tests.$($file.BaseName)")
            }
            $what = "our own tests ($(($ownFiles | ForEach-Object { $_.BaseName }) -join ', '))"
        }
    } else {
        # Look the file up by listing the folder: that gives its real spelling, which matters because
        # Windows ignores letter case in file names but JUnit does not.
        $testFiles = @(Get-ChildItem -Path "src\tests" -Filter "*.java")
        $classFileInfo = @($testFiles | Where-Object { $_.BaseName -ieq $Class })[0]
        if (-not $classFileInfo) {
            Write-Host "RESULT: there is no test file called '$Class'. Available: $(($testFiles | ForEach-Object { $_.BaseName }) -join ', ')" -ForegroundColor Red
            return 2
        }
        $Class = $classFileInfo.BaseName
        $classFile = $classFileInfo.FullName
        if ($Test -eq "") {
            $selector = @("--select-class", "tests.$Class")
            $what = "all tests in $Class"
        } else {
            # The real test names are read straight from the test file (every @Test method).
            $text = Get-Content -Path $classFile -Raw
            $known = @([regex]::Matches($text, '@Test\s+(?:public\s+)?void\s+(\w+)\s*\(') |
                       ForEach-Object { $_.Groups[1].Value })
            # Accept the full name or the short one (uc1 for test_plan_uc1). PowerShell ignores letter case
            # here but JUnit does not, so keep the exact spelling from the file.
            $method = @($known | Where-Object { $_ -ieq $Test -or $_ -ieq "test_plan_$Test" })[0]
            if (-not $method) {
                Write-Host "RESULT: there is no test called '$Test' in $Class. Available tests:" -ForegroundColor Red
                Write-Host ("  " + (($known -replace '^test_plan_', '') -join ", "))
                Write-Host "Tip: -Class picks another test file, for example -Class ModelTests" -ForegroundColor DarkGray
                return 2
            }
            $selector = @("--select-method", "tests.$Class#$method")
            $what = $method
        }
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
