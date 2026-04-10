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
- Payments (PayHere)
- APIs & Integrations
- Google Maps
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
- Save and manage addresses (with optional map/address selection)
- View and edit user profile

The UI relies on a single `MainActivity` hosting multiple fragments (Home, Category, Cart, Orders, Profile, Notifications, Settings, About, etc.). Firebase Authentication and Cloud Firestore handle users and dynamic app content. Payments can be processed using the PayHere integration (client SDK or server-backed flow).

Note: This README assumes the project bundles Firebase config (`app/google-services.json`). For PayHere and Google Maps you must add API keys or follow the integration steps below.

---

## Key features

- Home feed with featured products
- Category browsing
- Cart with add/remove and quantity handling
- Checkout with payment integration (PayHere)
- Orders list and status updates
- Notifications (local channel created for order updates; can use FCM for remote notifications)
- Profile and address management with optional Google Maps address picker
- Settings and About screens
- Basic REST/API interactions and optional Firebase Cloud Functions for server-side logic

---

## Architecture

- Language: Java
- Minimum: depends on project config (check `app/build.gradle`)
- UI: Activities + Fragments, AndroidX, Material components
- Backend: Firebase Authentication and Cloud Firestore for primary data storage
- Payments: PayHere SDK or server-backed PayHere integration for processing payments
- Maps: Google Maps SDK (for address selection, geocoding and map UI)
- Serverless: Firebase Cloud Functions (optional) for secure server-side tasks, webhooks, and payment verification
- Build system: Gradle (wrapper included)

Main entry: `app/src/main/java/com/shehan/automart/activity/MainActivity.java` (hosts fragments and navigation).

---

## Prerequisites

- Android Studio (Arctic Fox or newer recommended)
- JDK 8 or newer (as required by Android Gradle plugin)
- Stable internet connection to download Gradle dependencies
- A Firebase project (recommended)
- PayHere merchant account and API credentials if you want live payments
- Google Cloud account (for Google Maps API key) if using map features

---

## Setup

1. Clone or open the project in Android Studio.

2. Ensure the `google-services.json` file is present under `app/` (the repo already contains `app/google-services.json`). If you want to use your own Firebase project, download the `google-services.json` from the Firebase console and replace the one in `app/`.

3. Add the Google Maps API key to your `AndroidManifest.xml` (or use `local.properties`/gradle to inject it). Example (in `AndroidManifest.xml` inside `<application>`):

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_GOOGLE_MAPS_API_KEY" />
```

4. Configure PayHere credentials:
- If using PayHere's Android SDK, add the SDK dependency and configure the merchant keys as described in PayHere's integration docs.
- If using a server-backed flow, store merchant credentials on the server or in Firebase Cloud Functions and never hard-code secret keys in the app. Provide the client with ephemeral payment tokens from the server.

5. Let Android Studio sync Gradle and download dependencies.

6. (Optional) If the project uses any remote configuration or environment properties, add them to `local.properties` or the appropriate config files.

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

## Payments (PayHere)

This project can integrate with PayHere to accept payments in Sri Lanka. There are two common approaches:

1. Client-side SDK (quick integration):
   - Use the official PayHere Android SDK if available. Configure your merchant ID and sandbox/live keys in the app (do not store production secrets in the app in plain text).
   - The SDK will handle collecting payment details and returning a payment result which you should verify on the server if required.

2. Server-backed flow (recommended for production):
   - Use a secure server (for example Firebase Cloud Functions) to create/verify payments and hold merchant secrets.
   - App requests a payment token or order creation endpoint on the server; server communicates with PayHere and returns the client a payment token or confirmation.
   - After payment completion, verify the payment status on the server (webhook or direct API call) and update Firestore orders accordingly.

Security notes:
- Never embed production merchant secret keys in the client app. Use server-side verification or secure token exchange.
- Test payments in PayHere's sandbox environment before going live.

---

## APIs & Integrations

- Firestore REST/API: The app uses Firebase SDKs to read/write to Cloud Firestore. If you expose custom REST endpoints (for example for search, aggregation, or payment webhooks), document them here.
- Firebase Cloud Functions: Use Cloud Functions to implement server-side logic such as:
  - Payment token generation and verification
  - Webhook handlers for PayHere (confirming transaction status)
  - Scheduled jobs (e.g., cleaning stale carts or sending summary emails)
  - Lightweight REST APIs that the mobile client can call
- Third-party APIs: Product catalog, shipping rate services, or analytics providers can be integrated via the server or directly from the app if safe to do so.

Example API flow (recommended pattern):
- Client -> Cloud Function (createOrder) -> PayHere API
- PayHere -> Cloud Function (webhook) -> update Firestore order status
- Client polls order status or receives an FCM notification when status changes

---

## Google Maps

The app can use Google Maps SDK for Android for address picking, showing delivery locations, or visualizing nearby stores. Typical uses:
- Address autocomplete and geocoding (Places API / Geocoding API)
- An address map picker fragment that returns latitude/longitude and a formatted address to your address model
- Displaying delivery pins and routes (Directions API) — note that some APIs are billable and require enabling in Google Cloud Console and restricting keys

Configuration:
- Enable Maps SDK and (optionally) Places API in Google Cloud Console and restrict the API key to your app's package name + SHA-1.
- Add the API key to `AndroidManifest.xml` as shown in Setup.

Privacy note:
- When using location services, request and document runtime permissions (ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION) and follow platform privacy guidelines.

---

## Firebase configuration notes

- Authentication: The app uses Firebase Authentication (email/password or other providers configured in Firebase console).
- Firestore: App data such as products, orders, notifications, and addresses are stored in Cloud Firestore. Ensure Firestore rules allow reads/writes for your test users or configure rules appropriately.
- Cloud Functions: If you rely on server-side payment verification or webhooks, deploy Cloud Functions and configure endpoints in your PayHere dashboard for webhook callbacks.
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
- PayHere issues: Verify sandbox vs production keys, and check Cloud Function logs for webhook handling and verification failures.
- Maps issues: Ensure the API key is enabled and restricted properly; check Google Cloud Console usage and billing.

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

For questions, reach out to the repository owner or the original author present in the project files. Shehan +94766940120


