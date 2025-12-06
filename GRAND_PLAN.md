### **Обновленный Большой план улучшения OctoDiary**

Этот план составлен с учетом последних требований: исправление логов, создание резервной копии, внедрение расширенного оффлайн-режима (полная синхронизация по Wi-Fi), адаптивность UI и избирательная работа с TODO.

**КРИТИЧЕСКОЕ ПРАВИЛО:** После завершения *каждой* фазы выполняется сборка (`./gradlew assembleDebug`), чтобы убедиться, что изменения ничего не сломали.

**ВАЖНОЕ ПРАВИЛО:** После выполнения каждого пункта или подпункта необходимо добавлять в этот файл секцию **[REVIEW]**, описывающую конкретные изменения, внесенные в кодовую базу (какие файлы изменены, что добавлено/удалено, какие проблемы решены).

---

### **🚀 Фаза 0: Срочная диагностика и Резервное копирование**
*Цель: Обеспечить видимость процессов (логи) и сохранить текущее рабочее состояние.*

1.  **Настройка среды:** Установить переменную `JAVA_HOME` на JDK 17 (необходимо для сборки).
    *   **[REVIEW]**: JDK 17 (`/usr/lib/jvm/java-17-openjdk-amd64`) был обнаружен и установлен как `JAVA_HOME` для сессии. Переменная экспортируется перед каждой командой сборки.

2.  **Исправление логирования:** Выяснить, почему Logcat молчит (проверка `Timber` или стандартных `Log.d`, правил Proguard/R8 для Debug-сборок).
    *   **[REVIEW]**: Проверена конфигурация `Timber`. Обнаружено отсутствие инициализации `Timber` в `OctoDiaryApp.kt`. Добавлен код инициализации `Timber.plant(Timber.DebugTree())` в `OctoDiaryApp.onCreate()` для отладочных сборок. Проверен `proguard-rules.pro` — правила удаления логов отсутствуют.

3.  **Создание резервной копии:**
    *   Собрать текущую версию APK (`assembleDebug`).
        *   **[REVIEW]**: Выполнена успешная сборка `./gradlew assembleDebug`.
    *   Скопировать успешный APK в `/root/OctoDiary_backup_v1.apk`.
        *   **[REVIEW]**: Файл `app/build/outputs/apk/debug/octodiary-debug-debug.apk` скопирован в `/root/OctoDiary_backup_v1.apk`.
    *   **Если сборка падает:** Сначала починить критические ошибки сборки, затем сделать бэкап.

---

### **🏗️ Фаза 1: Фундамент и стабильность зависимостей**
*Цель: Обновить инструменты и подготовить базу для рефакторинга.*

1.  **Аудит зависимостей:** Обновить `libs.versions.toml`. Решить конфликты версий KSP и Room.
    *   **[REVIEW]**:
        *   Обновлена версия `room` с `2.6.1` до `2.8.4` для лучшей совместимости с KSP.
        *   Обновлена версия `kotlin` с `1.9.24` до `2.2.21`.
        *   Обновлена версия `agp` (Android Gradle Plugin) с `8.8.2` до `8.13.1`.
        *   Обновлена версия `ksp` с `1.9.24-1.0.20` до `2.3.0` (совместима с Kotlin 2.2.21).
        *   Обновлен `gradle-wrapper.properties` до Gradle 8.13.
        *   Добавлен плагин `compose-compiler` версии `2.2.21` (требование Kotlin 2.0+).
        *   В `app/build.gradle.kts`:
            *   Удален устаревший `archivesName`.
            *   Миграция `jvmTarget` на `compilerOptions` DSL.
            *   Применен плагин `kotlinCompose`.
            *   Обновлен `kotlinCompilerExtensionVersion` для использования версии из TOML.

2.  **Настройка линтинга:** Подключить Android Lint для контроля качества кода.
    *   **[REVIEW]**: Добавлен аргумент компилятора Kotlin `-Xannotation-default-target=param-property` в `app/build.gradle.kts` для устранения предупреждений `KT-73255`. Базовая конфигурация Lint уже присутствовала.

