$ErrorActionPreference = "Stop"

Write-Host "== mvn package (apps/api) =="
Push-Location apps/api
mvn -q package -DskipTests
Pop-Location

Write-Host "== npm run build (apps/web) =="
Push-Location apps/web
npm run build
Pop-Location
