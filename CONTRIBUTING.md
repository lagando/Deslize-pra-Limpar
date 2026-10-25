<p align="right">
  <a href="CONTRIBUTING.pt-BR.md">Português</a> | <b>English</b>
</p>

# Contributing Guide — Deslize pra Limpar

Thank you for your interest in contributing to **Deslize pra Limpar**! This document provides guidelines for development setup, code conventions, and submitting pull requests.

---

## 🛠️ Environment Setup

1. **JDK**: Ensure you have JDK 17 or higher configured.
2. **Android Studio**: Latest stable version recommended (Ladybug / Koala or newer).
3. **Android SDK**: The project requires `compileSdk 37` and `minSdk 24`.
4. **Clone the repository**:
   ```bash
   git clone git@github.com:lagando/Deslize-pra-limpar.git
   cd Deslize-pra-limpar
   ```
5. **Open the project**: In Android Studio, open the `android/` directory.

---

## 📋 Code Conventions

- **Language**: 100% Kotlin.
- **Architecture**: MVVM (Model-View-ViewModel):
  - UI logic and animations belong in `Fragments` / `Activities`.
  - State and business logic belong in `ViewModel` (using `LiveData` / `StateFlow`).
  - Data layer and MediaStore queries must be isolated in `MediaRepository` and Room DAOs.
- **Constants**:
  - Avoid loose magic strings in code. Use `MediaStatus`, `MediaType`, and `PrefsKeys`.
- **MediaStore URIs**:
  - Always use the helper extension `buildMediaUri(mediaId, mediaType)`.
- **Strings and Localization**:
  - Never hardcode user-facing strings in code or layouts. Add entries to both `res/values/strings.xml` (English) and `res/values-pt/strings.xml` (Portuguese).
- **Security**:
  - **Never** commit signing keys (`.keystore`), passwords, or secrets to the repository.

---

## 🧪 Testing

Before submitting changes, run the unit test suite:

```bash
cd android
./gradlew test
```

New features and utility methods should include unit tests in `android/app/src/test/`.

---

## 💬 Commit Convention

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

- `feat:` A new feature
- `fix:` A bug fix
- `refactor:` Code changes that neither fix a bug nor add a feature
- `test:` Adding missing tests or correcting existing tests
- `docs:` Documentation-only changes
- `chore:` Maintenance changes (build setup, gitignore, etc.)

---

## 🚀 Submitting a Pull Request

1. Create a branch for your feature or fix:
   ```bash
   git checkout -b feature/my-improvement
   ```
2. Make atomic, well-described commits.
3. Ensure unit tests pass and the build compiles cleanly.
4. Push your branch to GitHub:
   ```bash
   git push origin feature/my-improvement
   ```
5. Open a **Pull Request** explaining what was changed, why, and how to verify it.
