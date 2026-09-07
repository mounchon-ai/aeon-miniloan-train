/**
 * Where apps/api lives, as seen from the browser.
 *
 * `docker/docker-compose.yml` passes an `API_URL` build arg into `apps/web/Dockerfile` and the
 * Dockerfile declares it, but nothing consumes it: there is no `src/environments` and no
 * `fileReplacements` in `angular.json`. Wiring that is build tooling at the app's own root, outside
 * every layer `/dev:stack` declared (CLAUDE.md, "Mistakes already made"), so FE-miniloan-021 put the
 * value here in `core` — inside a declared layer, in one place — rather than scattering the literal
 * through the services or quietly rewriting the build.
 *
 * The seam is deliberate: whoever wires `API_URL` for real only has to change this file. Note that
 * an Angular bundle bakes this in at BUILD time, so setting an environment variable on a running
 * `web` container does nothing (CLAUDE.md, "Angular + Docker Compose gotcha").
 */
export const API_BASE_URL = 'http://localhost:5000';
