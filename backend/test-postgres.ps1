param(
    [int]$Port = 55488
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$project = 'pool-kr3-tests-' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$values = @{
    POOL_DB_NAME = 'pool_test'
    POOL_DB_USER = 'pool_app'
    POOL_DB_PASSWORD = [guid]::NewGuid().ToString('N')
    POOL_JWT_SECRET_BASE64 = [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
    POOL_DB_PORT = [string]$Port
    POOL_TEST_DB_ENABLED = 'true'
    SPRING_DATASOURCE_URL = "jdbc:postgresql://127.0.0.1:$Port/pool_test"
    SPRING_DATASOURCE_USERNAME = 'pool_app'
}
$values.SPRING_DATASOURCE_PASSWORD = $values.POOL_DB_PASSWORD
$previous = @{}
foreach ($name in $values.Keys) {
    $previous[$name] = [Environment]::GetEnvironmentVariable($name)
    [Environment]::SetEnvironmentVariable($name, $values[$name])
}
$compose = @('-p', $project, '-f', (Join-Path $PSScriptRoot 'docker-compose.yml'))
$result = 1
try {
    docker compose @compose up -d --wait db
    if ($LASTEXITCODE -ne 0) {
        throw 'Не удалось запустить тестовую PostgreSQL.'
    }
    mvn -B -ntp "-Dmaven.repo.local=$root/target/kr3-maven-cache" -f "$PSScriptRoot/pom.xml" clean test
    $result = $LASTEXITCODE
} finally {
    docker compose @compose down
    foreach ($name in $previous.Keys) {
        [Environment]::SetEnvironmentVariable($name, $previous[$name])
    }
}
exit $result
