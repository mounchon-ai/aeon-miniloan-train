$ErrorActionPreference = "Stop"

Write-Host "== mvn test (apps/api) =="
Push-Location apps/api
mvn -q test
Pop-Location

Write-Host "== npm test (apps/web) =="
Push-Location apps/web
npm test -- --watch=false
Pop-Location
