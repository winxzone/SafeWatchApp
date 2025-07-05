# SafeWatchApp

SafeWatchApp is a mobile application designed for parents to monitor their child's device activity. It collects usage metrics such as app usage time, notifications received, and the number of screen unlocks. The collected data is securely transferred to a parent’s device and presented in a clear report.

⚠️ This project includes both client (Android app) and server components. The backend server is available here: https://github.com/winxzone/safewatchserver

---

## Features

- 📊 Usage Tracking: Monitors app usage duration and frequency.
- 🔔 Notification Logging: Records incoming notifications.
- 🔓 Unlock Counter: Tracks the number of times the device is unlocked.
- 🔐 Secure Communication: Data exchange via RESTful JSON over HTTPS.
- 📤 Report Generation: Generates and sends activity reports to the parent device.
- 🔄 Child-Parent Device Linking: Devices are linked via account/session tokens for secure access.

---

## Tech Stack

### Mobile (Android)
- Language: Kotlin
- IDE: Android Studio
- Frameworks & Libraries:
  - Jetpack Components
  - AndroidX
  - Material Design (MDC)
- Architecture: MVVM
- Data Communication: Retrofit, JSON over HTTPS
- Permission Management: Android runtime permissions for notification access, usage stats, etc.

### Backend (Server)
- Language: Kotlin
- Framework: Ktor
- Database: MongoDB (with KMongo)
- API Design: RESTful endpoints for metrics submission and retrieval
- Security: HTTPS, token-based authentication

---

## System Architecture

Child Device --> (Collects Data) --> SafeWatch Server --> MongoDB  
Parent Device --> (Fetches Reports) --> SafeWatch Server

---

## UI/UX Design

The UI follows a clean, intuitive, and responsive design guided by the Pixso prototype:  
https://pixso.net/app/editor/jBcgJwd_H3ZzYYWbnyjROw?file_type=10&icon_type=1&page-id=0%3A1

### Sample Screens

Login Screen:  

<p align="center">
  <img src="https://github.com/user-attachments/assets/3d7fe23c-23e2-4efe-abc7-6694f769fd65" width="700"/>
</p>

Child Dashboard:  
<p align="center">
  <img src="https://github.com/user-attachments/assets/54b2d201-e778-4fb2-83eb-1b6339b38632" width="700"/>
</p>

Parent Report:  
<p align="center">
  <img src="https://github.com/user-attachments/assets/945e7926-360a-4327-90cf-a8b0748fc38e" width="350"/>
</p>


Child Screens:
<p align="center">
  <img src="https://github.com/user-attachments/assets/a2da482c-b3cc-4006-b4e2-f7f2ce5a9b95" width="700"/>
</p>

---

## How to Run

### Mobile App:
1. Open the project in Android Studio
2. Connect an emulator or Android device
3. Grant necessary permissions on first launch
4. Run the app

### Server:
1. Clone the backend repo: https://github.com/winxzone/safewatchserver
2. Run using Gradle or IntelliJ with Ktor
3. Ensure MongoDB is running locally or provide a remote URI

---

## Author

Illia Nevmyvannyi
GitHub: https://github.com/winxzone
