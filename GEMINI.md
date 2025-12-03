# OctoDiary-kt-Fix Context

## Project Overview
OctoDiary is a comprehensive system designed to interface with "МЭШ" (Moscow Electronic School) and "Моя школа МО" (My School Moscow Region). It provides a unified experience for accessing school diaries, grades, homework, and educational content.

The project is a mono-repository containing three main components:
1.  **Android Application (`app/`):** The primary user interface, a native Android app built with Kotlin and Jetpack Compose.
2.  **Content Server (`content-server/`):** A Node.js/TypeScript server implementation of the Model Context Protocol (MCP). It serves educational content (textbooks, paragraphs) to AI agents or the app itself.
3.  **NFC Animation (`nfc anim/`):** A React/TypeScript web component for visualizing NFC payment/interaction, embedded within the Android app.

## Architecture & Technologies

### Android Application (`app`)
*   **Language:** Kotlin (JVM Target 17)
*   **UI Framework:** Jetpack Compose (Material3)
*   **Navigation:** Jetpack Navigation Compose
*   **Data Persistence:** Room Database (with KSP), SharedPreferences
*   **Networking:** Retrofit2, OkHttp, Gson
*   **Concurrency:** Kotlin Coroutines, WorkManager
*   **AI & ML:**
    *   **ONNX Runtime:** For local execution of AI models.
    *   **ML Kit:** For text recognition (OCR).
*   **Widgets:** Jetpack Glance
*   **Charts:** Vico
*   **Other:** Biometric Auth, ZXing (QR Codes), Markwon (Markdown rendering).

### Content Server (`content-server`)
*   **Language:** TypeScript
*   **Runtime:** Node.js
*   **Protocol:** Model Context Protocol (MCP) for AI interaction.
*   **Purpose:** Provides tools like `get_table_of_contents`, `request_paragraph`, and `search_content`.

### NFC Animation (`nfc anim`)
*   **Language:** TypeScript, React
*   **Styling:** CSS, shadcn/ui
*   **Purpose:** Visual feedback component for NFC operations.

## Build & Run Instructions

### Android App
The project uses Gradle with Kotlin DSL (`.gradle.kts`).
*   **Build Debug APK:**
    ```bash
    ./gradlew assembleDebug
    ```
*   **Run Tests:**
    ```bash
    ./gradlew test
    ```
*   **Key Configuration:**
    *   `compileSdk`: 34
    *   `minSdk`: 26
    *   `targetSdk`: 35

### Content Server
*   **Install Dependencies:**
    ```bash
    cd content-server
    npm install
    ```
*   **Build:**
    ```bash
    npm run build:win
    # or check package.json for other build scripts
    ```

## Key Directories & Files

*   **`app/src/main/java/org/bxkr/octodiary/`**: Root package for Android source code.
    *   **`MainActivity.kt`**: Application entry point, handles high-level navigation and UI setup.
    *   **`DataService.kt`**: Central singleton for data management, caching, and business logic.
    *   **`OctoDiaryApp.kt`**: Application class, initializes services.
    *   **`screens/`**: Contains Composable UI screens (e.g., `ScheduleScreen`, `HomeworkDetailScreen`).
    *   **`network/`**: Network related classes (Retrofit services).
*   **`app/src/main/res/`**: Android resources (layouts, strings, drawables).
*   **`content-server/src/index.ts`**: Entry point for the MCP server.
*   **`gradle/libs.versions.toml`**: Version catalog for dependency management.

## Development Conventions

*   **Code Style:** Follows standard Kotlin and Android best practices.
*   **UI Pattern:** Compose-first. Screens are defined as Composables.
*   **Dependency Injection:** Currently appears to rely on manual injection or Singletons (e.g., `DataService`), rather than Hilt/Dagger (based on initial scan).
*   **Asynchronicity:** Heavy use of Coroutines for background tasks (network, DB).

## AI Context & MCP
The `content-server` is specifically designed to be an MCP server. This means it exposes "tools" that an AI (like Gemini) can call to retrieve structured data. When working on this project, consider how changes might affect the AI's ability to query school data.

## ПРАВИЛА РАБОТЫ
*   **Язык:** Говори на русском языке
*   **Терминал:** Все команды кроме базовыз системных по управлению хранилищем(ls, rm и тд) и команд gradle и wget ты должен выполнять по возможности в отдельной сессии терминала, не засоряя эту, чтобы в итоге не получалось так, что команда требует ввода в TUI, а ты жёшь,, пока команда завершит работу с exit code
    *    **Схема запуска сторонних команд:** Команда на запуск отдельной сессии терминала, в которой выполнится команда и сессия завершится => В твём окне терминала команда на тайм-аут на опредеённое количество времени, после которого весь вывод собеётся в отдельный файл, ты его прочитаешь и сделаешь вывод, хватило этого тайм-аута или нет + проанализируешь вывод как тебе надо(ошибки и тп, смотря что тебе необходимо)
