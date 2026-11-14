# 一键编译：WeChatBillUnlock (LSPosed 模块)
# 用法：右键 → 使用 PowerShell 运行，或 .\build.ps1

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

# 工具链：Gradle 9.7.1 / AGP 9.4.1 / Kotlin 2.4.20，推荐 JDK 21。
# JDK 17 也能用；JDK 22+ 未验证，脚本里先排除，避免踩坑。
function Test-JdkOk([string]$dir) {
    $java = Join-Path $dir 'bin\java.exe'
    if (-not (Test-Path $java)) { return $false }
    # java -version 往 stderr 写；在 $ErrorActionPreference='Stop' 下会被当成
    # 终止性错误，所以临时放开并用文件收集输出。
    $old = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $tmp = [System.IO.Path]::GetTempFileName()
    try {
        & $java -version *> $tmp
        $out = Get-Content $tmp -Raw -ErrorAction SilentlyContinue
        if ($out -match 'version "(\d+)') {
            $major = [int]$matches[1]
            return ($major -ge 17 -and $major -le 21)
        }
    } catch {
    } finally {
        Remove-Item $tmp -Force -ErrorAction SilentlyContinue
        $ErrorActionPreference = $old
    }
    return $false
}

function Find-Jdk {
    $candidates = @(
        "C:\Users\$env:USERNAME\.jdks\jbr-21*",
        "C:\Users\$env:USERNAME\.jdks\jbr-17*",
        "C:\Program Files\Eclipse Adoptium\jdk-21*",
        "C:\Program Files\Eclipse Adoptium\jdk-17*",
        "C:\Program Files\Java\jdk-21*",
        "C:\Program Files\Java\jdk-17*",
        "C:\Program Files\Android\Android Studio\jbr",
        "$env:LOCALAPPDATA\Programs\Android Studio\jbr",
        $env:JAVA_HOME
    )
    foreach ($c in $candidates) {
        if (-not $c) { continue }
        foreach ($d in (Get-Item $c -ErrorAction SilentlyContinue)) {
            if (Test-JdkOk $d.FullName) { return $d.FullName }
        }
    }
    return $null
}

function Find-Sdk {
    $candidates = @(
        $env:ANDROID_HOME,
        $env:ANDROID_SDK_ROOT,
        "$env:LOCALAPPDATA\Android\Sdk",
        "C:\Android\Sdk"
    )
    foreach ($c in $candidates) {
        if ($c -and (Test-Path (Join-Path $c 'platforms'))) { return $c }
    }
    return $null
}

$jdk = Find-Jdk
if (-not $jdk) {
    Write-Host "[!] 找不到 JDK 17~21（推荐 JDK 21）。" -ForegroundColor Yellow
    Write-Host "    winget install --id EclipseAdoptium.Temurin.21.JDK" -ForegroundColor Gray
    Write-Host "    或直接用 Android Studio 打开本目录编译（Studio 自带 JBR 21）。" -ForegroundColor Gray
    exit 1
}
Write-Host "[+] JDK     : $jdk" -ForegroundColor Green
$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$env:PATH"

$sdk = Find-Sdk
if (-not $sdk) {
    Write-Host "[!] 找不到 Android SDK。装 Android Studio 或用 sdkmanager 装命令行工具。" -ForegroundColor Yellow
    exit 1
}
Write-Host "[+] SDK     : $sdk" -ForegroundColor Green
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk

"sdk.dir=$($sdk -replace '\\','\\')" | Set-Content -Path (Join-Path $root 'local.properties') -Encoding ASCII

if (-not (Test-Path (Join-Path $root 'gradlew.bat'))) {
    Write-Host "[!] 缺 gradlew.bat，请先用 Android Studio 打开一次本目录。" -ForegroundColor Yellow
    exit 1
}

Write-Host "[+] 开始编译 ..." -ForegroundColor Cyan
& (Join-Path $root 'gradlew.bat') :app:distApk

# 产物固定在 dist\，文件名带版本号（版本号来自 Const.kt）
$apk = Get-ChildItem -Path (Join-Path $root 'dist') -Filter *.apk -ErrorAction SilentlyContinue |
       Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($apk) {
    Write-Host ""
    Write-Host "[OK] 产物: $($apk.FullName)" -ForegroundColor Green
    Write-Host "     安装: adb install -r `"$($apk.FullName)`"" -ForegroundColor Gray
} else {
    Write-Host "[!] 没找到 apk，检查上面的编译输出。" -ForegroundColor Red
    exit 1
}
