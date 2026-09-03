$ErrorActionPreference = "Stop"

Write-Host "== dotnet build (apps/api) =="
dotnet build apps/api -c Release

Write-Host "== npm run build (apps/web) =="
Push-Location apps/web
npm run build
Pop-Location
