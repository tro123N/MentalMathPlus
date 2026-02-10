# <a href="https://codeberg.org/Mental-Math/MentalMath"><img src="./fastlane/metadata/android/en-US/images/icon_transparent.svg" alt="Mental Math Icon" height="25"></a> Mental Math
<p align="center">
  <img src="fastlane/metadata/android/en-US/images/featureGraphic.png" alt="Main Screen" width="100%">
</p>

## App Overview
"Mental Math" is a simple and clean Android app for mental arithmetic training. You can practice on your own or challenge friends to compete and improve your skills. The app is [open source](https://codeberg.org/Mental-Math/MentalMath) and respects your privacy.

<p align="center">
  <a href="https://f-droid.org/packages/com.helddertierwelt.mentalmath/">
    <img src="fastlane/metadata/android/en-US/images/badges/fdroid-badge.png" alt="Get it on F-Droid" height="80">
  </a>
  <a href="https://play.google.com/store/apps/details?id=com.helddertierwelt.mentalmath">
    <img src="fastlane/metadata/android/en-US/images/badges/google-play-badge.png" alt="Get it on Google Play" height="80">
  </a>
  <a href="https://codeberg.org/Mental-Math/MentalMath/releases">
    <img src="fastlane/metadata/android/en-US/images/badges/codeberg-badge.png" alt="Get it on Codeberg" height="80">
  </a>
</p>

## Features
#### Customizable Training
Select which arithmetic operations to include (+, −, ×, ÷). For each operation, choose from nine difficulty levels to customize your training precisely to your skill level.

#### Two Game Modes
• Task Mode lets you complete a fixed number of tasks (10, 20, 30, 40, or 50).  
• Timed Mode challenges you to solve as many tasks as possible within a time limit (1, 2, 3, 4, or 5 minutes).

#### Challenge your Friends
You can share a game link to:  
*1.* Replay a completed game with the exact same tasks to compare scores with friends.  
*2.* Start a new game with preset settings for a fair and fresh challenge for everyone.

#### Difficulty Calculation
Task difficulty is determined using [algorithms](https://codeberg.org/Mental-Math/MentalMathEvaluator) based on column addition, column subtraction, long multiplication, and long division, creating realistic and well-balanced exercises.

#### Detailed Results
After each game, view a detailed summary that includes your selected settings, your performance (accuracy, speed, and score), and a complete list of all your tasks and answers for review.

#### Personal Leaderboard
Your top 10 results are displayed separately for each combination of settings, allowing you to easily track your progress. You can also review detailed summaries of your top games at any time.

#### Game History
Explore the complete history of all your scored games. You can sort the list by time or numerically to find specific games. Anytime, you can revisit any match to view a detailed summary of its results.

#### Practice Reminders
Set up periodic reminders to support regular mental math practice. Notifications are sent in the evening (around 6 p.m.) and can be scheduled daily, every three days, or weekly.

#### Clean Design
A simple and minimalist app design with no unnecessary elements or distractions, designed to keep your focus fully on mental math training. Supports both light and dark modes.

#### Open Source and Privacy
"Mental Math" is fully [open source](https://codeberg.org/Mental-Math/MentalMath), free to use, and does not collect or process any personal data. It requires no permissions, contains no ads, and runs entirely offline to respect your privacy.

## Screenshots
<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/01.png" alt="Main Screen" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/02.png" alt="Main Screen" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/03.png" alt="Main Screen" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/04.png" alt="Main Screen" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/05.png" alt="Main Screen" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/06.png" alt="Main Screen" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/07.png" alt="Main Screen" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/08.png" alt="Main Screen" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/09.png" alt="Main Screen" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/10.png" alt="Main Screen" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/11.png" alt="Main Screen" width="30%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/12.png" alt="Main Screen" width="30%">
</p>

## Certificate Verification
To verify the authenticity of the apk, compare the SHA-256 fingerprint of the app's signing certificate with the values below. A matching fingerprint confirms that the apk was signed with an official developer key.

- FOSS Release:  
  `DE:19:7B:4C:F2:B8:58:80:E0:21:11:59:B6:0B:6E:3C:39:F9:6F:F0:68:68:AE:48:39:C4:2D:8A:4D:6F:3F:E5`

- Google Play Store Release:  
  `4D:C3:38:FD:7C:2D:9D:CB:8C:9F:B1:76:AB:90:D3:D3:E0:B1:97:AB:D5:21:EC:05:E5:BD:BB:D7:60:B8:21:AE`

## Build Instructions
Clone the repository and navigate to the project directory:

```bash
git clone https://codeberg.org/Mental-Math/MentalMath.git
cd MentalMath
```

Make sure you have the required Android SDK and JDK (Java 17) installed. The project uses multiple product flavors (dev, foss, and play). To build a specific version, use one of the following commands:

```bash
./gradlew assembleDevRelease
./gradlew assembleFossRelease
./gradlew assemblePlayRelease
```

For successful signing, configure the keys in your gradle.properties file (compare signingConfigs in build.gradle.kts). If no keys are provided, the build will be unsigned. Since the original signing keys are private, using your own keys will break the [app links](https://developer.android.com/training/app-links/about?hl=de) for sharing a game. To make them work, follow these steps:

- Replace all instances of “mental-math.codeberg.page” relating to app links with your own domain.
- On your website, create an assetlinks.json file and place it at https://&lt;your-domain&gt;/.well-known/assetlinks.json
- Add the SHA-256 fingerprint of your key to the assetlinks.json like this:

```json
{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.helddertierwelt.mentalmath",
    "sha256_cert_fingerprints": [
      "YOUR_SHA256_FINGERPRINT"
    ]
  }
}
```

## License Information
Mental Math © 2026 HeldDerTierwelt  
This project is licensed under the [GNU General Public License Version 3 (GPLv3)](https://www.gnu.org/licenses/gpl-3.0.html). The full license text can also be found in the [LICENSE.md]("https://codeberg.org/HeldDerTierwelt/MentalMath/src/branch/main/LICENSE.md") file.  

## Privacy Policy
Your privacy is important. Here’s what you should know:

- The app does not collect any personal data, use trackers, ads, or third-party services.
- By default, no permissions are required. Only if you choose to set reminders will it request permission to send notifications, which is used exclusively for that feature.
- Opening a shared game link requires a brief internet connection only the first time you open such a link, so that Android can verify it and open the app. Apart from that, no internet connection is required.

## Donation
If you enjoy this app, you can support me via [Ko-fi](https://ko-fi.com/mental_math). It means a lot to me ❤️.

## Contact
Feel free to contact me for questions, feedback, issues or any other reason:

Alexander Goy  
aka HeldDerTierwelt  
mental_math_app@pm.me

<p align="center">
  <br>
  <img src="fastlane/metadata/android/en-US/images/icon_transparent.svg" alt="Mental Math Icon" width="200">
</p>