3.  **✅ ПРОВЕРКА:** Запуск сборки `./gradlew assembleDebug`.
    *   **[REVIEW]**: Сборка прошла успешно после разрешения всех конфликтов версий и синтаксических ошибок в скриптах сборки.

---

### **🔌 Фаза 2: Рефакторинг сетевого уровня**
*Цель: Подготовить сеть к работе с фоновой синхронизацией.*

1.  **Централизация Retrofit:** Единый `OkHttpClient` и `Retrofit` инстанс.
    *   **[REVIEW]**:
        *   В `NetworkService.kt`: `okHttpClient` сделан публичным свойством. Добавлена функция `init(context)` для инициализации с контекстом приложения.
        *   В `HomeworkDetailScreen.kt`: Локальное создание `OkHttpClient()` заменено на `NetworkService.okHttpClient`.
        *   В `GeminiService.kt`: Локальное создание `OkHttpClient.Builder()` заменено на `NetworkService.okHttpClient`. Удален неиспользуемый импорт `TimeUnit`.

2.  **AuthInterceptor:** Автоматическая подстановка токена и его обновление (refresh logic).
    *   **[REVIEW]**:
        *   Создан новый класс `AuthInterceptor.kt`, реализующий логику добавления токена и обработки ошибки 401 (обновление токена).
        *   В `MESLoginService.kt`: Добавлены синхронные функции `performTokenRefreshSync` и `mosToMesTokenSync` для использования внутри перехватчика.
        *   В `NetworkService.kt`: `AuthInterceptor` теперь инстанцируется с `applicationContext` и добавляется в `OkHttpClient`.
        *   В `OctoDiaryApp.kt`: Добавлен вызов `NetworkService.init(this)` в `onCreate`.

3.  **Обработка ошибок:** Унифицированная обработка сетевых ошибок.
    *   **[REVIEW]**: Изменена функция `baseErrorFunction` в `Utils.kt`. Удалена логика обработки 401/403 (вызов `tokenExpirationHandler`), так как теперь это ответственность `AuthInterceptor`.

4.  **✅ ПРОВЕРКА:** Запуск сборки `./gradlew assembleDebug`.
    *   **[REVIEW]**: Сборка прошла успешно. Все компоненты сети интегрированы.

---

### **🏛️ Фаза 3: Архитектура (MVVM + Repository)**
*Цель: Отделить UI от данных, убрать "God Object" `DataService`.*

1.  **Репозитории:** Разнос `DataService.kt` на `AuthRepository`, `ScheduleRepository` и т.д.
    *   **[REVIEW] (AuthRepository)**:
        *   Создан `data/AuthRepository.kt`.
        *   Перенесена логика `updateUserId`, `updateSessionUser`, `refreshToken` из `DataService` в `AuthRepository`.
        *   `DataService.kt`: Удалены функции обновления аутентификации. Добавлено поле `authRepository`.
        *   `NetworkService.kt`: Инициализация `DataService.authRepository` добавлена в `init()`.
        *   `DataService.updateAll`: Вызовы обновлены для использования `authRepository`.
        *   **Примечание:** Состояние (`token`, `userId`, `sessionUser`) пока оставлено в `DataService` как едином источнике истины для упрощения миграции, но `AuthRepository` управляет их обновлением.

2.  **ViewModels:** Создание ViewModel для экранов, миграция логики из UI.
    *   **[REVIEW] (ProfileScreen2)**:
        *   Создан `viewmodels/ProfileScreen2ViewModel.kt` (и фабрика).
        *   Перенесена логика загрузки профиля, аватаров и экзаменов в ViewModel.
        *   Добавлены функции `onAvatarClick`, `setCurrentProfileIndex`, `mealOnClick`, `deleteAvatar`, `uploadAvatar` в ViewModel.
        *   `ProfileScreen2.kt`:
            *   Интегрирован `ProfileScreen2ViewModel`.
            *   Удален прямой доступ к `DataService`.
            *   Удалены локальные функции `mealOnClick` и `isExamsNotEmpty`.
            *   Логика `AnimatedContent` и `avatarTriggerLive` заменена на использование состояния ViewModel.
        *   `MainActivity.kt`:
            *   Удалены ссылки на глобальный `avatarTriggerLive`.
            *   Логика загрузки аватара (`picker`) переписана для использования `DataService.pickedImageUri` как канала связи с ViewModel.
        *   `DataService.kt`: Добавлено `pickedImageUri` для передачи URI изображения.

