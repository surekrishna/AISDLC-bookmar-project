$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'C:\Users\surebabu\.jdks\temurin-21.0.10'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Set-Location 'C:\Krishna\Intellij-Workspace\NagarroAssignments\sure-babu\Assignment 1'
$logPath = '.\verify-integration.log'
if (Test-Path $logPath) { Remove-Item $logPath -Force }
& .\gradlew.bat test --tests com.krish.bookmarks.integration.BookmarkPersistenceIntegrationTest 2>&1 | Tee-Object -FilePath $logPath
$exitCode = $LASTEXITCODE
Add-Content -Path $logPath -Value "EXITCODE=$exitCode"
exit $exitCode

