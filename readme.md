# OctoDiary

OctoDiary — это многокомпонентная система, разработанная для удобного доступа к информации из МЭШ и Моей школы МО. Проект включает в себя основное Android-приложение, встроенный контент-сервер на Node.js/TypeScript и React-компонент для NFC-анимации.

&nbsp;

<div align=center style="padding: 30px">

<img src=https://github.com/OctoDiary/OctoDiary-kt/assets/66333241/fdb72a5d-9f7a-4fb9-bb9d-a26c3a735169 width=150>
<img src=https://github.com/OctoDiary/OctoDiary-kt/assets/66333241/6e4b7741-058e-4926-97d9-cb7022794744 width=150>
<img src=https://github.com/OctoDiary/OctoDiary-kt/assets/66333241/6904bac2-c2a5-43ce-97ef-3eeed403eba1 width=150>
<img src=https://github.com/OctoDiary/OctoDiary-kt/assets/66333241/0929a1e5-814a-4013-bb72-c8cd97b4d474 width=150>

</div>
&nbsp;

## Общая архитектура

Проект представляет собой многокомпонентную систему: основное Android-приложение, встроенный контент-сервер (Node.js/TypeScript) и React-компонент для NFC-анимации.
*   **Сборка Android-части:** Gradle.
*   **Сборка серверной части и NFC-компонента:** NPM.

## Модули проекта

### Android-приложение (`app`)
*   **Назначение:** Клиент для МЭШ и Моей школы МО, предоставляющий доступ к дневнику, оценкам, домашним заданиям. Приложение взаимодействует с API, отображает контент, поддерживает виджеты и позволяет работать с оффлайн-данными.
*   **Технологии:** Kotlin, Jetpack Compose (Material3, Navigation), Room (локальная БД), Retrofit2 (сеть), OkHttp, WorkManager, Coroutines, ONNX Runtime (локальные AI-модели), ML Kit (распознавание текста), Glide (изображения), Markwon (Markdown), Vico charts, Telephoto Zoomable, DotsIndicator, iText7 (PDF), ZXing (QR-коды), биометрия.
*   **Процесс запуска:** Приложение инициализируется через [`OctoDiaryApp`](app/src/main/java/org/bxkr/octodiary/OctoDiaryApp.kt), который запускает `DataService`. Основной пользовательский интерфейс и фоновые службы управляются [`MainActivity`](app/src/main/java/org/bxkr/octodiary/MainActivity.kt), которая также отвечает за навигацию.
*   **Навигация:** Определена в [`Screens.kt`](app/src/main/java/org/bxkr/octodiary/Screens.kt), где `Screen` используется для уникальных экранов, а `NavSection` — для элементов нижней навигации.
*   **Данные:** [`DataService`](app/src/main/java/org/bxkr/octodiary/DataService.kt) является синглтоном, отвечающим за загрузку, кэширование данных, управление токенами и реализацию бизнес-логики. Локальное хранилище данных осуществляется через обертки над `SharedPreferences`.
*   **Демо-режим:** Доступен для быстрого ознакомления без авторизации. Включается через Debug-меню.

### Контент-сервер (`content-server`)
*   **Назначение:** **MCP-сервер для ИИ в чате в приложении.** Также может использоваться для предоставления локального API или для отображения сложного веб-контента внутри `WebView` в приложении.
*   **Технологии:** Node.js, TypeScript, NPM.

### NFC Анимация (`nfc anim`)
*   **Назначение:** Визуализация процесса NFC-взаимодействия, встраиваемая в Android-приложение через WebView.
*   **Технологии:** React, TypeScript, shadcn/ui, CSS.

## Сборка

*   **Команда:** Для сборки отладочной версии Android-приложения используйте:
    ```bash
    ./gradlew assembleDebug
    ```
*   **Требования:**
    *   JDK 17+
    *   Android SDK
    *   Git должен быть установлен и добавлен в PATH (используется для именования артефактов).

## Точки входа в код

*   [`MainActivity.kt`](app/src/main/java/org/bxkr/octodiary/MainActivity.kt) — основная активность, навигация, темы, диалоги.
*   [`screens/navsections/daybook/`](app/src/main/java/org/bxkr/octodiary/screens/navsections/daybook/) — содержит логику расписания (`ScheduleScreen`, `DayItem`, `EventItem`) и карточки уроков.
*   [`screens/navsections/homeworks/`](app/src/main/java/org/bxkr/octodiary/screens/navsections/homeworks/) — список домашних заданий и экран `HomeworkDetailScreen`.
*   [`DataService.kt`](app/src/main/java/org/bxkr/octodiary/DataService.kt) — загрузка данных, кэширование и бизнес-операции.
*   [`NetworkService.kt`](app/src/main/java/org/bxkr/octodiary/network/NetworkService.kt) — конструкторы API Retrofit.

## Лицензии и торговые марки

## Копилефт

Google Play и логотип Google Play являются товарными знаками корпорации Google LLC.

## Текущий статус (Dev Status) - В РАЗРАБОТКЕ

**Ветка:** `v3-develop`
**Фаза:** Фаза 3 - Архитектура (MVVM + Repository) и StateFlow миграция.

**Статус сборки:** 🟢 **СБОРКА УСПЕШНА** (Build Successful)

Сборка завершена успешно. Ошибки миграции на StateFlow и конфигурации устранены.

**Что сделано:**
1.  Полная миграция с `LiveData` на `StateFlow` в `MainActivity`, `DataService` и всех экранах.
2.  Исправление ошибок компиляции (Kotlin syntax, Type mismatch, Composable invocations).
3.  Обновление конфигурации Gradle (JDK path, local.properties).
4.  Разрешение конфликтов слияния.

**Что нужно сделать (Next Steps):**
1.  Протестировать работу приложения на устройстве/эмуляторе (проверить навигацию, смену темы, загрузку данных).
2.  Продолжить рефакторинг архитектуры (MVVM).
3.  Вернуть удаленный код `launchPicker` и `snapshotFlow` в `MainActivity.kt`.