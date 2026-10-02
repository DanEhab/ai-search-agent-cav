<#
.SYNOPSIS
    Builds the zip for the course form: the src folder plus a .txt with the team members.

.DESCRIPTION
    Runs the public tests first and stops if one fails, checks that src only holds the packages
    code and tests, then writes submission\Assignment1.zip. The zip holds src\ and team.txt.
    Your own copy of the files is never changed. The submission folder is ignored by git.

.PARAMETER Members
    One entry per team member, with the name and the ID.

.EXAMPLE
    .\make-submission.ps1 -Members "Jane Doe - 49-1234", "John Roe - 49-5678"
#>
param(
    [string[]]$Members = @()
)

# Plain-text only in this file: Windows PowerShell 5.1 misreads non-ASCII characters in scripts.

$root = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
Push-Location $root
try {
    if ($Members.Count -eq 0) {
        Write-Host 'ERROR: give the names and IDs, for example -Members "Jane Doe - 49-1234", "John Roe - 49-5678"' -ForegroundColor Red
        exit 2
    }

    # --- 1. The tests have to pass -----------------------------------------------------------
    & "$root\run-tests.ps1" | Out-Host
    if ($LASTEXITCODE -ne 0) {
        Write-Host "STOPPED: the tests do not pass, so there is nothing to submit yet." -ForegroundColor Red
        exit 1
    }

    # --- 2. src may only hold the packages code and tests, with java files in them -----------
    $folders = @(Get-ChildItem -Path "src" -Directory | ForEach-Object { $_.Name } | Sort-Object)
    if (($folders -join ",") -ne "code,tests") {
        Write-Host "STOPPED: src should hold exactly the folders code and tests, but holds: $($folders -join ', ')" -ForegroundColor Red
        exit 1
    }
    foreach ($file in Get-ChildItem -Path "src" -Recurse -File) {
        $package = (Get-Content -Path $file.FullName -TotalCount 1)
        if ($file.Extension -ne ".java" -or $package -ne "package $($file.Directory.Name);") {
            Write-Host "STOPPED: $($file.FullName) does not fit, src may only hold java files that start with the package line of their folder." -ForegroundColor Red
            exit 1
        }
    }

    # --- 3. Write the zip ---------------------------------------------------------------------
    $folder = Join-Path $root "submission"
    New-Item -ItemType Directory -Force -Path $folder | Out-Null
    $teamFile = Join-Path $folder "team.txt"
    [IO.File]::WriteAllLines($teamFile, $Members)
    $zipPath = Join-Path $folder "Assignment1.zip"
    if (Test-Path $zipPath) {
        Remove-Item $zipPath
    }

    # Entries are named with / on purpose: the zip command of PowerShell 5.1 uses \, which unzips
    # badly on Linux and macOS.
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $srcPath = (Resolve-Path "src").Path
    $zip = [IO.Compression.ZipFile]::Open($zipPath, "Create")
    try {
        [void][IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $teamFile, "team.txt")
        foreach ($file in Get-ChildItem -Path "src" -Recurse -File) {
            $name = "src/" + $file.FullName.Substring($srcPath.Length + 1).Replace("\", "/")
            [void][IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $file.FullName, $name)
        }
    } finally {
        $zip.Dispose()
    }

    # --- 4. Look inside the zip we just made --------------------------------------------------
    $zip = [IO.Compression.ZipFile]::OpenRead($zipPath)
    try {
        $names = @($zip.Entries | ForEach-Object { $_.FullName })
    } finally {
        $zip.Dispose()
    }
    Write-Host ""
    Write-Host "== $zipPath holds $($names.Count) files:" -ForegroundColor Cyan
    $names | ForEach-Object { Write-Host "   $_" }
    Write-Host "Send this zip through the course form yourself." -ForegroundColor Green
} finally {
    Pop-Location
}
