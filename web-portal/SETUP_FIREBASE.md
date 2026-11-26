# Center One Firebase setup

Use a dedicated Firebase project with Firestore in Native mode, Firebase Authentication and Cloud Messaging. Deploy Cloud Functions on the Blaze plan. Use one authoritative desktop database per Firebase project. The first sync binds the cloud project to that database. Keep its SQLite backup so a restored desktop retains the same identity. Sync reconciles parent and teacher accounts and the student records produced by that database.

1. Register a Firebase Web app. In Cloud Messaging, generate a Web Push certificate and copy its public VAPID key. Add the portal hostname under Authentication → Settings → Authorized domains.
2. Create `web-portal/.env.local` using the public Web app values and the public VAPID key. See `src/firebaseConfig.example.ts` for examples.

   ```dotenv
   VITE_FIREBASE_API_KEY=
   VITE_FIREBASE_AUTH_DOMAIN=
   VITE_FIREBASE_PROJECT_ID=
   VITE_FIREBASE_MESSAGING_SENDER_ID=
   VITE_FIREBASE_APP_ID=
   VITE_FIREBASE_VAPID_KEY=
   ```

3. From `web-portal/` run `npm ci`, `npm run build` and `firebase deploy --project YOUR_PROJECT_ID --only firestore:rules,functions,hosting`. The Functions runtime identity needs Firestore read/write access, permission to send FCM, and permission to sign custom tokens. Grant the runtime service account the Service Account Token Creator role on the signing account if required by your configuration.
4. Create a service account for the desktop with Firestore read/write and FCM send permissions. Store the JSON key outside the repository. Sign in to the desktop as Admin, select the JSON in Settings → Cloud Sync (Firebase) and test the connection. Enable Auto-sync or click the status pill to sync manually.
5. Open Student Details to copy the family PIN and Teacher Details for the teacher PIN. Egyptian mobile numbers saved as local numbers or with the country code resolve to the same account. A teacher phone must identify one teacher. Teachers can enter grades for students who have an active enrollment and a valid parent phone.
6. CSV and XLSX imports read the first row as headers. Include `code` or `name` and `score`. Arabic equivalents include `الكود`, `الاسم` and `الدرجة`. Store codes with leading zeros as text in Excel. Select an active group and set the exam title and maximum before desktop import. On the web, review imported scores before saving.

   ```csv
   code,name,score
   ST-001,,18.5
   ,Mona Adel,16
   ```

Cloud Functions validate the PIN and issue a scoped Firebase Auth token. Clients can read their own family’s portal sections, while PINs, teacher rosters, login-attempt counters and device tokens are server-only. Desktop service credentials and Functions use the Admin SDK. Sign-in sessions expire after 12 hours. Parent push notifications use a general message and the signed-in portal shows the event details. In iOS Safari, install the web app using **Add to Home Screen** before requesting push permission.

Before inviting families, test with two parent phones, siblings under one phone and teachers assigned to different groups. Confirm check-in and check-out times, one payment, a reminder, and one grade from each platform. Test push with a real device over HTTPS.
