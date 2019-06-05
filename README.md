# intercor-app
An android app to track position and on-board messages. A user can select icons below a map. The position and icon selection is stored in a local CSV that is synced regularly to Google Team Drive.

![app workflow](https://raw.githubusercontent.com/wegenenverkeer/intercor-app/master/eu-funding.png)  
Grant Agreement No: INEA/CEF/TRAN/M2015/1143833  
Action No: 2015-EU-TM-0159-S

Step-by-step developer guide
============================
- STEP 1: Install Android Studio
  + https://developer.android.com/studio
- STEP 2: Clone the repository
  + https://github.com/WegenenVerkeer/intercor-app.git
- STEP 3: Create a Google account
  + https://accounts.google.com
- STEP 4: Create Firebase project
  - STEP 4.1 Go to Firebase 
    + https://firebase.google.com/
  - STEP 4.2 Log on with Google account
  - STEP 4.3 Click 'Add project', set project name and click 'Create project'
  - STEP 4.4 Click on the configuration icon (gear) and click 'Project settings'
  - STEP 4.5 In settings/general the web api key is found
  - STEP 4.6 Click 'Add Firebase to your Android App'
  - STEP 4.7 Fill in the project name and click 'Register App'
  - STEP 4.8 Click 'Download google-services.json'
  - STEP 4.9 In settings/cloud messaging the server key is found
- STEP 5: Organize the Firebase project
  - STEP 5.1 In develop/authentication add the allowed users
  - STEP 5.2 In develop/database/data add a new collection and name it 'icons'. Add a first document with the following fields:
    + detail: string
    + message: string
    + position: number
    + type: string
    + urlIcon: string
  - STEP 5.3 In develop/database/rules add the following code:
    >  service cloud.firestore {
    >    match /databases/{database}/documents {
    >      match /{document=**} {
    >        allow read, write: if request.auth.uid != null;
    >      }
    >    }
    >  }
  - STEP 5.4 In develop/storage/files add a folder 'icons' and upload all required PNG files
  - STEP 5.5 In develop/storage/rules add the following code:
    > service firebase.storage {
    >   match /b/{bucket}/o {
    >     match /{allPaths=**} {
    >       allow read, write: if request.auth.uid != null;
    >     }
    >   }
    > }
- STEP 6: Create Google Maps project and get the key
  + https://developers.google.com/maps/documentation/embed/get-api-key
- STEP 7: Download debug and release key and put them in c:\users\<username>\_android
   + https://developer.android.com/studio/publish/app-signing#generate-key
- STEP 8: Personalize the Android project
  - STEP 8.1 Project name
    + In app/src/main/res/values/strings.xml set the app name under <--APP_NAME-->
    + In app/src/main/macq/intercor/MainActivity.java replace <--APP_NAME-->
  - STEP 8.2 Release and debug key
    + This is an **important** point, it will be necessary during the development and for the release. To use some features (like Google Drive), we need to "Sign" our APK. Go to : app/build.gradle and replace <-- USERNAME -->, <-- PASSWORD -->, <-- APP_ID -->, and <-- APP_NAME -->
- STEP 9: Add the *google-services.json* downloaded in step 4.8 to the folder /app
- STEP 10: Add Google Maps key to the project
  + The API key for Google Maps-based APIs is defined as a string resource. Note that the API key is linked to the encryption key used to sign the APK. As such a different API key is needed for each encryption key, including the release key that is used to sign the APK for publishing.
  + For debug, go to app/src/debug/res/values/google_maps_api.xml and replace <-- MAPS_API_KEY -->
  + For release, go to app/src/release/res/values/google_maps_api.xml and replace <-- MAPS_API_KEY -->
- STEP 11: Add settings keys
  + https://docs.fabric.io/android/fabric/overview.html
- STEP 12: Compile the app and verify correctness of the keys
  + app/build/generated/res/google-services/debug/values/values.xml
  + app/build/generated/res/google-services/release/values/values.xml
- STEP 13: Set build variants
  + In order to use more advanced features, the release variant had to be used (both variants can be easily switched, and it is
recommended to stick on release variant).
- STEP 14: Add permissions to the Google Team Drive

Workflow
========
![app workflow](https://raw.githubusercontent.com/wegenenverkeer/intercor-app/master/workflow.png)

How to
======
## How to debug app?
To launch the app, you can use a built-in emulator of Android Studio, but you can also (and it is recommended) to use your own phone to run the app. To do so, you have to enable the developer mode of your phone. Please refer to the phone's brand and follow tutorials to enable it. (Search: USB debugging) Once it is enabled, connect your phone to your computer. If prompted, accept the connection. Also, if prompted, transfer file must be used (and not only charging or other option) If you run the app, your phone will be used and the application will be installed in it.
## How to build your APK?
Once your development is ready to release:
Don't forget to change the versionName in the build.gradle file following the semver versioning.
Commit and push files to the server beginning with the versionName in the commit message.
In Android Studio, go to "Build > Generate Signed Bundle / APK... > Build APK".
Once the APK is generated, you will see a message in the bottom right of Android Studio. Click on "locate".
You can see your APK built with the version you provided. This file can be sent to an Android device and you can install the application.
