# 一键发版脚本：编译 release APK -> 打 tag -> 推送 -> 在 GitHub 建 Release 并上传 APK
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts\release.ps1 -Version 1.1.0 -Notes "修复了提醒延迟"
#
# 前置条件：
#   1. keystore.properties 存在（正式签名，不用提交）
#   2. 本机 git 已经能 push（凭据管理器里存过 GitHub 凭据）
#   3. 需要时设置代理：$env:HTTPS_PROXY = 'http://127.0.0.1:7897'

param(
    [Parameter(Mandatory = $true)][string]$Version,
    [string]$Notes = "",
    [string]$Proxy = "http://127.0.0.1:7897",
    [switch]$SkipPush
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$repo = (git remote get-url origin) -replace '^https://github.com/', '' -replace '\.git$', ''
$tag = "v$Version"
$apk = Join-Path $root 'app\build\outputs\apk\release\app-release.apk'

if ($Proxy) {
    $env:HTTPS_PROXY = $Proxy
    $env:HTTP_PROXY = $Proxy
}

Write-Host "==> 1/5 编译 release APK" -ForegroundColor Cyan
$env:JAVA_HOME = 'E:\demo\.tools\jdk-21.0.12.1+1'
$env:ANDROID_HOME = 'E:\Android\Sdk'
& (Join-Path $root 'gradlew.bat') assembleRelease --no-daemon
if ($LASTEXITCODE -ne 0) { throw '编译失败' }
if (-not (Test-Path $apk)) { throw "找不到 APK: $apk" }

Write-Host "==> 2/5 提交并打 tag $tag" -ForegroundColor Cyan
git add -A
$dirty = git status --porcelain
if ($dirty) { git commit -m "release: $tag" }
git tag -f $tag

if (-not $SkipPush) {
    Write-Host "==> 3/5 推送代码和 tag" -ForegroundColor Cyan
    git push origin main
    git push -f origin $tag
} else {
    Write-Host "==> 3/5 跳过推送（-SkipPush）" -ForegroundColor Yellow
}

Write-Host "==> 4/5 创建 GitHub Release" -ForegroundColor Cyan
# 从 Windows 凭据管理器里取出已经保存的 GitHub token（不打印、不落盘）
$cred = ("protocol=https`nhost=github.com`n`n" | git credential fill) -split "`n"
$tok = ($cred | Where-Object { $_ -like 'password=*' }) -replace 'password=',''
if (-not $tok) { throw '取不到 GitHub 凭据，请先手动 git push 一次' }

$headers = @{ Authorization = "token $tok"; Accept = 'application/vnd.github+json'; 'User-Agent' = 'release-script' }
$body = @{
    tag_name   = $tag
    name       = "$tag"
    body       = if ($Notes) { $Notes } else { "版本 $Version" }
    draft      = $false
    prerelease = $false
} | ConvertTo-Json

$proxyParam = if ($Proxy) { @{ Proxy = $Proxy } } else { @{} }
$rel = Invoke-RestMethod -Method Post `
    -Uri "https://api.github.com/repos/$repo/releases" `
    -Headers $headers -Body ([Text.Encoding]::UTF8.GetBytes($body)) `
    -ContentType 'application/json; charset=utf-8' -TimeoutSec 60 @proxyParam

Write-Host "==> 5/5 上传 APK" -ForegroundColor Cyan
$assetName = "A-Notebook-$Version.apk"
$up = Invoke-RestMethod -Method Post `
    -Uri "https://uploads.github.com/repos/$repo/releases/$($rel.id)/assets?name=$assetName" `
    -Headers @{ Authorization = "token $tok"; 'User-Agent' = 'release-script' } `
    -ContentType 'application/vnd.android.package-archive' -InFile $apk -TimeoutSec 600 @proxyParam

Write-Host ""
Write-Host "发布完成：$($rel.html_url)" -ForegroundColor Green
Write-Host "APK 直链：$($up.browser_download_url)" -ForegroundColor Green
