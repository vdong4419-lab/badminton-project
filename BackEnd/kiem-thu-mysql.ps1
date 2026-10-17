param(
    [int]$Port = 3308,
    [string]$MavenCommand = "$PSScriptRoot/mvnw.cmd",
    [string[]]$MavenArguments = @()
)

$ErrorActionPreference = 'Stop'
$testName = 'badminton-test-' + [Guid]::NewGuid().ToString('N')
$testPassword = [Guid]::NewGuid().ToString('N')
$tempSql = [System.IO.Path]::GetTempFileName()
$created = $false
$previous = @{}
foreach ($key in @('RUN_MYSQL_TESTS', 'TEST_DB_URL', 'TEST_DB_USERNAME', 'TEST_DB_PASSWORD')) {
    $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
}

try {
    $binding = "127.0.0.1:${Port}:3306"
    docker run --detach --rm --name $testName --publish $binding --tmpfs /var/lib/mysql --env "MYSQL_ROOT_PASSWORD=$testPassword" --env MYSQL_DATABASE=dat_san_kiem_thu mysql:8.0 --default-time-zone=+07:00 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Cannot create the isolated MySQL test container.' }
    $created = $true
    $ready = $false
    for ($attempt = 0; $attempt -lt 90; $attempt++) {
        docker exec $testName mysqladmin ping -h 127.0.0.1 --silent 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    if (!$ready) { throw 'MySQL did not become ready.' }

    $schema = [System.IO.File]::ReadAllText("$PSScriptRoot/database/001-init.sql")
    [System.IO.File]::WriteAllText($tempSql, $schema.Replace('dat_san_cau_long', 'dat_san_kiem_thu'), [System.Text.UTF8Encoding]::new($false))
    docker cp $tempSql "${testName}:/tmp/init.sql"
    if ($LASTEXITCODE -ne 0) { throw 'Cannot copy the test schema.' }
    docker exec --env "MYSQL_PWD=$testPassword" $testName sh -c 'exec mysql --default-character-set=utf8mb4 -uroot < /tmp/init.sql'
    if ($LASTEXITCODE -ne 0) { throw 'Cannot initialize the test schema.' }

    $env:RUN_MYSQL_TESTS = 'true'
    $env:TEST_DB_URL = "jdbc:mysql://127.0.0.1:${Port}/dat_san_kiem_thu?connectionTimeZone=Asia/Ho_Chi_Minh"
    $env:TEST_DB_USERNAME = 'root'
    $env:TEST_DB_PASSWORD = $testPassword
    Push-Location $PSScriptRoot
    try {
        & $MavenCommand @MavenArguments test
        if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed. See target/surefire-reports.' }
    } finally {
        Pop-Location
    }
} finally {
    foreach ($key in $previous.Keys) {
        [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process')
    }
    # Only remove the container this script successfully created, with a unique generated name.
    if ($created) { docker rm --force --volumes $testName | Out-Null }
    Remove-Item -LiteralPath $tempSql -ErrorAction SilentlyContinue
}
