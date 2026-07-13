# Changelog

All notable changes to this project will be documented in this file.

## [1.1.0] - 2026-07-12

### Added
- **Device Boot Receiver**: Configured `BillingReminderReceiver` to handle the `android.intent.action.BOOT_COMPLETED` action. This ensures that when the user's device is rebooted, all recurring utility billing alerts are immediately rescheduled without requiring the user to manually open the app.
- **Manifest Intent Filter**: Added `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />` and the `<intent-filter>` for `BOOT_COMPLETED` into `AndroidManifest.xml` with `exported="true"`.
- **AutoMirrored Icon Imports**: Imported and configured `androidx.compose.material.icons.automirrored.filled.ArrowBack` and `androidx.compose.material.icons.automirrored.filled.Undo` to prevent compilation issues with modern Compose.

### Changed
- **Modernized Room Database Builder**: Replaced deprecated `fallbackToDestructiveMigration()` with the overloaded version `fallbackToDestructiveMigration(dropAllTables = true)` in `MainActivity.kt` to comply with Room 2.7.0 guidelines.
- **Compose UI Deprecations Cleanup**: 
  - Upgraded all `Divider` components to `HorizontalDivider` throughout `UtilityAppUI.kt` in compliance with Material 3 design systems.
  - Replaced deprecated `Icons.Default.ArrowBack` with `Icons.AutoMirrored.Filled.ArrowBack`.
  - Replaced deprecated `Icons.Default.Undo` with `Icons.AutoMirrored.Filled.Undo`.
- **Gradle Wrapper Restoration**: Restored the corrupt `gradle-wrapper.jar` and related property configurations, upgrading it to a fresh, clean Gradle 9.3.1 build toolchain that works perfectly out-of-the-box.
