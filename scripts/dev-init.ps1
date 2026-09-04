$ErrorActionPreference = "Stop"

Write-Host "== mvn dependency:resolve (apps/api) =="
Push-Location apps/api
mvn -q dependency:resolve
Pop-Location

Write-Host "== npm install (apps/web) =="
Push-Location apps/web
npm install
Pop-Location

Write-Host "== docker compose pull (db) =="
docker compose -f docker/docker-compose.yml pull db
