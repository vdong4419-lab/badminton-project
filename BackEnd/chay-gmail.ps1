param()
$ErrorActionPreference = 'Stop'
$previousMailEnv = @{}
foreach ($mailKey in @('MAIL_ENABLED','MAIL_USERNAME','MAIL_PASSWORD')) {
    $previousMailEnv[$mailKey] = [Environment]::GetEnvironmentVariable($mailKey, 'Process')
}
$smtpSecret = $null
$backendExitCode = 1
try {
    $smtpSecret = Read-Host 'Nhap mat khau ung dung Gmail cua vdong4419@gmail.com (an khi go)' -AsSecureString
    $env:MAIL_PASSWORD = [System.Net.NetworkCredential]::new('', $smtpSecret).Password.Replace(' ', '')
    if ([string]::IsNullOrWhiteSpace($env:MAIL_PASSWORD)) { throw 'Can nhap mat khau ung dung Gmail.' }
    $env:MAIL_USERNAME = 'vdong4419@gmail.com'
    $env:MAIL_ENABLED = 'true'
    Push-Location $PSScriptRoot
    try {
        & "$PSScriptRoot/mvnw.cmd" spring-boot:run
        $backendExitCode = $LASTEXITCODE
    } finally { Pop-Location }
} finally {
    foreach ($mailKey in $previousMailEnv.Keys) {
        [Environment]::SetEnvironmentVariable($mailKey, $previousMailEnv[$mailKey], 'Process')
    }
    if ($smtpSecret) { $smtpSecret.Dispose() }
}
exit $backendExitCode
