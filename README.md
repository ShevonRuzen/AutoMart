# AutoMart Android App

AutoMart is a native Android shopping application (Java) that demonstrates a typical e-commerce flow: browsing categories and products, managing a cart, placing orders, viewing order status and notifications, and managing user profiles and addresses. The app uses Firebase for authentication and Firestore for backend data.

---

## Table of Contents

- Project overview
- Key features
- Architecture
- Prerequisites
- Setup
- Build & Run
- Firebase configuration
- Project structure
- Troubleshooting
- Contributing
- License

---

## Project overview

AutoMart is an Android application built with Java and AndroidX libraries. It provides a mobile storefront experience where users can:

- Browse products by category
- Add items to cart
- Place and track orders
- Receive notifications about order status
- Save and manage addresses
- View and edit user profile

The UI relies on a single `MainActivity` hosting multiple fragments (Home, Category, Cart, Orders, Profile, Notifications, Settings, About, etc.). Firebase Authentication and Cloud Firestore handle users and dynamic app content.

---

## Key features

- Home feed with featured products
- Category browsing
- Cart with add/remove and quantity handling
- Orders list and status updates
- Notifications (local channel created for order updates)
- Profile and address management
- Settings and About screens

---

## Architecture

- Language: Java
- Minimum: depends on project config (check `app/build.gradle`)
- UI: Activities + Fragments, AndroidX, Material components
- Backend: Firebase Authentication, Cloud Firestore (and optionally Firebase Cloud Messaging)
- Build system: Gradle (wrapper included)

Main entry: `app/src/main/java/com/shehan/automart/activity/MainActivity.java` (hosts fragments and navigation).

---

## Prerequisites

- Android Studio (Arctic Fox or newer recommended)
- JDK 8 or newer (as required by Android Gradle plugin)
- Stable internet connection to download Gradle dependencies
- A Firebase project (optional for a fully-featured experience)

---

## Setup

1. Clone or open the project in Android Studio.

2. Ensure the `google-services.json` file is present under `app/` (the repo already contains `app/google-services.json`). If you want to use your own Firebase project, download the `google-services.json` from the Firebase console and replace the one in `app/`.

3. Let Android Studio sync Gradle and download dependencies.

4. (Optional) If the project uses any remote configuration or environment properties, add them to `local.properties` or the appropriate config files.

---

## Build & Run

From Android Studio:

1. Select a device or emulator.
2. Build and Run the app (Run > Run 'app').

From the command line (PowerShell on Windows):

```powershell
# From project root (where gradlew.bat lives)
.\gradlew.bat assembleDebug
# or to install on a connected device (requires device/emulator running)
.\gradlew.bat installDebug
```

---

## Firebase configuration notes

- Authentication: The app uses Firebase Authentication (email/password or other providers configured in Firebase console).
- Firestore: App data such as products, orders, notifications, and addresses are stored in Cloud Firestore. Ensure Firestore rules allow reads/writes for your test users or configure rules appropriately.
- google-services.json: This file is required and is included in the `app/` folder. If you change Firebase projects, replace this file and re-sync Gradle.

---

## Project structure (high level)

- app/
  - src/main/java/com/shehan/automart/activity/ - Activities (MainActivity, LoginActivity, etc.)
  - src/main/java/com/shehan/automart/fragment/ - UI fragments (Home, Cart, Profile, etc.)
  - src/main/java/com/shehan/automart/model/ - Data models used with Firestore
  - res/ - Layouts, drawables, values
  - google-services.json - Firebase Android config

---

## Troubleshooting

- Gradle sync fails: Invalidate caches and restart Android Studio, check your network, and verify the Gradle plugin version in `build.gradle`.
- Firebase errors (auth/firestore): Make sure `google-services.json` matches your Firebase project and Firestore rules permit access for your testing users.
- Missing dependencies: Run Gradle sync and check the `build.gradle` files for repository sources (mavenCentral/google/jcenter).

---

## Contributing

Contributions are welcome. Open an issue first to discuss major changes. For small fixes, send a pull request with a clear description of changes and a short test plan.

When contributing:
- Keep Java coding style consistent with existing files
- Add unit tests where practical
- Update README if you add or change major functionality

---

## License

Specify project license here (e.g., MIT). If you don't want to include a license, state that the project is proprietary.

---

## Contact

For questions, reach out to the repository owner or the original author present in the project files.


