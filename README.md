# GitHub Client (Android)

Cliente nativo de GitHub para Android hecho con **Kotlin**, **Jetpack Compose** y **Material 3**.

## Funciones

- Inicio de sesión con **Personal Access Token (PAT)**, guardado cifrado con `EncryptedSharedPreferences`.
- Listado de tus repositorios (propios, colaboraciones y de organizaciones) con búsqueda y pull-to-refresh.
- Explorador de carpetas y archivos con migas de pan (breadcrumbs).
- Visor de archivos de texto (copiar contenido y abrir en GitHub).
- Creación de repositorios (nombre, descripción, público/privado, README inicial).

## Configuración

| Elemento | Valor |
|---|---|
| compileSdk / targetSdk | 35 |
| minSdk | 26 |
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.7.3 |
| Gradle (en CI) | 8.9 |
| JDK | 17 |

## Compilar con GitHub Actions

1. Sube el contenido de esta carpeta a la raíz de la rama `main` de tu repositorio.
2. El workflow `.github/workflows/build.yml` se ejecuta solo con cada push a `main`.
3. Cuando termine, entra en la pestaña **Actions**, abre la ejecución y descarga el artefacto `github-client-debug-apk`.
4. Descomprime el artefacto e instala `app-debug.apk` en tu teléfono.

También puedes lanzarlo a mano desde **Actions > Build APK > Run workflow**.

## Compilar en local

El proyecto no incluye el Gradle Wrapper (el workflow usa Gradle 8.9 directamente). Para trabajar en local:

1. Con Gradle 8.9 instalado, ejecuta una vez `gradle wrapper --gradle-version 8.9` en la raíz.
2. Abre la carpeta con Android Studio (Ladybug o superior) y pulsa **Run**, o ejecuta `./gradlew assembleDebug`.

## Crear el token (PAT)

Crea uno en <https://github.com/settings/tokens/new?description=GitHubClient&scopes=repo>.

- Token clásico: marca el permiso `repo` (necesario para repos privados y para crear repositorios).
- Token de grano fino: da acceso a los repositorios que quieras ver y los permisos **Contents** (lectura) y **Administration** (escritura, solo si quieres crear repos).

El token solo se guarda en tu dispositivo, cifrado, y únicamente se envía a `api.github.com`.
