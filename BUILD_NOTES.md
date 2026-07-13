# Build Notes

This document describes the design decisions, tools, and options configured in the build system of this project.

## 1. Toolchain & Version Compatibility Matrix

To ensure stable incremental compilations, efficient configuration caching, and error-free builds, we have pinned the following compatible versions across the project:

- **Gradle Version**: `9.3.1` (optimized for performance, parallelism, and configuration caching).
- **Android Gradle Plugin (AGP)**: `9.1.1` (fully compatible with modern Android SDK tools and Gradle 9.3+).
- **Kotlin Language**: `2.2.10` (includes the modern K2 compiler and Jetpack Compose compiler plugin integrated natively).
- **Kotlin Symbol Processing (KSP)**: `2.2.10-1.0.x` or compliant version tied to Kotlin `2.2.10`.
- **JDK Target / Toolchain**: Java `11` (for both `sourceCompatibility` and `targetCompatibility`).
- **Target SDK**: `36` (fully aligned with the latest Google Play Store requirements).
- **Minimum SDK**: `24` (provides a broad coverage of Android devices while ensuring access to modern APIs).

## 2. Secure Signing Configurations

Separation between debug and release builds is strictly enforced:
- **Debug Builds**: Sign automatically with the standard local `/debug.keystore` with credentials `android`/`android`.
- **Release Builds**: Are prepared for production using environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`). This ensures that no hardcoded signing secrets are leaked in the repository, making the project highly secure and ready for Continuous Integration/Continuous Deployment (CI/CD) pipelines.

## 3. Performance Optimizations

In `gradle.properties`, several compiler optimizations are set:
- `org.gradle.parallel=true`: Compiles multiple modules in parallel.
- `org.gradle.caching=true`: Leverages local build caches to avoid recompiling unchanged files.
- `org.gradle.configuration-cache=true`: Caches configuration phases for instantaneous warm starts.
- `org.gradle.workers.max=4`: Caps workers to avoid resource exhaustion on compilation servers.
- `kotlin.compiler.execution.strategy=in-process`: Prevents Kotlin compilation daemon connection drops.

## 4. Third-Party Integrations

- **Secrets Gradle Plugin**: Configured to read API keys and private keys from client-side `.env` and `.env.example` files securely, eliminating the risk of accidental check-ins of private credentials.
- **Google Services**: Allows compile-time pass-through even if `google-services.json` is missing locally (`googleServices.missing.passthrough=true`), resolving common build-blocking obstacles on fresh dev setups.
