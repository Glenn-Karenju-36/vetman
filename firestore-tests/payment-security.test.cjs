
const fs = require("node:fs");
const path = require("node:path");

const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} = require("@firebase/rules-unit-testing");

const {
  doc,
  setDoc,
  updateDoc,
  getDoc,
  runTransaction
} = require("firebase/firestore");

const projectId = "vetman-59a43";

const VET_UID = "test-approved-vet";
const OWNER_UID = "test-unrelated-owner";
const PATIENT_ID = "test-patient";
const BILL_ID = "test-bill";

// Load our local Firestore security rules.
const rulesPath = path.join(__dirname, "..", "firestore.rules");

async function main() {
  const env = await initializeTestEnvironment({
    projectId,
    firestore: {
      host: "127.0.0.1",
      port: 8080,
      rules: fs.readFileSync(rulesPath, "utf8")
    }
  });

  try {
    // Clear only the local emulator test database.
    await env.clearFirestore();

    // Create isolated test records without security restrictions.
    await env.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();

      await setDoc(doc(db, "users", VET_UID), {
        uid: VET_UID,
        email: "testvet@example.com",
        role: "veterinarian",
        vetApproved: true
      });

      await setDoc(doc(db, "users", OWNER_UID), {
        uid: OWNER_UID,
        email: "testowner@example.com",
        role: "pet_owner"
      });

      await setDoc(doc(db, "pets", PATIENT_ID), {
        id: PATIENT_ID,
        vetId: VET_UID,
        ownerId: "",
        petName: "Test Dog"
      });

      await setDoc(doc(db, "bills", BILL_ID), {
        id: BILL_ID,
        patientId: PATIENT_ID,
        vetId: VET_UID,
        description: "Test consultation",
        amount: 2500.0,
        amountPaid: 0.0,
        status: "unpaid",
        paymentMethod: "",
        notes: "",
        createdAt: 1000,
        updatedAt: 1000,
        lastPaymentId: ""
      });
    });

    const vetDb = env.authenticatedContext(VET_UID).firestore();
    const ownerDb = env.authenticatedContext(OWNER_UID).firestore();

    const billRef = doc(vetDb, "bills", BILL_ID);
    const paymentRef = doc(
      vetDb,
      "bills",
      BILL_ID,
      "payments",
      "payment-001"
    );

    // TEST 1: Valid payment and bill update in one transaction.
    await assertSucceeds(
      runTransaction(vetDb, async (transaction) => {
        const snapshot = await transaction.get(billRef);
        const previousPaid = snapshot.data().amountPaid;
        const now = 2000;

        transaction.set(paymentRef, {
          id: "payment-001",
          billId: BILL_ID,
          vetId: VET_UID,
          amount: 500.0,
          method: "M-Pesa",
          reference: "TEST123",
          createdAt: now
        });

        transaction.update(billRef, {
          amountPaid: previousPaid + 500.0,
          status: "partial",
          paymentMethod: "M-Pesa",
          updatedAt: now,
          lastPaymentId: "payment-001"
        });
      })
    );

    console.log("PASS 1: Valid payment transaction allowed");

    const updatedBill = await getDoc(billRef);

    if (
      updatedBill.data().amountPaid !== 500 ||
      updatedBill.data().status !== "partial"
    ) {
      throw new Error("Bill balance or status is incorrect");
    }

    console.log("PASS 2: Bill balance updated correctly");

    // TEST 3: Direct balance manipulation must fail.
    await assertFails(
      updateDoc(billRef, {
        amountPaid: 1000.0,
        status: "partial",
        updatedAt: 3000
      })
    );

    console.log("PASS 3: Direct balance manipulation denied");

    // TEST 4: Standalone payment creation must fail.
    const orphanPaymentRef = doc(
      vetDb,
      "bills",
      BILL_ID,
      "payments",
      "payment-002"
    );

    await assertFails(
      setDoc(orphanPaymentRef, {
        id: "payment-002",
        billId: BILL_ID,
        vetId: VET_UID,
        amount: 100.0,
        method: "Cash",
        reference: "",
        createdAt: 3000
      })
    );

    console.log("PASS 4: Standalone payment creation denied");

    // TEST 5: Unrelated pet owner cannot modify the bill.
    await assertFails(
      updateDoc(doc(ownerDb, "bills", BILL_ID), {
        amountPaid: 1500.0,
        status: "partial"
      })
    );

    console.log("PASS 5: Unauthorized pet-owner update denied");

    // TEST 6: Reusing an existing payment must fail.
    // payment-001 was already used in the successful transaction.
    await assertFails(
      updateDoc(billRef, {
        amountPaid: 1000.0,
        status: "partial",
        paymentMethod: "M-Pesa",
        updatedAt: 4000,
        lastPaymentId: "payment-001"
      })
    );

    console.log("PASS 6: Payment replay denied");

    // TEST 7: Payment amount must match the bill balance increase.
    const mismatchPaymentRef = doc(
      vetDb,
      "bills",
      BILL_ID,
      "payments",
      "payment-003"
    );

    await assertFails(
      runTransaction(vetDb, async (transaction) => {
        const snapshot = await transaction.get(billRef);

        transaction.set(mismatchPaymentRef, {
          id: "payment-003",
          billId: BILL_ID,
          vetId: VET_UID,
          amount: 100.0,
          method: "Cash",
          reference: "",
          createdAt: 5000
        });

        // Deliberately inconsistent: KSh 100 payment,
        // but KSh 500 increase in the bill balance.
        transaction.update(billRef, {
          amountPaid: snapshot.data().amountPaid + 500.0,
          status: "partial",
          paymentMethod: "Cash",
          updatedAt: 5000,
          lastPaymentId: "payment-003"
        });
      })
    );

    console.log("PASS 7: Mismatched payment amount denied");

    // TEST 8: Another veterinarian cannot record a payment.
    const otherVetDb = env.authenticatedContext(
      "test-other-vet"
    ).firestore();

    const otherVetBillRef = doc(
      otherVetDb,
      "bills",
      BILL_ID
    );

    const otherVetPaymentRef = doc(
      otherVetDb,
      "bills",
      BILL_ID,
      "payments",
      "payment-004"
    );

    await assertFails(
      runTransaction(otherVetDb, async (transaction) => {
        const snapshot = await transaction.get(otherVetBillRef);

        transaction.set(otherVetPaymentRef, {
          id: "payment-004",
          billId: BILL_ID,
          vetId: "test-other-vet",
          amount: 100.0,
          method: "Cash",
          reference: "",
          createdAt: 6000
        });

        transaction.update(otherVetBillRef, {
          amountPaid: snapshot.data().amountPaid + 100.0,
          status: "partial",
          paymentMethod: "Cash",
          updatedAt: 6000,
          lastPaymentId: "payment-004"
        });
      })
    );

    console.log("PASS 8: Other veterinarian payment denied");

    // TEST 9: A partially paid bill cannot be marked fully paid.
    // This is a valid transaction structure with an invalid status.
    const prematurePaymentRef = doc(
      vetDb,
      "bills",
      BILL_ID,
      "payments",
      "payment-005"
    );

    await assertFails(
      runTransaction(vetDb, async (transaction) => {
        const snapshot = await transaction.get(billRef);

        transaction.set(prematurePaymentRef, {
          id: "payment-005",
          billId: BILL_ID,
          vetId: VET_UID,
          amount: 100.0,
          method: "Cash",
          reference: "",
          createdAt: 7000
        });

        transaction.update(billRef, {
          amountPaid: snapshot.data().amountPaid + 100.0,
          status: "paid",
          paymentMethod: "Cash",
          updatedAt: 7000,
          lastPaymentId: "payment-005"
        });
      })
    );

    console.log("PASS 9: Premature paid status denied");

    console.log("\nALL PAYMENT SECURITY TESTS PASSED");
  } finally {
    // Release the Firebase test connections.
    await env.cleanup();
  }
}

main().catch((error) => {
  console.error("\nTEST FAILED:");
  console.error(error.message);
  process.exitCode = 1;
});
