// ─── Auth ───────────────────────────────────────────────────────────────────

export interface User {
  id: number
  username: string
  email: string
  displayName: string
  avatarUrl: string | null
  createdAt: string
}

export interface AuthResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: User
}

// ─── Groups ─────────────────────────────────────────────────────────────────

export interface GroupMember {
  userId: number
  username: string
  displayName: string
  avatarUrl: string | null
  role: 'ADMIN' | 'MEMBER'
  joinedAt: string
}

export interface Group {
  id: number
  name: string
  description: string | null
  inviteCode: string
  currency: string
  createdBy: User
  members: GroupMember[]
  createdAt: string
  memberCount: number
}

export interface GroupSummary {
  groupId: number
  groupName: string
  currency: string
  userNetBalance: number
  memberCount: number
}

// ─── Expenses ───────────────────────────────────────────────────────────────

export type SplitType = 'EQUAL' | 'PERCENTAGE' | 'EXACT'

export interface Split {
  userId: number
  username: string
  displayName: string
  amount: number
  percentage: number | null
}

export interface Expense {
  id: number
  groupId: number
  paidBy: User
  description: string
  amount: number
  splitType: SplitType
  category: string | null
  expenseDate: string
  createdAt: string
  splits: Split[]
}

export interface ExpenseRequest {
  description: string
  amount: number
  splitType: SplitType
  category?: string
  expenseDate: string
  participantIds: number[]
  percentageSplits?: Record<number, number>
  exactSplits?: Record<number, number>
}

// ─── Balances & Settlements ─────────────────────────────────────────────────

export interface SettlementTransaction {
  fromUserId: number
  toUserId: number
  amount: number
  fromUserName: string
  toUserName: string
}

export interface GroupBalance {
  groupId: number
  groupName: string
  memberBalances: Record<number, number>
  memberDisplayNames: Record<number, string>
  suggestedSettlements: SettlementTransaction[]
}

export interface Settlement {
  id: number
  groupId: number
  fromUser: User
  toUser: User
  amount: number
  note: string | null
  status: 'PENDING' | 'COMPLETED'
  createdAt: string
  settledAt: string | null
}

export interface SettlementRequest {
  toUserId: number
  amount: number
  note?: string
}

// ─── Dashboard ──────────────────────────────────────────────────────────────

export interface Dashboard {
  userId: number
  totalOwed: number
  totalOwedToYou: number
  netBalance: number
  groupSummaries: GroupSummary[]
}

// ─── API Errors ─────────────────────────────────────────────────────────────

export interface ApiError {
  status: number
  error: string
  message: string
  path: string
  timestamp: string
  fieldErrors?: Record<string, string[]>
}
