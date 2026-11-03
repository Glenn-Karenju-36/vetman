
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
  setDoc,
  updateDoc,
  deleteDoc,
  getDoc,
  getDocs
} = require("firebase/firestore");

const projectId = "vetman-59a43";

const VET = "security-test-vet";
const OWNER_A = "security-test-owner-a";
const OWNER_B = "security-test-owner-b";

const PET_A = "security-test-pet-a";
const PET_B = "security-test-pet-b";

const RECORD_A = "security-test-record-a";
const APPOINTMENT_A = "security-test-appointment-a";
const BILL_A = "security-test-bill-a";
const PAYMENT_A = "security-test-payment-a";

const rulesPath = path.join(
  __dirname,
  "..",
  "firestore.rules"
);

async function main() {
  const env = await initializeTestEnvironment({
    projectId,
    firestore: {
      host: "127.0.0.1",
      port: 8080,
      rules: fs.readFileSync(rulesPath, "utf8")
    }
  });

  let passed = 0;

  // Run an access check and report whether the expected
  // Firestore permission was enforced.
  async function allowed(description, operation) {
    await assertSucceeds(operation());
    passed++;
    console.log(`PASS ${passed}: ${description}`);
  }

  async function denied(description, operation) {
    await assertFails(operation());
    passed++;
    console.log(`PASS ${passed}: ${description}`);
  }

  try {
    // IMPORTANT: Clears only the local Firestore Emulator.
    // Never point this test at the production database.
    await env.clearFirestore();

    // Create fixture data while bypassing rules.
    // This simulates records created by our application.
    await env.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();

      await setDoc(doc(db, "users", VET), {
        uid: VET,
        email: "security-vet@example.com",
        role: "veterinarian",
        vetApproved: true
      });

      await setDoc(doc(db, "users", OWNER_A), {
        uid: OWNER_A,
        email: "security-owner-a@example.com",
        role: "pet_owner"
      });

      await setDoc(doc(db, "users", OWNER_B), {
        uid: OWNER_B,
        email: "security-owner-b@example.com",
        role: "pet_owner"
      });

      await setDoc(doc(db, "pets", PET_A), {
        id: PET_A,
        vetId: VET,
        ownerId: OWNER_A,
        petName: "Cyrus Test",
        species: "dog",
        breed: "German Shepherd",
        age: "2",
        sex: "male",
        ownerName: "Owner A",
        ownerPhone: "0700000000",
        notes: "",
        createdAt: 1000
      });

      await setDoc(doc(db, "pets", PET_B), {
        id: PET_B,
        vetId: VET,
        ownerId: OWNER_B,
        petName: "Other Test Pet",
        species: "cat",
        breed: "",
        age: "1",
        sex: "female",
        ownerName: "Owner B",
        ownerPhone: "",
        notes: "",
        createdAt: 1000
      });

      await setDoc(
        doc(db, "pets", PET_A, "medicalRecords", RECORD_A),
        {
          id: RECORD_A,
          patientId: PET_A,
          vetId: VET,
          visitDate: 2000,
          symptoms: "Fever",
          diagnosis: "Test diagnosis",
          treatment: "Test treatment",
          medications: "",
          procedures: "",
          notes: "",
          createdAt: 2000
        }
      );

      await setDoc(doc(db, "appointments", APPOINTMENT_A), {
        id: APPOINTMENT_A,
        patientId: PET_A,
        vetId: VET,
        scheduledAt: 3000,
        status: "scheduled",
        reason: "Checkup",
        notes: "",
        createdAt: 2000
      });

      await setDoc(doc(db, "bills", BILL_A), {
        id: BILL_A,
        patientId: PET_A,
        vetId: VET,
        description: "Consultation",
        amount: 1000.0,
        amountPaid: 400.0,
        status: "partial",
        paymentMethod: "M-Pesa",
        notes: "",
        createdAt: 2000,
        updatedAt: 3000,
        lastPaymentId: PAYMENT_A
      });

      await setDoc(
        doc(db, "bills", BILL_A, "payments", PAYMENT_A),
        {
          id: PAYMENT_A,
          billId: BILL_A,
          vetId: VET,
          amount: 400.0,
          method: "M-Pesa",
          reference: "TEST400",
          createdAt: 3000
        }
      );
    });

    // Simulate authenticated users without creating real
    // Firebase Authentication accounts.
    const ownerADb = env.authenticatedContext(OWNER_A).firestore();
    const ownerBDb = env.authenticatedContext(OWNER_B).firestore();
    const vetDb = env.authenticatedContext(VET).firestore();
    const guestDb = env.unauthenticatedContext().firestore();

    // --------------------------------------------------
    // A. OWNER A: AUTHORIZED READ ACCESS
    // --------------------------------------------------

    await allowed(
      "Owner A can read their own pet",
      () => getDoc(doc(ownerADb, "pets", PET_A))
    );

    await allowed(
      "Owner A can list their linked pets",
      () => getDocs(
        query(
          collection(ownerADb, "pets"),
          where("ownerId", "==", OWNER_A)
        )
      )
    );

    await allowed(
      "Owner A can read their pet's medical history",
      () => getDocs(
        collection(
          ownerADb,
          "pets",
          PET_A,
          "medicalRecords"
        )
      )
    );

    await allowed(
      "Owner A can read their pet's appointments",
      () => getDocs(
        query(
          collection(ownerADb, "appointments"),
          where("patientId", "==", PET_A)
        )
      )
    );

    await allowed(
      "Owner A can read their pet's bills",
      () => getDocs(
        query(
          collection(ownerADb, "bills"),
          where("patientId", "==", PET_A)
        )
      )
    );

    await allowed(
      "Owner A can read their pet's payment history",
      () => getDocs(
        collection(ownerADb, "bills", BILL_A, "payments")
      )
    );

    // --------------------------------------------------
    // B. OWNER B: UNAUTHORIZED READ ACCESS
    // --------------------------------------------------

    await denied(
      "Owner B cannot read Owner A's pet",
      () => getDoc(doc(ownerBDb, "pets", PET_A))
    );

    await denied(
      "Owner B cannot read Owner A's medical records",
      () => getDoc(
        doc(ownerBDb, "pets", PET_A, "medicalRecords", RECORD_A)
      )
    );

    await denied(
      "Owner B cannot read Owner A's appointment",
      () => getDoc(
        doc(ownerBDb, "appointments", APPOINTMENT_A)
      )
    );

    await denied(
      "Owner B cannot read Owner A's bill",
      () => getDoc(doc(ownerBDb, "bills", BILL_A))
    );

    await denied(
      "Owner B cannot read Owner A's payment",
      () => getDoc(
        doc(ownerBDb, "bills", BILL_A, "payments", PAYMENT_A)
      )
    );

    // --------------------------------------------------
    // C. PET OWNERS: NO WRITE PERMISSIONS
    // --------------------------------------------------

    await denied(
      "Owner A cannot edit their pet",
      () => updateDoc(doc(ownerADb, "pets", PET_A), {
        petName: "Unauthorized Change"
      })
    );

    await denied(
      "Owner A cannot create medical records",
      () => setDoc(
        doc(ownerADb, "pets", PET_A, "medicalRecords", "fake-record"),
        {
          id: "fake-record",
          patientId: PET_A,
          vetId: VET,
          visitDate: 4000
        }
      )
    );

    await denied(
      "Owner A cannot edit medical records",
      () => updateDoc(
        doc(ownerADb, "pets", PET_A, "medicalRecords", RECORD_A),
        { diagnosis: "Changed diagnosis" }
      )
    );

    await denied(
      "Owner A cannot delete medical records",
      () => deleteDoc(
        doc(ownerADb, "pets", PET_A, "medicalRecords", RECORD_A)
      )
    );

    await denied(
      "Owner A cannot create appointments",
      () => setDoc(
        doc(ownerADb, "appointments", "fake-appointment"),
        {
          id: "fake-appointment",
          patientId: PET_A,
          vetId: VET,
          scheduledAt: 5000,
          status: "scheduled"
        }
      )
    );

    await denied(
      "Owner A cannot change appointment status",
      () => updateDoc(
        doc(ownerADb, "appointments", APPOINTMENT_A),
        { status: "cancelled" }
      )
    );

    await denied(
      "Owner A cannot delete appointments",
      () => deleteDoc(
        doc(ownerADb, "appointments", APPOINTMENT_A)
      )
    );

    await denied(
      "Owner A cannot create bills",
      () => setDoc(doc(ownerADb, "bills", "fake-bill"), {
        id: "fake-bill",
        patientId: PET_A,
        vetId: VET,
        amount: 500.0,
        amountPaid: 0.0,
        status: "unpaid"
      })
    );

    await denied(
      "Owner A cannot change bill balances",
      () => updateDoc(doc(ownerADb, "bills", BILL_A), {
        amountPaid: 1000.0,
        status: "paid"
      })
    );

    await denied(
      "Owner A cannot delete bills",
      () => deleteDoc(doc(ownerADb, "bills", BILL_A))
    );

    await denied(
      "Owner A cannot create payment records",
      () => setDoc(
        doc(ownerADb, "bills", BILL_A, "payments", "fake-payment"),
        {
          id: "fake-payment",
          billId: BILL_A,
          vetId: VET,
          amount: 600.0,
          method: "Cash",
          reference: "",
          createdAt: 5000
        }
      )
    );

    await denied(
      "Owner A cannot edit payment records",
      () => updateDoc(
        doc(ownerADb, "bills", BILL_A, "payments", PAYMENT_A),
        { amount: 1000.0 }
      )
    );

    await denied(
      "Owner A cannot delete payment records",
      () => deleteDoc(
        doc(ownerADb, "bills", BILL_A, "payments", PAYMENT_A)
      )
    );

    // --------------------------------------------------
    // D. SIGNED-OUT USERS
    // --------------------------------------------------

    await denied(
      "Signed-out user cannot read a pet",
      () => getDoc(doc(guestDb, "pets", PET_A))
    );

    await denied(
      "Signed-out user cannot read a bill",
      () => getDoc(doc(guestDb, "bills", BILL_A))
    );

    // --------------------------------------------------
    // E. VETERINARIAN ACCESS REMAINS INTACT
    // --------------------------------------------------

    await allowed(
      "Assigned veterinarian can read their patient",
      () => getDoc(doc(vetDb, "pets", PET_A))
    );

    await allowed(
      "Assigned veterinarian can read medical history",
      () => getDocs(
        collection(vetDb, "pets", PET_A, "medicalRecords")
      )
    );

    console.log(
      `\nALL ${passed} PET-OWNER SECURITY TESTS PASSED`
    );

  } finally {
    await env.cleanup();
  }
}

main().catch((error) => {
  console.error("\nPET-OWNER SECURITY TEST FAILED:");
  console.error(error);
  process.exitCode = 1;
});
