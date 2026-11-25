# Run once on the repository owner's Windows PC with GitHub CLI and a JDK installed.
$ErrorActionPreference = "Stop"
$repo = "Fatal-Kode/Wholphin"
Get-Command gh, keytool -ErrorAction Stop | Out-Null
$existing = gh secret list --repo $repo --json name | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw "Sign in with gh auth login and run this script again." }
$names = @("SIGNING_KEY", "KEY_ALIAS", "KEY_PASSWORD", "KEY_STORE_PASSWORD")
$folder = Join-Path $env:LOCALAPPDATA "Wholphin-SD-signing"
$store = Join-Path $folder "updates.p12"
$passwordFile = Join-Path $folder "password.dpapi"
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
        keytool -genkeypair -alias wholphin-sd -keystore $store -storetype PKCS12 -storepass:env WHOLPHIN_STORE_PASS -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=Wholphin SD"
        if ($LASTEXITCODE -ne 0) { throw "Key generation failed." }
    } finally { Remove-Item Env:WHOLPHIN_STORE_PASS }
}
$secure = Get-Content $passwordFile | ConvertTo-SecureString
$password = (New-Object System.Net.NetworkCredential("", $secure)).Password
$secrets = @{
    SIGNING_KEY = [Convert]::ToBase64String([IO.File]::ReadAllBytes($store))
    KEY_ALIAS = "wholphin-sd"
    KEY_PASSWORD = $password
    KEY_STORE_PASSWORD = $password
}
foreach ($name in $names) {
    $secrets[$name] | gh secret set $name --repo $repo
    if ($LASTEXITCODE -ne 0) { throw "Could not save $name in GitHub. Retry on this PC." }
}
# The workflow exists on the SD branch, so a rerun also works on forks where manual dispatch is unavailable.
$runs = gh api "repos/$repo/actions/runs?branch=sd-deinterlace&per_page=30" | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw "Could not find the SD update workflow." }
$run = $runs.workflow_runs | Where-Object { $_.path -eq ".github/workflows/sd-deinterlace.yml" } | Select-Object -First 1
if (!$run) { throw "No SD build found on the fork." }
gh run rerun $run.id --repo $repo
if ($LASTEXITCODE -ne 0) { throw "Signing is configured. Rerun the SD build after the current run finishes." }
Write-Host "Signing secrets configured. Keep the signing files in $folder."