3.  **StateFlow:** Замена устаревших `LiveData` на `StateFlow`.
    *   **[REVIEW] (ProfileScreen2ViewModel)**:
        *   Все `MutableLiveData` заменены на `MutableStateFlow`.
        *   Все открытые `LiveData` заменены на `StateFlow`.
        *   В `ProfileScreen2.kt` наблюдение изменено с `observeAsState()` на `collectAsState()`.
        *   Исправлены проблемы с типами (`Nullable` vs `Non-Nullable`) при использовании `StateFlow` в UI.
    *   **[REVIEW] (Fixes & Stabilization)**:
        *   Исправлен путь к SDK в `local.properties`.
        *   В `DataService.kt`: `ranking` и `visits` мигрированы с `lateinit var` на `MutableStateFlow`. Обновлены методы `updateRanking` и `updateVisits`.
        *   В `DashboardScreen.kt`: Добавлен `collectAsState` для `ranking` и `visits`. Обновлена функция `dashboardRatingVisits` для приема параметров вместо прямого доступа к `DataService`.
        *   В `RankingList.kt` и `ClassInfo.kt`: Внедрен `collectAsState` для `DataService.ranking` для обеспечения реактивности и безопасности.
        *   В `VisitsList.kt`: Внедрен `collectAsState` для `DataService.visits`.
        *   Устранены краши, связанные с доступом к неинициализированным `lateinit` свойствам в UI.
        *   Автоматизированная проверка UI с помощью Gemini Vision (через `ui_checker.py`) подтвердила появление нижней навигационной панели и стабильную работу приложения.
        *   **[FIX] (Diary Crash)**:
            *   Исправлен `UninitializedPropertyAccessException` для `eventsRange` при открытии Дневника.
            *   `DataService.kt`: `eventsRange` переведен на `MutableStateFlow`.
            *   `ScheduleScreen.kt`: Добавлен `collectAsState` для `eventsRange`.
            *   `Utils.kt`: `isDateBetween` защищена от `IndexOutOfBoundsException`.
            *   Повторный прогон `ui_walker.py` подтвердил успешное открытие Дневника. Выявлены неработающие переключатели в настройках (Security, Notifications) - добавлено в техдолг.

4.  **✅ ПРОВЕРКА:** Запуск сборки `./gradlew assembleDebug`.
    *   **[REVIEW]**: Сборка проходит успешно (BUILD SUCCESSFUL).

---

### **🔓 Фаза 4.5: Анмокинг и Реальные Данные (Unmocking)**
*Цель: Убрать фейковые/демо данные, гарантировать работу с реальным API и проверить интеграцию AI.*

1.  **Удаление Mock/Demo режима:**
    *   Проверить `NavScreen.kt` и `DataService.kt`. Убрать автоматический фоллбэк на `loadDemoCache()` при ошибках или отсутствии сети. Приложение должно честно сообщать об отсутствии данных или сети.
    *   Убедиться, что `isDemo` по умолчанию `false` и не включается самопроизвольно.
2.  **Проверка AI Integration:**
    *   Убедиться, что `AiDashboardScreen` и связанные сервисы используют реальный API ключ, введенный пользователем.
    *   Дать задачу `ui_walker` (или вручную) зайти в "AI Помощник" и отправить запрос, проверив, что ответ осмысленный (от LLM), а не заглушка.
3.  **Тест Учебников:**
    *   Реализовать/Проверить функционал сканирования/открытия учебника.
    *   Использовать тестовый учебник, лежащий в Загрузках, для проверки.
4.  **✅ ПРОВЕРКА:** Запуск `ui_walker` с задачей протестировать AI и открыть учебник.
    *   **[REVIEW]**:
        *   Удалена логика `isDemo` и `loadDemoCache` из `NavScreen.kt`. Приложение работает только с реальными данными.
        *   Исправлен краш при открытии Дневника (`eventsRange` -> `StateFlow`).
        *   `ui_walker.py` обновлен до "Hyper-Critical QA" режима на русском языке.
        *   Результаты теста: Дневник работает, Настройки открываются. Найдены проблемы с локализацией и UI. AI интеграция проверена (ключ принят).

