# Universe Pro manual UPI access setup

The existing `universe-origin-notes.html` remains public: it was already published in this public repository and its Git history. Do not copy paid-only material into public HTML or other tracked files. The new `/universe-pro.html` fetches expanded notes only from Firestore after checking the signed-in user's membership document. Firestore rules are the actual access control; hiding page elements alone is not protection.

## One-time Firebase Console setup

Use the Firebase project `space-f58de`:

1. In **Build → Firestore Database**, create the database if it does not exist.
2. Open **Firestore Database → Rules**, replace the rules with the repository's `firestore.rules`, and publish them. Do not use open/test-mode rules.
3. In **Firestore Data**, create `subscriptionConfig` → document `pro` with string field `upiId`, number field `amountInr`, and string field `instructions`. Do not put a UPI PIN, password, or banking credential in Firestore.
4. Create `proContent` → document `universe` with string fields `title` and `intro`, and an array field `sections`. Each array item is a map with string fields `heading` and `body`. Put the expanded Pro-only notes here, not in this public repository.

## Manually approve a UPI payment

A learner signs into the site, pays the configured amount using UPI, and submits the transaction reference. Their request appears under `proRequests`, with the Firebase Auth UID as the document ID. Verify the transfer in your own UPI app/bank first. Only then, in Firestore, create `proMembers` → document with that exact UID as the ID and boolean field `active` set to `true`. To revoke access, set `active` to `false` or delete that membership document. Users cannot edit membership, requests, or Pro content under the supplied rules.

Each account can submit one request. To allow a rejected user to submit again, delete their old `proRequests/{uid}` document after reviewing it.

## Important

The Firebase Console setup is required before the new page can accept requests or serve Pro notes. The page shows a setup message until Firestore and the documents above exist. Do not approve a payment reference from the website alone; verify it independently. The public free notes remain available, as agreed, while the Pro edition is protected by Firestore rules.
