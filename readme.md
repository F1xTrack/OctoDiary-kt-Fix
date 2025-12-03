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
    **Статус сборки:** 🔴 **СБОРКА СЛОМАНА** (Build Broken)
        * Последняя попытка сборки завершилась ошибками компиляции после миграции LiveData на StateFlow.
    **Последние известные ошибки:**
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:123:42 Unresolved reference 'NavHostController'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:123:62 Null cannot be a value of a non-null type 'uninferred T (of fun <T>
  MutableStateFlow)'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:291:21 Argument type mismatch: actual type is 'Boolean?', but 'Boolean' was
  expected.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:332:27 Property delegate must have a 'getValue(Nothing?, KProperty0<ERROR
  CLASS: Cannot infer argument for type parameter T>)' method. None of the following functions is applicable:
  fun <T> State<T>.getValue(thisObj: Any?, property: KProperty<*>): T
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:332:30 Cannot infer type for type parameter 'T'. Specify it explicitly.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:332:48 Cannot infer type for type parameter 'T'. Specify it explicitly.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:336:48 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:367:23 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:369:65 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:391:53 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:392:55 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:393:55 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:394:51 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:395:52 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:396:53 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:422:48 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:422:110 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:439:63 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:441:47 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:463:62 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:472:59 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:473:49 @Composable invocations can only happen from the context of a
  @Composable function
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:476:33 @Composable invocations can only happen from the context of a
  @Composable function
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:480:41 Cannot infer type for type parameter 'S'. Specify it explicitly.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:481:64 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:483:43 Cannot infer type for type parameter 'S'. Specify it explicitly.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:491:64 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:491:71 Cannot infer type for type parameter 'T'. Specify it explicitly.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:496:50 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:510:62 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:550:36 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:553:60 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:558:49 Unresolved reference 'hierarchy'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:558:66 Cannot infer type for value parameter 'destination'. Specify it
  explicitly.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:558:93 Unresolved reference 'route'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:569:47 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:569:55 Unresolved reference 'navigate'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:570:37 Unresolved reference 'popUpTo'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:570:59 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:570:67 Unresolved reference 'graph'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:571:41 Unresolved reference 'saveState'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:573:37 Unresolved reference 'launchSingleTop'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:574:37 Unresolved reference 'restoreState'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:593:49 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:621:67 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:629:67 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:643:32 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:654:43 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/MainActivity.kt:654:50 Cannot infer type for type parameter 'T'. Specify it explicitly.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/Mark.kt:318:53 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/Mark.kt:319:29 @Composable invocations can only happen from the context of a
  @Composable function
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/Mark.kt:340:60 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/Mark.kt:349:55 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/Mark.kt:351:43 Unresolved reference 'navigate'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/Mark.kt:379:31 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/Mark.kt:380:33 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/Mark.kt:380:45 @Composable invocations can only happen from the context of a
  @Composable function
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/ProfileChooser.kt:48:46 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/SettingsDialog.kt:313:51 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/TokenLogin.kt:99:54 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/TokenLogin.kt:120:16 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/ai/AiQuickAccessCard.kt:63:45 Unresolved reference 'navigate'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/ai/AiQuickAccessCard.kt:82:38 Unresolved reference 'navigate'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/ai/AiQuickAccessCard.kt:95:38 Unresolved reference 'navigate'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/ai/AiQuickAccessCard.kt:108:38 Unresolved reference 'navigate'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/settings/Appearance.kt:32:35 None of the following candidates is applicable:
  fun <T> LiveData<T>.observeAsState(): State<T?>
  fun <R, T : R> LiveData<T>.observeAsState(initial: R): State<R>
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/settings/Appearance.kt:50:41 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/settings/Appearance.kt:60:33 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/settings/Appearance.kt:68:21 Unresolved reference 'primary'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/settings/Appearance.kt:69:21 Unresolved reference 'secondary'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/settings/Appearance.kt:70:21 Unresolved reference 'surfaceVariant'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/settings/Appearance.kt:72:37 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/settings/Appearance.kt:92:33 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/settings/Appearance.kt:98:33 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/components/settings/Appearance.kt:114:19 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/network/MySchoolLoginService.kt:76:24 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/network/MySchoolLoginService.kt:101:28 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/AiDashboardScreen.kt:49:49 Unresolved reference 'navigateUp'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/AiDashboardScreen.kt:263:42 Unresolved reference 'navigate'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/AiDashboardScreen.kt:273:42 Unresolved reference 'navigate'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/CallbackScreen.kt:47:32 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/LectureNotesScreen.kt:34:49 Unresolved reference 'navigateUp'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/NavScreen.kt:207:55 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/NavScreen.kt:245:70 Unresolved reference 'value'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/TextbookExtractorScreen.kt:51:49 Unresolved reference 'navigateUp'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/VocabularySmartScreen.kt:127:49 Unresolved reference 'navigateUp'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/dashboard/DashboardScreen.kt:142:59 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/dashboard/DashboardScreen.kt:181:63 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/daybook/DayChooser.kt:19:64 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/daybook/DayChooser.kt:24:38 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/daybook/EventItem.kt:211:74 Unresolved reference 'navigate'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/homeworks/HomeworkDetailScreen.kt:80:49 Unresolved reference
  'navigateUp'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/homeworks/HomeworkSubject.kt:41:59 Unresolved reference 'navigate'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/homeworks/HomeworksScreen.kt:53:40 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/homeworks/HomeworksScreen.kt:54:36 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/homeworks/HomeworksScreen.kt:56:32 @Composable invocations can only
  happen from the context of a @Composable function
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/marks/MarksByDate.kt:30:32 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/marks/MarksByDate.kt:30:44 @Composable invocations can only happen from
  the context of a @Composable function
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/marks/MarksBySubject.kt:49:32 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/marks/MarksBySubject.kt:49:44 @Composable invocations can only happen
  from the context of a @Composable function
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/marks/MarksScreen.kt:33:20 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/marks/MarksScreen.kt:34:36 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/marks/SubjectCard.kt:102:43 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/marks/SubjectCard.kt:209:47 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/marks/SubjectCard.kt:237:55 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/marks/SubjectCard.kt:238:57 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/marks/SubjectCard.kt:239:33 @Composable invocations can only happen
  from the context of a @Composable function
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/profile/ProfileScreen2.kt:63:27 Conflicting import: imported name 'R'
  is ambiguous.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/profile/ProfileScreen2.kt:64:27 Conflicting import: imported name 'R'
  is ambiguous.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/profile/ProfileScreen2.kt:103:37 Smart cast to 'ProfileResponse' is
  impossible, because 'profileResponse' is a delegated property.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/profile/ProfileScreen2.kt:122:50 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/profile/ProfileScreen2.kt:126:56 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/profile/ProfileScreen2.kt:184:38 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/profile/ProfileScreen2.kt:327:31 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/profile/ProfileScreen2.kt:328:33 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/screens/navsections/profile/meal/Meal.kt:44:44 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/viewmodels/ProfileScreen2ViewModel.kt:114:30 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/viewmodels/ProfileScreen2ViewModel.kt:128:34 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/viewmodels/ProfileScreen2ViewModel.kt:130:27 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/viewmodels/ProfileScreen2ViewModel.kt:142:38 Unresolved reference 'postValue'.
  e: file:///D:/OctoDiary-kt-Fix-2-develop/app/src/main/java/org/bxkr/octodiary/viewmodels/ProfileScreen2ViewModel.kt:177:42 Unresolved reference 'postValue'.
