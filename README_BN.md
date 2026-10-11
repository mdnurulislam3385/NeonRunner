# Neon Runner — Android starter project

এটি একটি native Kotlin/Canvas endless runner starter project। এতে আছে:
- Main menu, endless runner gameplay, score/high score
- Swipe left/right lane change, swipe up jump
- সময়ের সাথে speed বৃদ্ধি, coins collect করা
- Coin Store screen এবং Google Play Billing Library 9.1.0-এর one-time product flow

## Android Studio-তে চালানো
1. ZIP extract করুন এবং Android Studio-তে `NeonRunner` folder Open করুন।
2. Gradle sync শেষ হতে দিন (ইন্টারনেট লাগবে)।
3. Run করে local gameplay পরীক্ষা করুন।

## Google Play Billing চালু করার জন্য
Google Play Console-এ app তৈরি করে Monetize > Products > One-time products-এ এই product ID-গুলো তৈরি করুন:
- `coins_500`
- `coins_1500`
- `coins_5000`
- `coins_12000`

প্রতিটি product-এর মূল্য, availability, এবং activation সেট করুন। অ্যাপটি Google Play-এর internal testing track-এ publish করে tester account দিয়ে purchase পরীক্ষা করুন। sideloaded APK-তে real purchase flow নির্ভরযোগ্যভাবে পরীক্ষা করা যায় না।

## গুরুত্বপূর্ণ production note
এই starter code purchase acknowledgement করে এবং client-side coins যোগ করে। এটি prototype-এর জন্য; production release-এ purchase token secure backend-এ Google Play Developer API দিয়ে verify না করে coins grant করবেন না। Restore/reconciliation, duplicate-token protection, server-side verification এবং privacy policy যোগ করতে হবে। Price UI-তে hard-code করা নেই; Google Play থেকে product details load হলে purchase flow চালু হবে।

Package: `com.kadeza.neonrunner`
