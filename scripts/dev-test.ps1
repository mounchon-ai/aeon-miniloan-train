$ErrorActionPreference = "Stop"

Write-Host "== dotnet test (apps/api) =="
dotnet test apps/api

Write-Host "== npm test (apps/web) =="
Push-Location apps/web
npm test --if-present
Pop-Location
