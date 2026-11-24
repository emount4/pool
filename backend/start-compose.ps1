$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root '.env'
$composeFile = Join-Path $PSScriptRoot 'docker-compose.yml'

if (-not (Test-Path -LiteralPath $envFile)) {
    throw 'Создайте .env в корне проекта по образцу .env.example.'
}

docker compose --env-file $envFile -f $composeFile up --build -d --wait
exit $LASTEXITCODE
