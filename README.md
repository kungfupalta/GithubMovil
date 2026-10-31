# GitHub Móvil - Cliente Android Nativo

Proyecto completo en Kotlin + Jetpack Compose + Material 3 preparado para compilar automáticamente con GitHub Actions.

## Instrucciones para Compilar el APK

1. Descomprimí este ZIP en una carpeta de tu PC.
2. Creá un repositorio vacío en tu cuenta de GitHub (ej. `GithubMovil`).
3. Subí todos los archivos a tu repositorio usando Git:
   ```bash
   git init
   git add .
   git commit -m "Inicializar proyecto"
   git branch -M main
   git remote add origin https://github.com/TU_USUARIO/TU_REPOSITORIO.git
   git push -u origin main
   ```
4. En GitHub, andá a la pestaña **Actions**. El workflow **Compilar APK Debug** se ejecutará automáticamente.
5. Al finalizar (círculo verde), hacé clic en la ejecución y descargá el archivo APK desde la sección **Artifacts**.
