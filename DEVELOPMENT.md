# Code Style

Please make sure, that EditorConfig Support is activated in your IDE.
See [IntelliJ Documentation](https://www.jetbrains.com/help/idea/editorconfig.html) for more information.

# Code Formatting

This project uses Spotless for code formatting. Install the pre-push Git hook to automatically check formatting before pushing. 
It will block the push if you forget to run `./gradlew spotlessApply`.
