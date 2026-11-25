# Run once on the repository owner's Windows PC with GitHub CLI and a JDK installed.
$ErrorActionPreference = "Stop"
$repo = "Fatal-Kode/Wholphin"
Get-Command gh, keytool -ErrorAction Stop | Out-Null
$existing = gh secret list --repo $repo --json name | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw "Sign in with gh auth login and run this script again." }
$names = @("SIGNING_KEY", "KEY_ALIAS", "KEY_PASSWORD", "KEY_STORE_PASSWORD")
$folder = Join-Path $env:LOCALAPPDATA "Wholphin-signing"
$legacyFolder = Join-Path $env:LOCALAPPDATA "Wholphin-SD-signing"
$alias = "wholphin-release"
if (!(Test-Path (Join-Path $folder "updates.p12")) -and (Test-Path (Join-Path $legacyFolder "updates.p12"))) {
    $folder = $legacyFolder
    $alias = "wholphin-sd"
}
$store = Join-Path $folder "updates.p12"
$passwordFile = Join-Path $folder "password.dpapi"
if ($existing | Where-Object { $_.name -eq "WHOLPHIN_SIGNING_BUNDLE" }) {
    throw "The repository already uses a signing bundle. Keep using the existing key."
}
if (($existing | Where-Object { $_.name -in $names }) -and !(Test-Path $store)) {
    throw "The repository already has signing secrets. Use the original signing key."
}
if (!(Test-Path $store)) {
    New-Item -ItemType Directory -Force $folder | Out-Null
    $random = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($random) } finally { $rng.Dispose() }
    $password = [Convert]::ToBase64String($random)
    ConvertTo-SecureString $password -AsPlainText -Force | ConvertFrom-SecureString | Set-Content $passwordFile
    $env:WHOLPHIN_STORE_PASS = $password
    try {
        keytool -genkeypair -alias $alias -keystore $store -storetype PKCS12 -storepass:env WHOLPHIN_STORE_PASS -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=Wholphin"
        if ($LASTEXITCODE -ne 0) { throw "Key generation failed." }
    } finally { Remove-Item Env:WHOLPHIN_STORE_PASS }
}
$secure = Get-Content $passwordFile | ConvertTo-SecureString
$password = (New-Object System.Net.NetworkCredential("", $secure)).Password
$secrets = @{
    SIGNING_KEY = [Convert]::ToBase64String([IO.File]::ReadAllBytes($store))
    KEY_ALIAS = $alias
    KEY_PASSWORD = $password
    KEY_STORE_PASSWORD = $password
}
foreach ($name in $names) {
    $secrets[$name] | gh secret set $name --repo $repo
    if ($LASTEXITCODE -ne 0) { throw "Could not save $name in GitHub. Retry on this PC." }
}
gh workflow run android-tv-release.yml --ref main --repo $repo
if ($LASTEXITCODE -ne 0) { throw "Signing is configured. Start Wholphin release from the Actions page." }
Write-Host "Signing secrets configured. Keep the signing files in $folder."
