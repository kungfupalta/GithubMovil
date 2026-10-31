# GitHub Mobile

App nativa para Android (Kotlin + Jetpack Compose, SDK 35) para gestionar GitHub desde el celular:
repos, explorador de código con edición y commits, subir/descargar archivos, issues, pull requests
(crear, comentar, merge), Actions (disparar workflows, ver runs/jobs/logs, re-ejecutar, cancelar,
descargar artifacts), commits, releases, notificaciones, búsqueda, stars y forks.

## Pasos (una sola vez)
1. Crea una OAuth App: GitHub → Settings → Developer settings → OAuth Apps → New OAuth App
   (Homepage y Callback pueden ser cualquier URL, p. ej. https://github.com). Al crearla, activa
   **Enable Device Flow** y copia el **Client ID**.
2. En este repo: Settings → Secrets and variables → Actions → pestaña **Variables** → New repository
   variable: nombre `GH_CLIENT_ID`, valor = tu Client ID. (Si no lo haces, la app te pide el Client ID
   en la pantalla de login, o puedes entrar con un token personal.)
3. Descomprime el ZIP en la raíz del repo, haz commit y push a `main`.
4. Pestaña **Actions** → workflow "Build APK" → al terminar, abajo en **Artifacts** descarga
   `GitHubMobile-debug-apk`, descomprime el ZIP e instala `app-debug.apk`.

A partir de ahí, la propia app puede disparar compilaciones y bajar los artifacts del repo.
