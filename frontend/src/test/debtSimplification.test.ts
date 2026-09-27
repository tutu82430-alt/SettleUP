import { describe, it, expect } from 'vitest'

/**
 * Tests for the debt simplification algorithm logic (TypeScript port for frontend).
 * These mirror the Java backend tests to ensure consistent behavior.
 */

interface Balance {
  userId: number
  balance: number
}

interface Transaction {
  fromUserId: number
  toUserId: number
  amount: number
}

/**
 * Computes minimum transactions using a greedy priority queue approach.
 * Mirrors the Java DebtSimplificationService algorithm.
 */
function computeMinimumTransactions(balances: Balance[]): Transaction[] {
  const creditors = balances.filter((b) => b.balance > 0.005).sort((a, b) => b.balance - a.balance)
  const debtors = balances.filter((b) => b.balance < -0.005).sort((a, b) => a.balance - b.balance)

  const credits = creditors.map((c) => ({ userId: c.userId, amount: c.balance }))
  const debts = debtors.map((d) => ({ userId: d.userId, amount: -d.balance }))

  const transactions: Transaction[] = []

  let ci = 0
  let di = 0

  while (ci < credits.length && di < debts.length) {
    const credit = credits[ci]
    const debt = debts[di]

    const settled = Math.min(credit.amount, debt.amount)
    const rounded = Math.round(settled * 100) / 100

    if (rounded > 0.005) {
      transactions.push({
        fromUserId: debt.userId,
        toUserId: credit.userId,
        amount: rounded,
      })
    }

    credit.amount -= settled
    debt.amount -= settled

    if (credit.amount < 0.005) ci++
    if (debt.amount < 0.005) di++
  }

  return transactions
}

describe('Debt Simplification Algorithm (Frontend)', () => {
  it('(a) returns empty list when group is already settled', () => {
    const txs = computeMinimumTransactions([
      { userId: 1, balance: 0 },
      { userId: 2, balance: 0 },
    ])
    expect(txs).toHaveLength(0)
  })

  it('(b) returns one transaction for single debtor/creditor pair', () => {
    const txs = computeMinimumTransactions([
      { userId: 1, balance: 25 },   // Alice is owed $25
      { userId: 2, balance: -25 },  // Bob owes $25
    ])
    expect(txs).toHaveLength(1)
    expect(txs[0].fromUserId).toBe(2)
    expect(txs[0].toUserId).toBe(1)
    expect(txs[0].amount).toBe(25)
  })

  it('(c) returns 2 transactions for multiple debtors and creditors (3-person scenario)', () => {
    // Alice is owed $60, Bob and Charlie each owe $30
    const txs = computeMinimumTransactions([
      { userId: 1, balance: 60 },  // Alice
      { userId: 2, balance: -30 }, // Bob
      { userId: 3, balance: -30 }, // Charlie
    ])
    expect(txs).toHaveLength(2)
    const total = txs.reduce((sum, t) => sum + t.amount, 0)
    expect(total).toBe(60)
  })

  it("(d) handles non-even floating point split (splitting ₹100 three ways)", () => {
    // ₹100 / 3 scenario: Alice +66.66, Bob -33.33, Charlie -33.33
    const txs = computeMinimumTransactions([
      { userId: 1, balance: 66.66 },  // Alice (creditor)
      { userId: 2, balance: -33.33 }, // Bob (debtor)
      { userId: 3, balance: -33.33 }, // Charlie (debtor)
    ])
    expect(txs).toHaveLength(2)
    const totalSettled = txs.reduce((sum, t) => sum + t.amount, 0)
    expect(Math.abs(totalSettled - 66.66)).toBeLessThan(0.01)
  })

  it('(e) handles member who paid for an expense but is also owed elsewhere', () => {
    // Net balances after multiple overlapping expenses:
    // Alice: +15, Bob: +15, Charlie: -5, Dave: -25
    const txs = computeMinimumTransactions([
      { userId: 1, balance: 15 },
      { userId: 2, balance: 15 },
      { userId: 3, balance: -5 },
      { userId: 4, balance: -25 },
    ])
    expect(txs.length).toBeLessThanOrEqual(3)
    const totalSettled = txs.reduce((sum, t) => sum + t.amount, 0)
    expect(totalSettled).toBe(30)
  })

  it('produces at most N-1 transactions for N people', () => {
    const txs = computeMinimumTransactions([
      { userId: 1, balance: 100 },
      { userId: 2, balance: -30 },
      { userId: 3, balance: -40 },
      { userId: 4, balance: -30 },
    ])
    // 4 people, so at most 3 transactions
    expect(txs.length).toBeLessThanOrEqual(3)
  })
})
