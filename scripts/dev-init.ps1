$ErrorActionPreference = "Stop"

Write-Host "== dotnet restore (apps/api) =="
dotnet restore apps/api

Write-Host "== generate theme tokens (apps/web) =="
node scripts/sync-theme-tokens.mjs

Write-Host "== npm install (apps/web) =="
Push-Location apps/web
npm install
Pop-Location

Write-Host "== docker compose pull (db) =="
docker compose -f docker/docker-compose.yml pull db
