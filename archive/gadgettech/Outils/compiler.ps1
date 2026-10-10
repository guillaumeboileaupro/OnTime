# Compile ProchainMetro avec une barre de progression et les logs en direct.
#   .\Outils\compiler.ps1                      compile seulement
#   .\Outils\compiler.ps1 -Televerser          compile puis televerse sur COM4
#   .\Outils\compiler.ps1 -Televerser -Port COM5 -LogsCore
# -LogsCore : logs detailles du core ESP32 sur le port serie (Wi-Fi, TLS, HTTP...)
param(
  [switch]$Televerser,
  [string]$Port = "COM4",
  [switch]$LogsCore
)

$cli = "$env:LOCALAPPDATA\Programs\Arduino IDE\resources\app\lib\backend\resources\arduino-cli.exe"
$sketch = Join-Path $PSScriptRoot "..\ProchainMetro" | Resolve-Path
$build = "$env:LOCALAPPDATA\prochainmetro-build"  # meme dossier a chaque fois : le cache sert d'une compilation a l'autre
$fqbn = "esp32:esp32:esp32:PartitionScheme=min_spiffs"
if ($LogsCore) { $fqbn += ",DebugLevel=debug" }

function Barre([int]$pourcent) {
  $plein = [math]::Floor($pourcent / 4)
  return "[" + ("#" * $plein) + ("." * (25 - $plein)) + "] " + "$pourcent%".PadLeft(4)
}

# 1. Compter les fichiers a compiler (rapide : arduino-cli liste les commandes sans compiler)
Write-Host "Preparation : recherche des fichiers a compiler (long la premiere fois, ensuite en cache)..." -ForegroundColor Cyan
$preparation = Start-Process $cli -ArgumentList "compile --only-compilation-database --build-path `"$build`" --fqbn $fqbn `"$sketch`"" -NoNewWindow -PassThru -RedirectStandardOutput "$env:TEMP\prochainmetro-prep.log"
$debutPrep = Get-Date
while (!$preparation.HasExited) {
  $analyses = (Get-ChildItem $build -Recurse -File -ErrorAction SilentlyContinue | Measure-Object).Count
  $temps = [math]::Round(((Get-Date) - $debutPrep).TotalSeconds)
  Write-Progress -Activity "Preparation" -Status "$temps s - $analyses fichiers analyses"
  Start-Sleep -Milliseconds 500
}
Write-Progress -Activity "Preparation" -Completed
$total = 1
$base = Join-Path $build "compile_commands.json"
if (Test-Path $base) { $total = [math]::Max(1, (Get-Content $base -Raw | ConvertFrom-Json).Count) }
Write-Host "$total fichiers au total`n" -ForegroundColor Cyan

# 2. Compiler en suivant chaque fichier
$fait = 0
$debut = Get-Date
$erreurs = 0
& $cli compile -v --build-path $build --fqbn $fqbn $sketch 2>&1 | ForEach-Object {
  $l = "$_"
  $fichier = $null
  $cache = $false
  if ($l -match '\s-c\s.*-o\s+"?([^"\s]+\.o)"?') { $fichier = $Matches[1] }
  elseif ($l -match 'Using previously compiled file:\s*(.+\.o)') { $fichier = $Matches[1]; $cache = $true }

  if ($fichier) {
    $fait++
    $pourcent = [math]::Min(99, [math]::Floor(100 * $fait / $total))
    $nom = (Split-Path $fichier -Leaf) -replace '\.o$', ''
    $dossier = Split-Path (Split-Path $fichier -Parent) -Leaf
    $temps = [math]::Round(((Get-Date) - $debut).TotalSeconds)
    Write-Progress -Activity "Compilation de ProchainMetro" -Status "$fait / $total  ($temps s)  $dossier/$nom" -PercentComplete $pourcent
    $etat = if ($cache) { "cache  " } else { "compile" }
    Write-Host ("{0} {1} {2}/{3}" -f (Barre $pourcent), $etat, $dossier, $nom) -ForegroundColor $(if ($cache) { "DarkGray" } else { "Gray" })
  }
  elseif ($l -match 'error:|Error during|#error') { $erreurs++; Write-Host $l -ForegroundColor Red }
  elseif ($l -match 'warning:') { Write-Host $l -ForegroundColor Yellow }
  elseif ($l -match '^(Compiling|Linking|Sketch uses|Global variables|Using (library|platform|core))') { Write-Host $l -ForegroundColor Cyan }
}
$code = $LASTEXITCODE
Write-Progress -Activity "Compilation de ProchainMetro" -Completed
$temps = [math]::Round(((Get-Date) - $debut).TotalSeconds)

if ($code -ne 0) {
  Write-Host "`nECHEC de la compilation ($temps s)" -ForegroundColor Red
  exit $code
}
Write-Host ("`n{0} Compilation terminee en {1} s" -f (Barre 100), $temps) -ForegroundColor Green

# 3. Televerser
if ($Televerser) {
  Write-Host "`nTeleversement sur $Port (fermez la page ecran virtuel ou cliquez Deconnecter)..." -ForegroundColor Cyan
  & $cli upload --build-path $build --fqbn $fqbn -p $Port $sketch 2>&1 | ForEach-Object {
    $l = "$_"
    if ($l -match '(\d+)(\.\d+)?\s*%') {  # esptool : "Writing at 0x... [====>  ]  86.9% ..."
      Write-Progress -Activity "Televersement sur $Port" -PercentComplete ([int]$Matches[1])
      Write-Host ("{0} {1}" -f (Barre ([int]$Matches[1])), $l.Trim()) -ForegroundColor Gray
    }
    elseif ($l -match 'error|Failed|could not') { Write-Host $l -ForegroundColor Red }
    else { Write-Host $l -ForegroundColor DarkGray }
  }
  $code = $LASTEXITCODE
  Write-Progress -Activity "Televersement sur $Port" -Completed
  if ($code -ne 0) { Write-Host "`nECHEC du televersement" -ForegroundColor Red; exit $code }
  Write-Host "`nTeleverse. L'ESP32 redemarre." -ForegroundColor Green
}
