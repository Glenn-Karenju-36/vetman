
/**
 * Vetman — Pet Owner Linking Security Tests
 *
 * Runs ONLY against the local Firestore emulator.
 * No real Firebase accounts or production pets are modified.
 *
 * Run:
 * firebase emulators:exec --only firestore "node test.cjs"
 */

const fs = require("node:fs");
const path = require("node:path");

const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} = require("@firebase/rules-unit-testing");

const {
  doc,
  collection,
  query,
  where,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  deleteDoc,
  writeBatch,
  serverTimestamp
} = require("firebase/firestore");

// -----------------------------------------------------
// CONFIGURATION
// -----------------------------------------------------

const PROJECT_ID = "demo-vetman-link-tests";
const RULES_PATH = path.join(__dirname, "..", "firestore.rules");

const VET_UID = "test-vet-approved";
const OWNER_UID = "test-owner-monkey";
const OTHER_UID = "test-other-owner";
const OTHER_VET_UID = "test-other-vet";
const UNAPPROVED_VET_UID = "test-unapproved-vet";

const OWNER_EMAIL = "karenjumaina@gmail.com";
const OTHER_EMAIL = "another-owner@example.com";

const PET_ID = "monkey-siamese-test";

const PET_PATH = `pets/${PET_ID}`;
const REQUEST_PATH = `petLinkRequests/${PET_ID}`;

const PET = {
  id: PET_ID,
  vetId: VET_UID,
  ownerId: "",
  petName: "Monkey",
  species: "Cat",
  breed: "Siamese",
  age: "2",
  sex: "Female",
  ownerName: "",
  ownerPhone: "",
  notes: ""
};

let env;
let vetDb;
let ownerDb;
let otherDb;
let otherVetDb;
let unapprovedVetDb;
let unverifiedOwnerDb;

let passed = 0;
let failed = 0;

// -----------------------------------------------------
// HELPERS
// -----------------------------------------------------

function petRef(db) {
  return doc(db, PET_PATH);
}

function requestRef(db) {
  return doc(db, REQUEST_PATH);
}

function pendingRequest() {
  return {
    id: PET_ID,
    patientId: PET_ID,
    vetId: VET_UID,
    ownerEmail: OWNER_EMAIL,
    status: "pending",
    createdAt: serverTimestamp(),

    // Safe preview fields for the intended owner.
    petName: "Monkey",
    species: "Cat",
    breed: "Siamese"
  };
}

async function createRequest(db = vetDb) {
  return setDoc(requestRef(db), pendingRequest());
}

async function acceptRequest(db = ownerDb, uid = OWNER_UID) {
  const batch = writeBatch(db);

  // The request and patient ownership change together.
  batch.update(requestRef(db), {
    status: "accepted",
    ownerUid: uid,
    acceptedAt: serverTimestamp()
  });

  batch.update(petRef(db), {
    ownerId: uid
  });

  return batch.commit();
}

async function declineRequest(db = ownerDb) {
  return updateDoc(requestRef(db), {
    status: "declined",
    declinedAt: serverTimestamp()
  });
}

// Reset the emulated database before every test.
// Rules are bypassed ONLY for test fixture preparation.
async function seed() {
  await env.clearFirestore();

  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();

    const users = [
      {
        uid: VET_UID,
        email: "vet@example.com",
        role: "veterinarian",
        vetApproved: true
      },
      {
        uid: OWNER_UID,
        email: OWNER_EMAIL,
        role: "pet_owner"
      },
      {
        uid: OTHER_UID,
        email: OTHER_EMAIL,
        role: "pet_owner"
      },
      {
        uid: OTHER_VET_UID,
        email: "other-vet@example.com",
        role: "veterinarian",
        vetApproved: true
      },
      {
        uid: UNAPPROVED_VET_UID,
        email: "unapproved@example.com",
        role: "veterinarian",
        vetApproved: false
      }
    ];

    const batch = writeBatch(db);

    for (const user of users) {
      batch.set(doc(db, "users", user.uid), user);
    }

    batch.set(doc(db, PET_PATH), PET);

    await batch.commit();
  });
}

async function test(name, operation) {
  try {
    await seed();
    await operation();

    passed++;
    console.log(`PASS  ${name}`);
  } catch (error) {
    failed++;
    console.error(`FAIL  ${name}`);
    console.error(`      ${error.message}`);
  }
}

// -----------------------------------------------------
// TEST SUITE
// -----------------------------------------------------