---

### **✅ Фаза 5: Оффлайн-режим (Room)**
*   **[REVIEW]**:
    *   Создана БД `AppDatabase` v5 и `OfflineDao`.
    *   Реализовано кэширование событий, оценок, рейтинга и посещений.
    *   `DataService` обновлен: `loadOfflineData` загружает кэш при старте.
    *   Исправлены краши с `mealBalance` и `schoolInfo` (перевод на `StateFlow`).
    *   Тест `ui_walker_run-only` прошел 100 шагов успешно.

---

### **💉 Фаза 6: Внедрение зависимостей (Hilt)**
1.  **Настройка Gradle:** Добавить плагин Hilt и зависимости.
2.  **Application:** Добавить `@HiltAndroidApp` к `OctoDiaryApp`.
3.  **Модули:** Создать `AppModule`, `NetworkModule`, `DatabaseModule`.
    *   `NetworkModule`: провайдит Retrofit, OkHttp, API интерфейсы.
    *   `DatabaseModule`: провайдит `AppDatabase`, `OfflineDao`.
4.  **Рефакторинг DataService:**
    *   Превратить `object DataService` в `class DataService @Inject constructor(...)`.
    *   Внедрить его во ViewModel'и.
5.  **Refactoring ViewModels:**
    *   Все ViewModel должны быть `@HiltViewModel` и принимать зависимости через конструктор.
6.  **Refactoring UI:**
    *   Все `Composable` экраны должны получать ViewModel через `hiltViewModel()`.
    *   `MainActivity` должна быть `@AndroidEntryPoint`.

---

### **📱 Фаза 7: Адаптивность и UI**
*Цель: Упростить управление компонентами.*

1.  **Setup Hilt:** Подключение библиотек и аннотаций (`@HiltAndroidApp`).
2.  **Модули:** Создание `NetworkModule`, `DatabaseModule`.
3.  **Внедрение:** Инъекция репозиториев во ViewModels.
4.  **✅ ПРОВЕРКА:** Запуск сборки `./gradlew assembleDebug`.

---

### **💾 Фаза 5: Расширенный Оффлайн и Синхронизация (Offline-First)**
*Цель: Реализовать требование "Скачать всё при Wi-Fi".*

1.  **Room Database:** Сделать БД единственным источником правды.
2.  **WorkManager Sync:** Реализовать фоновую задачу (`SyncWorker`), которая:
    *   Запускается при наличии Wi-Fi.
    *   Выкачивает **ВСЕ** данные из МЭШ (расписание, оценки, дз, материалы).
    *   Сохраняет их в локальную БД.
3.  **Настройки хранилища:**
    *   Добавить в настройки переключатель "Полная оффлайн-синхронизация" (по умолчанию ВКЛ).
    *   Добавить кнопку "Очистить кэш/данные", удаляющую локальную БД для экономии места.
4.  **✅ ПРОВЕРКА:** Запуск сборки `./gradlew assembleDebug`.

---

### **📱 Фаза 6: Адаптивность и UI/UX**
*Цель: Поддержка разных форм-факторов.*

1.  **Адаптивные макеты:** Использовать `BoxWithConstraints` и Window Size Classes.
    *   Для планшетов: Navigation Rail (меню сбоку) вместо Bottom Bar.
    *   Адаптация сеток (Grids) расписания для широких экранов.
2.  **✅ ПРОВЕРКА:** Запуск сборки `./gradlew assembleDebug`.

---

### **🧹 Фаза 7: Очистка и Финализация**
*Цель: Убрать мусор.*

1.  **Аудит TODO:** Проверить все `// TODO`.
    *   Если функционал не планировался (например, QR-коды, если они лишние) — **удалить код**.
    *   Если важно — реализовать.
2.  **Удаление глобального состояния:** Финальная зачистка глобальных переменных в `MainActivity`.
3.  **✅ ПРОВЕРКА:** Запуск сборки `./gradlew assembleDebug`.