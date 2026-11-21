# Dit Dah: from these files to a published app (₹0 route)

You don't need to code or install anything. GitHub builds the app on its servers for free. Budget about an hour for Part 1 and Part 2.

**What you have:**

- `DitDah.zip`: the complete app code, ready to upload.
- `DitDah-signing-key-KEEP-PRIVATE.zip`: your app's digital signature. Back it up in two places (for example Google Drive and a pen drive). **Never upload it to GitHub as a file.**

---

## Part 1: Get the app onto your own phone (about 30 minutes)

### 1. Create a GitHub account

Go to github.com and sign up (free). Choose a username you're happy to show publicly, because it appears in the app's web addresses.

### 2. Create a repository (a project folder on GitHub)

1. Click **+** (top right) and choose **New repository**.
2. Name it `ditdah`, choose **Public**, and tick **Add a README file**.
3. Click **Create repository**.

### 3. Upload the code

1. Unzip `DitDah.zip` on your computer. You'll get a folder called `DitDah`.
2. In your repository, click **Add file** and choose **Upload files**.
3. Open the `DitDah` folder, select **everything inside it** (including the `.github` folder; on Windows, turn on *View › Show › Hidden items*; on a Mac, press Cmd+Shift+. in Finder) and drag it all onto the GitHub page.
4. When the upload finishes, click **Commit changes**. If GitHub asks, let it replace the README.

**Check:** your repository should now show the folders `.github`, `app`, `docs`, `fastlane` and `gradle`.

**If `.github` is missing:**

1. Click **Add file** and choose **Create new file**.
2. Type `.github/workflows/build.yml` as the name.
3. Paste in the contents of that file from the zip, then commit.

### 4. Watch GitHub build your app

1. Click the **Actions** tab. A run called **Build app** will appear. If GitHub asks you to enable workflows, click the green button.
2. Wait about 5 to 8 minutes for a green tick ✅. A red ✗ means something failed: click it, copy the red error text, and paste it to Claude.
3. Click the finished run and scroll down to **Artifacts**. Download **DitDah-test-apk** (a zip containing `app-debug.apk`).

### 5. Install it on your phone

1. Send the APK to your phone, for example by emailing it to yourself or putting it in Google Drive.
2. Tap it on the phone. Android will ask you to allow installs from that app (Files, Chrome or Drive), so allow it, then tap **Install**.
3. Open **Dit Dah**, type something and tap **Play**. Also try the flashlight and vibrate outputs, the Decode tap key and the Chart.

This test version can't update over itself. If you install a newer test build, uninstall the old one first.

---

## Part 2: Make it public for free (about 30 minutes)

### 6. Add your signing key to GitHub (as secrets, never as files)

1. In your repository, go to **Settings › Secrets and variables › Actions › New repository secret**.
2. Open `README-KEEP-THIS-SAFE.txt` from the key zip.
3. Create these four secrets, one at a time. The values are all in that text file.

| Name | Value |
|---|---|
| `KEYSTORE_BASE64` | the entire contents of `keystore-base64.txt` |
| `KEYSTORE_PASSWORD` | the password in the text file |
| `KEY_ALIAS` | `ditdah` |
| `KEY_PASSWORD` | the same password |

Then run the build again: **Actions › Build app › Run workflow**. This time it also produces **DitDah-release**, which contains the signed `.apk` and the `.aab` that Google Play needs.

### 7. Put your privacy policy online

1. In GitHub, open `docs/index.md` and click the pencil icon.
2. Replace `YOUR-EMAIL@example.com` with your contact email, then commit.
3. Go to **Settings › Pages**. Under *Branch*, choose `main` and the `/docs` folder, then click **Save**.
4. After a minute your policy is live at `https://YOUR-USERNAME.github.io/ditdah/`. Every store will ask for this link.

### 8. Publish a release

1. Click **Releases** (right side of the repository) and choose **Draft a new release**.
2. Under *Choose a tag*, type `v1.0.0` and choose **Create new tag**. Set the title to `Dit Dah 1.0.0`.
3. Click **Publish release**.

About 8 minutes later the signed `DitDah-1.0.0.apk` is attached to the release automatically. Anyone can now download your app from that page.

### 9. List it on free app stores

**Indus Appstore (PhonePe Appstore).** It's free for developers and reaches Indian users.

1. Sign up on its developer platform.
2. Upload the `.apk` or `.aab` from **DitDah-release**.
3. For the listing, use the text and images in `fastlane/metadata/android/en-US/`.

**IzzyOnDroid.** This gets you into the F-Droid app ecosystem.

1. Make a free account on codeberg.org.
2. Open a new issue on the IzzyOnDroid repository requesting inclusion.
3. Include your GitHub repository link. Your app already has the open-source licence, the release APK and the store text that IzzyOnDroid needs.

**Take screenshots for the listings.** On your phone, open each of the three screens and take a screenshot. Then upload them to GitHub at `fastlane/metadata/android/en-US/images/phoneScreenshots/`, named `1.png`, `2.png` and `3.png`.

### 10. Register the app name before Google's new rules reach India

From early 2027 Android phones will warn about or block apps from unregistered developers, including apps installed from GitHub.

1. Create a free **limited distribution** account at the Android Developer Console (developer.android.com/developer-verification).
2. Register the package name `com.kaushikc.ditdah` there.

---

## Part 3: Google Play (when you can spend ₹2,100 once)

There's no free way onto Play, but everything else is already prepared. When you're ready:

1. **Before paying:**
   - Turn on international transactions on your debit or credit card.
   - Keep your physical PAN card (or passport, Voter ID or driving licence) and a proof of address ready.
   - Aadhaar and e-PAN are not accepted.
2. **Sign up and verify.** Go to play.google.com/console, register as a **Personal** account, pay US$25 and complete ID verification (usually 2 to 5 days).
3. **Create the app.**
   - Name: `Dit Dah: Morse Code`, a free app.
   - Store listing: copy the text from `fastlane/metadata/android/en-US/` and upload `icon.png`, `featureGraphic.png` and your screenshots.
4. **Fill in the App content section.**
   - Privacy policy: your GitHub Pages link.
   - Ads: none.
   - Data safety: **no data collected, no data shared**.
   - Content rating: answer "no" to everything.
   - Target audience: 13+ or 18+.
5. **Run a closed test.**
   - Go to **Testing › Closed testing**, upload the `.aab` from **DitDah-release**, and add **12 or more testers** by Gmail address. Friends, family and colleagues are fine.
   - They must opt in using the link and stay opted in for **14 days in a row**, and they should actually use the app.
6. **Apply for production access.** Answer Google's questions honestly (the review usually takes up to 7 days), then publish.

---

## Updating the app later

Before each new release:

1. In `app/build.gradle.kts`, increase `versionCode` by 1 (1 → 2 → 3…) and update `versionName` (for example `1.0.1`).
2. Commit the change and publish a new release with a matching tag (`v1.0.1`).

Ask Claude to make code changes. You only need to upload the changed files.

## If something goes wrong

- **The build turned red:** open the failed step, copy the error and give it to Claude.
- **The phone says "App not installed":** uninstall the old version first. This happens when switching between test and release builds.
- **The flashlight chip is greyed out:** the phone has no flash, or another app is using the camera.