async function run() {
  if (!fs.existsSync(RULES_PATH)) {
    throw new Error(
      "firestore.rules not found beside test.cjs"
    );
  }

  // Connect exclusively to the emulator.
  // A missing emulator causes an error, not a live write.
  const emulatorAddress =
    process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8080";

  const [host, portText] = emulatorAddress.split(":");
  const port = Number(portText);

  if (!host || !Number.isInteger(port) || port <= 0) {
    throw new Error(
      "Invalid FIRESTORE_EMULATOR_HOST: " + emulatorAddress
    );
  }

  env = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host,
      port,
      rules: fs.readFileSync(RULES_PATH, "utf8")
    }
  });

  // Mock accounts, including real-looking Auth token claims.
  vetDb = env.authenticatedContext(VET_UID, {
    email: "vet@example.com",
    email_verified: true
  }).firestore();

  ownerDb = env.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true
  }).firestore();

  otherDb = env.authenticatedContext(OTHER_UID, {
    email: OTHER_EMAIL,
    email_verified: true
  }).firestore();

  otherVetDb = env.authenticatedContext(OTHER_VET_UID, {
    email: "other-vet@example.com",
    email_verified: true
  }).firestore();

  unapprovedVetDb = env.authenticatedContext(
    UNAPPROVED_VET_UID,
    {
      email: "unapproved@example.com",
      email_verified: true
    }
  ).firestore();

  unverifiedOwnerDb = env.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: false
  }).firestore();

  console.log("\nVETMAN — OWNER LINKING RULES");
  console.log("Pet: Monkey (Siamese cat)");
  console.log("Destination: " + OWNER_EMAIL);
  console.log("Project: " + PROJECT_ID);
  console.log("----------------------------------------\n");

  // TEST 1: Unlinked owners cannot access private pet data.
  await test(
    "01 Unlinked owner cannot read Monkey",
    async () => {
      await assertFails(getDoc(petRef(ownerDb)));
    }
  );

  // TEST 2: The assigned vet retains normal patient access.
  await test(
    "02 Assigned vet can read Monkey",
    async () => {
      await assertSucceeds(getDoc(petRef(vetDb)));
    }
  );

  // TEST 3: Assigned approved vet may create a request.
  await test(
    "03 Vet can create Monkey's pending request",
    async () => {
      await assertSucceeds(createRequest());
      const snapshot = await assertSucceeds(
        getDoc(requestRef(vetDb))
      );

      if (!snapshot.exists()) {
        throw new Error("Request was not created");
      }

      if (snapshot.data().status !== "pending") {
        throw new Error("Incorrect request status");
      }
    }
  );

  // TEST 4: A different veterinarian cannot invite Monkey.
  await test(
    "04 Unrelated vet cannot create the request",
    async () => {
      await assertFails(createRequest(otherVetDb));
    }
  );

  // TEST 5: An unapproved vet cannot create the request.
  await test(
    "05 Unapproved vet cannot create the request",
    async () => {
      await assertFails(createRequest(unapprovedVetDb));
    }
  );

  // TEST 6: The intended verified owner can see the request.
  await test(
    "06 Verified owner can read Monkey's request",
    async () => {
      await assertSucceeds(createRequest());

      const snapshot = await assertSucceeds(
        getDoc(requestRef(ownerDb))
      );

      if (snapshot.data().petName !== "Monkey") {
        throw new Error("Pet preview is incorrect");
      }
    }
  );

  // TEST 7: Owner dashboard query by email must work.
  await test(
    "07 Verified owner can query pending requests",
    async () => {
      await assertSucceeds(createRequest());

      const requests = query(
        collection(ownerDb, "petLinkRequests"),
        where("ownerEmail", "==", OWNER_EMAIL)
      );

      const results = await assertSucceeds(
        getDocs(requests)
      );

      if (results.size !== 1) {
        throw new Error(
          `Expected 1 request, got ${results.size}`
        );
      }
    }
  );

  // TEST 8: Another owner cannot see Monkey's invitation.
  await test(
    "08 Unrelated owner cannot read the request",
    async () => {
      await assertSucceeds(createRequest());
      await assertFails(getDoc(requestRef(otherDb)));
    }
  );

  // TEST 9: Verification must actually be required.
  await test(
    "09 Unverified owner cannot read the request",
    async () => {
      await assertSucceeds(createRequest());
      await assertFails(
        getDoc(requestRef(unverifiedOwnerDb))
      );
    }
  );

  // TEST 10: A duplicate invitation is not permitted.
  await test(
    "10 Duplicate request creation is rejected",
    async () => {
      await assertSucceeds(createRequest());

      // setDoc on an existing document is an update.
      // The vet is not allowed to reset existing requests.
      await assertFails(createRequest());
    }
  );

  // TEST 11: The intended owner can accept atomically.
  await test(
    "11 Owner can accept and link Monkey atomically",
    async () => {
      await assertSucceeds(createRequest());
      await assertSucceeds(acceptRequest());

      const pet = await assertSucceeds(
        getDoc(petRef(ownerDb))
      );

      const request = await assertSucceeds(
        getDoc(requestRef(ownerDb))
      );

      if (pet.data().ownerId !== OWNER_UID) {
        throw new Error("Pet ownerId was not updated");
      }

      if (request.data().status !== "accepted") {
        throw new Error("Request was not accepted");
      }

      if (request.data().ownerUid !== OWNER_UID) {
        throw new Error("Accepted owner UID is wrong");
      }
    }
  );

  // TEST 12: Ownership cannot be assigned directly.
  await test(
    "12 Owner cannot change ownerId without acceptance",
    async () => {
      await assertSucceeds(createRequest());

      await assertFails(
        updateDoc(petRef(ownerDb), {
          ownerId: OWNER_UID
        })
      );
    }
  );

  // TEST 13: Request cannot be accepted alone.
  await test(
    "13 Request cannot be accepted without pet update",
    async () => {
      await assertSucceeds(createRequest());

      await assertFails(
        updateDoc(requestRef(ownerDb), {
          status: "accepted",
          ownerUid: OWNER_UID,
          acceptedAt: serverTimestamp()
        })
      );
    }
  );

  // TEST 14: An unrelated owner cannot accept.
  await test(
    "14 Another owner cannot accept Monkey's request",
    async () => {
      await assertSucceeds(createRequest());
      await assertFails(
        acceptRequest(otherDb, OTHER_UID)
      );
    }
  );

  // TEST 15: Decline is allowed without linking.
  await test(
    "15 Owner can decline without linking Monkey",
    async () => {
      await assertSucceeds(createRequest());
      await assertSucceeds(declineRequest());

      const request = await assertSucceeds(
        getDoc(requestRef(ownerDb))
      );

      if (request.data().status !== "declined") {
        throw new Error("Request was not declined");
      }

      // Vet may still read the unlinked pet.
      const pet = await assertSucceeds(
        getDoc(petRef(vetDb))
      );

      if (pet.data().ownerId !== "") {
        throw new Error(
          "Declining unexpectedly linked Monkey"
        );
      }
    }
  );

  // TEST 16: Declined requests cannot be accepted.
  await test(
    "16 Declined request cannot later be accepted",
    async () => {
      await assertSucceeds(createRequest());
      await assertSucceeds(declineRequest());
      await assertFails(acceptRequest());
    }
  );

  // TEST 17: An already-linked pet cannot be invited.
  await test(
    "17 Vet cannot invite an already-linked pet",
    async () => {
      await env.withSecurityRulesDisabled(
        async (context) => {
          await updateDoc(
            doc(context.firestore(), PET_PATH),
            { ownerId: OTHER_UID }
          );
        }
      );

      await assertFails(createRequest());
    }
  );

  // TEST 18: The vet can remove a pending invitation.
  await test(
    "18 Assigned vet can delete a pending request",
    async () => {
      await assertSucceeds(createRequest());
      await assertSucceeds(
        deleteDoc(requestRef(vetDb))
      );
    }
  );

  // TEST 19: An accepted invitation is retained.
  await test(
    "19 Vet cannot delete accepted link history",
    async () => {
      await assertSucceeds(createRequest());
      await assertSucceeds(acceptRequest());
      await assertFails(
        deleteDoc(requestRef(vetDb))
      );
    }
  );

  // TEST 20: The owner cannot edit medical details.
  await test(
    "20 Linked owner cannot edit clinical pet fields",
    async () => {
      await assertSucceeds(createRequest());
      await assertSucceeds(acceptRequest());

      await assertFails(
        updateDoc(petRef(ownerDb), {
          petName: "Changed without vet permission"
        })
      );
    }
  );

  console.log("\n----------------------------------------");
  console.log(`PASSED: ${passed}`);
  console.log(`FAILED: ${failed}`);
  console.log(`TOTAL:  ${passed + failed}`);

  if (failed > 0) {
    process.exitCode = 1;
  } else {
    console.log("\nALL LINKING SECURITY TESTS PASSED");
  }
}

run()
  .catch((error) => {
    console.error("\nTEST RUNNER ERROR:");
    console.error(error);
    process.exitCode = 1;
  })
  .finally(async () => {
    if (env) {
      await env.cleanup();
    }
  });
