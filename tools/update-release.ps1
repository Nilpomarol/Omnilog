[CmdletBinding()]
param(
    [string]$KeystorePath = (Join-Path $env:USERPROFILE 'Documents\AndroidKeys\omnilog-release')
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$resolvedKeystore = (Resolve-Path -LiteralPath $KeystorePath).Path
$javaHome = if ($env:JAVA_HOME) {
    $env:JAVA_HOME
} else {
    'C:\Program Files\Android\Android Studio\jbr'
}
$keytool = Join-Path $javaHome 'bin\keytool.exe'
$sdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$adb = Join-Path $sdkRoot 'platform-tools\adb.exe'
$gradle = Join-Path $repoRoot 'gradlew.bat'
$releaseApk = Join-Path $repoRoot 'app\build\outputs\apk\release\app-release.apk'
$installedCopy = Join-Path $env:TEMP 'omnilog-installed-release.apk'
$resultPath = Join-Path $repoRoot 'app\build\release-update-result.txt'

if (-not (Test-Path -LiteralPath $keytool)) {
    throw "keytool was not found at $keytool"
}
if (-not (Test-Path -LiteralPath $adb)) {
    throw "adb was not found at $adb"
}

function ConvertFrom-SecureValue([Security.SecureString]$Value) {
    return [System.Net.NetworkCredential]::new('', $Value).Password
}

function Read-CertificateDigest([string]$ApkPath, [string]$ApkSigner) {
    $certificateOutput = & $ApkSigner verify --print-certs $ApkPath 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Could not verify $ApkPath`n$($certificateOutput -join [Environment]::NewLine)"
    }
    $digest = $certificateOutput |
        Select-String -Pattern 'SHA-256 digest:\s*([0-9a-f]+)' |
        Select-Object -First 1
    if ($digest -eq $null) {
        throw "No signing certificate was found in $ApkPath"
    }
    return $digest.Matches[0].Groups[1].Value
}

$storeSecret = Read-Host 'Release-keystore password' -AsSecureString
$storePassword = ConvertFrom-SecureValue $storeSecret
if ([string]::IsNullOrEmpty($storePassword)) {
    throw 'The release-keystore password cannot be empty.'
}
$keySecret = Read-Host 'Key password (press Enter if it is the same)' -AsSecureString
$keyPassword = ConvertFrom-SecureValue $keySecret
if ([string]::IsNullOrEmpty($keyPassword)) {
    $keyPassword = $storePassword
}

$env:OMNILOG_RELEASE_KEYSTORE = $resolvedKeystore
$env:OMNILOG_RELEASE_STORE_PASSWORD = $storePassword
$env:OMNILOG_RELEASE_KEY_PASSWORD = $keyPassword

try {
    $keytoolOutput = & $keytool '-J-Duser.language=en' -list -v -storetype PKCS12 `
        -keystore $resolvedKeystore -storepass:env OMNILOG_RELEASE_STORE_PASSWORD 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "The keystore could not be opened. Check its password."
    }
    $aliases = @(
        $keytoolOutput |
            Select-String -Pattern '^Alias name:\s*(.+)$' |
            ForEach-Object { $_.Matches[0].Groups[1].Value.Trim() }
    )
    if ($aliases.Count -eq 0) {
        throw 'No signing alias was found in the keystore.'
    }
    $alias = if ($aliases.Count -eq 1) {
        $aliases[0]
    } else {
        $selected = Read-Host "Key alias ($($aliases -join ', '))"
        if ($selected -notin $aliases) {
            throw "The keystore does not contain alias '$selected'."
        }
        $selected
    }
    $env:OMNILOG_RELEASE_KEY_ALIAS = $alias

    Push-Location $repoRoot
    try {
        & $gradle assembleRelease
        if ($LASTEXITCODE -ne 0) {
            throw 'The signed release build failed.'
        }
    } finally {
        Pop-Location
    }

    $apksigner = Get-ChildItem -Path (Join-Path $sdkRoot 'build-tools') -Recurse `
        -Filter 'apksigner.bat' |
        Sort-Object FullName -Descending |
        Select-Object -First 1 -ExpandProperty FullName
    if ([string]::IsNullOrEmpty($apksigner)) {
        throw 'apksigner was not found in the Android SDK.'
    }

    $installedPathOutput = & $adb shell pm path com.nilpo.contenttracker 2>&1
    if ($LASTEXITCODE -ne 0 -or $installedPathOutput -notmatch '^package:') {
        throw 'The Omnilog release app is not installed on the connected phone.'
    }
    $installedPath = ($installedPathOutput | Select-Object -First 1) -replace '^package:', ''
    & $adb pull $installedPath $installedCopy | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'The installed APK could not be read for its certificate check.'
    }

    $newDigest = Read-CertificateDigest $releaseApk $apksigner
    $installedDigest = Read-CertificateDigest $installedCopy $apksigner
    if ($newDigest -ne $installedDigest) {
        throw 'The new APK does not match the installed app signing certificate. Nothing was installed.'
    }

    & $adb install -r $releaseApk
    if ($LASTEXITCODE -ne 0) {
        throw 'Android rejected the release update.'
    }
    Write-Host 'Omnilog release updated successfully; app data was preserved.' -ForegroundColor Green
    Set-Content -LiteralPath $resultPath -Value "SUCCESS $([DateTimeOffset]::Now.ToString('O'))"
} catch {
    Set-Content -LiteralPath $resultPath -Value "FAILED $($_.Exception.Message)"
    throw
} finally {
    Remove-Item Env:OMNILOG_RELEASE_KEYSTORE -ErrorAction SilentlyContinue
    Remove-Item Env:OMNILOG_RELEASE_STORE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:OMNILOG_RELEASE_KEY_ALIAS -ErrorAction SilentlyContinue
    Remove-Item Env:OMNILOG_RELEASE_KEY_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $installedCopy -ErrorAction SilentlyContinue
    $storePassword = $null
    $keyPassword = $null
}
