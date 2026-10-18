const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read expenses", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("expenses").get());
});

test("Unauthenticated user: cannot create expense", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("expenses").doc("exp1").set({
      userId: ALICE_UID,
      title: "Coffee",
      amount: 4.5,
      category: "Food & Dining",
      date: "2026-10-15",
      paymentMethod: "Credit Card",
      isRecurring: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create valid expense", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("expenses").doc("exp_101").set({
      userId: ALICE_UID,
      title: "Whole Foods Groceries",
      amount: 85.25,
      category: "Food & Dining",
      date: "2026-10-15",
      paymentMethod: "Credit Card",
      isRecurring: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot create expense for another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("expenses").doc("exp_102").set({
      userId: BOB_UID,
      title: "Unauthorized Grocery",
      amount: 50.0,
      category: "Food & Dining",
      date: "2026-10-15",
      paymentMethod: "Credit Card",
      isRecurring: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot read another user's expense", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("expenses").doc("bob_exp").set({
      userId: BOB_UID,
      title: "Bob Expense",
      amount: 100.0,
      category: "Utilities",
      date: "2026-10-10",
      paymentMethod: "Debit Card",
      isRecurring: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("expenses").doc("bob_exp").get());
});

test("Authenticated user: can query own expenses with userId filter", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("expenses").doc("alice_exp").set({
      userId: ALICE_UID,
      title: "Alice Expense",
      amount: 45.0,
      category: "Entertainment",
      date: "2026-10-12",
      paymentMethod: "Credit Card",
      isRecurring: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("expenses").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: query without userId filter fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("expenses").get());
});

test("Authenticated user: can create and update monthly budget", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("monthly_budgets").doc("budget_2026_10").set({
      userId: ALICE_UID,
      monthYear: "2026-10",
      monthlyIncome: 6000.0,
      savingsGoal: 1500.0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("monthly_budgets").doc("budget_2026_10").update({
      monthlyIncome: 6500.0,
      savingsGoal: 1800.0,
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create savings goal", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("savings_goals").doc("goal_1").set({
      userId: ALICE_UID,
      name: "Emergency Fund",
      targetAmount: 10000.0,
      currentAmount: 3500.0,
      targetDate: "2026-12-31",
      category: "Emergency",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});
