# Fix Report: Application Crash due to `lateinit var token` & DataService Refactoring

## Status: SUCCESS (Builds, Installs & Runs without Crash)

The critical architectural refactoring of `DataService` is complete. The application now compiles successfully, installs on the device, and the `IllegalAccessException` crash during login has been resolved by removing reflection-based caching logic in `NavScreen.kt`.

## Summary of Changes

### 1. `DataService.kt` (Complete Overhaul)
- **StateFlow Migration**: All data properties (`token`, `profile`, `marks`, etc.) are now `StateFlow` with private `MutableStateFlow` backing fields. This ensures thread safety and reactivity.
- **Initialization Safety**: Removed all `lateinit var` modifiers. Nullability is handled explicitly via `StateFlow<T?>`.
- **Removed `has...` Flags**: Boolean flags (`hasProfile`, `hasToken`) replaced by checking `flow.value != null`.
- **Thread-Safe Updates**: All `update...` methods launch coroutines, check for internet, and handle token refreshing atomically using `Mutex`.
- **Safe Access**: Added helper methods like `setCurrentProfile`, `setUserId`, `setSessionUser` to safely update state.
- **Crash Fix**: Updated `loadFromCache` to correctly handle `profile` and `classMembers` deserialization.

### 2. ViewModel Architecture (MVVM)
- **New ViewModels**:
    - `ScheduleViewModel`: For Daybook/Schedule screen.
    - `HomeworksViewModel`: For Homeworks screen.
    - `MarksViewModel`: For Marks, Subject Marks, and Finals screens.
    - `ClassInfoViewModel`: For Class Info and student profile details.
- **Updated**: `ProfileScreen2ViewModel` adapted to new `DataService`.

### 3. UI Components (Reactive Updates)
- **Screens**: `DaybookScreen`, `HomeworksScreen`, `MarksScreen`, `FinalsScreen`, `ProfileChooser`, `DashboardScreen`, `Documents`, `PersonalData` updated to use `collectAsState()`.
- **NavScreen Fix**: Removed dangerous reflection code (`DataService::class.java.getDeclaredField(name).get(DataService)`) which caused `IllegalAccessException` on runtime because `StateFlow` properties are compiled to private fields. Replaced with a safe `when` expression to access flow values directly.

### 4. Verification
- **Build**: `BUILD SUCCESSFUL`.
- **Install**: Successfully installed on physical device.
- **Runtime**: Ran full automated test suite (`test_launch.ps1`). Logs confirm **NO FATAL EXCEPTIONS** and **NO IllegalAccessException**.

## Recommendations for Future
- Continue migrating remaining logic to ViewModels to fully decouple UI from DataService.
- Add Unit Tests for new ViewModels.