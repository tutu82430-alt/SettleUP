import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useGroup } from '@/hooks/useGroups'
import { useGroupBalance, useExpenses, useRecordSettlement } from '@/hooks/useExpenses'
import { useGroupWebSocket } from '@/hooks/useWebSocket'
import { useAuthStore } from '@/store/authStore'
import AddExpenseModal from '@/components/group/AddExpenseModal'
import ExpenseList from '@/components/group/ExpenseList'
import BalancePanel from '@/components/group/BalancePanel'
import {
  Users,
  Copy,
  PlusCircle,
  Loader2,
  BarChart2,
  Receipt,
  ArrowLeftRight,
} from 'lucide-react'
import toast from 'react-hot-toast'
import clsx from 'clsx'

type Tab = 'expenses' | 'balances' | 'settle'

export default function GroupDetailPage() {
  const { groupId } = useParams<{ groupId: string }>()
  const gid = Number(groupId)
  const { user } = useAuthStore()
  const [tab, setTab] = useState<Tab>('expenses')
  const [showAddExpense, setShowAddExpense] = useState(false)

  const { data: group, isLoading: groupLoading } = useGroup(gid)
  const { data: balance, isLoading: balanceLoading } = useGroupBalance(gid)
  const { data: expenses, isLoading: expensesLoading } = useExpenses(gid)

  // Real-time WebSocket subscription
  useGroupWebSocket(gid)

  const copyInviteCode = () => {
    if (group?.inviteCode) {
      navigator.clipboard.writeText(group.inviteCode)
      toast.success('Invite code copied!')
    }
  }

  if (groupLoading) {
    return (
      <div className="p-8 flex items-center justify-center min-h-[60vh]">
        <Loader2 className="w-8 h-8 animate-spin text-brand-400" />
      </div>
    )
  }

  if (!group) {
    return (
      <div className="p-8 text-center">
        <p className="text-slate-400">Group not found.</p>
      </div>
    )
  }

  const userBalance = user ? (balance?.memberBalances?.[user.id] ?? 0) : 0

  const TABS: { key: Tab; label: string; icon: React.ElementType }[] = [
    { key: 'expenses', label: 'Expenses', icon: Receipt },
    { key: 'balances', label: 'Balances', icon: BarChart2 },
    { key: 'settle', label: 'Settle Up', icon: ArrowLeftRight },
  ]

  return (
    <div className="p-6 md:p-8 max-w-5xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">{group.name}</h1>
          {group.description && (
            <p className="text-slate-400 mt-1 text-sm">{group.description}</p>
          )}
          <div className="flex items-center gap-3 mt-2 flex-wrap">
            <span className="badge-blue">
              <Users className="w-3 h-3" />
              {group.memberCount} members
            </span>
            <span className="badge-blue">{group.currency}</span>
            <button
              onClick={copyInviteCode}
              className="badge bg-surface-700/50 text-slate-400 hover:text-slate-200 border border-surface-600/50 cursor-pointer transition-colors font-mono"
            >
              <Copy className="w-3 h-3" />
              {group.inviteCode}
            </button>
          </div>
        </div>

        {/* Balance pill */}
        <div className="flex items-center gap-3">
          <div
            className={clsx(
              'card px-4 py-2 text-center min-w-[120px]',
              userBalance > 0
                ? 'border-success-600/40 bg-success-600/10'
                : userBalance < 0
                ? 'border-danger-600/40 bg-danger-600/10'
                : '',
            )}
          >
            <p className="text-xs text-slate-500 uppercase tracking-wide">Your Balance</p>
            <p
              className={clsx(
                'text-xl font-bold tabular-nums',
                userBalance > 0 ? 'text-success-400' : userBalance < 0 ? 'text-danger-400' : 'text-slate-400',
              )}
            >
              {userBalance > 0 ? '+' : ''}{userBalance.toFixed(2)}
            </p>
          </div>
          <button
            id="add-expense-btn"
            onClick={() => setShowAddExpense(true)}
            className="btn-primary"
          >
            <PlusCircle className="w-4 h-4" />
            Add Expense
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-surface-800/50 rounded-xl p-1 border border-surface-700/50">
        {TABS.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={clsx(
              'flex-1 flex items-center justify-center gap-2 py-2 px-3 rounded-lg text-sm font-medium transition-all duration-200',
              tab === key
                ? 'bg-brand-600 text-white shadow-md'
                : 'text-slate-400 hover:text-slate-200 hover:bg-surface-700/50',
            )}
          >
            <Icon className="w-4 h-4" />
            {label}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      {tab === 'expenses' && (
        <ExpenseList
          expenses={expenses ?? []}
          isLoading={expensesLoading}
          groupId={gid}
          currentUserId={user?.id ?? 0}
        />
      )}
      {tab === 'balances' && (
        <BalancePanel
          balance={balance}
          isLoading={balanceLoading}
          members={group.members}
        />
      )}
      {tab === 'settle' && (
        <SettleUpPanel
          balance={balance}
          groupId={gid}
          currentUserId={user?.id ?? 0}
        />
      )}

      {/* Add Expense Modal */}
      {showAddExpense && (
        <AddExpenseModal
          groupId={gid}
          members={group.members}
          currentUserId={user?.id ?? 0}
          onClose={() => setShowAddExpense(false)}
        />
      )}
    </div>
  )
}

// ─── Settle Up Panel ────────────────────────────────────────────────────────

function SettleUpPanel({
  balance,
  groupId,
  currentUserId,
}: {
  balance?: import('@/types').GroupBalance
  groupId: number
  currentUserId: number
}) {
  const recordSettlement = useRecordSettlement(groupId)

  if (!balance) {
    return (
      <div className="card p-8 text-center text-slate-400">
        <ArrowLeftRight className="w-10 h-10 mx-auto mb-3 opacity-30" />
        <p>Loading settlement data…</p>
      </div>
    )
  }

  if (balance.suggestedSettlements.length === 0) {
    return (
      <div className="card p-12 text-center">
        <div className="w-14 h-14 rounded-full bg-success-600/20 flex items-center justify-center mx-auto mb-4">
          <ArrowLeftRight className="w-7 h-7 text-success-400" />
        </div>
        <h3 className="text-lg font-semibold text-white mb-1">All Settled Up! 🎉</h3>
        <p className="text-slate-400 text-sm">No outstanding debts in this group.</p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="card p-4 border-brand-800/40 bg-brand-900/10">
        <p className="text-xs text-brand-300">
          <strong>Minimum transactions:</strong> These {balance.suggestedSettlements.length} payment(s)
          will settle all debts in this group optimally.
        </p>
      </div>
      {balance.suggestedSettlements.map((tx, i) => (
        <div key={i} className="card p-5 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="flex flex-col items-center gap-1">
              <div className="w-8 h-8 rounded-full bg-danger-600/20 border border-danger-600/30 flex items-center justify-center text-xs font-bold text-danger-400">
                {tx.fromUserName?.[0]?.toUpperCase() ?? '?'}
              </div>
              <p className="text-xs text-slate-400">{tx.fromUserName}</p>
            </div>
            <div className="flex flex-col items-center">
              <ArrowLeftRight className="w-5 h-5 text-brand-400 my-1" />
              <p className="text-base font-bold text-white tabular-nums">
                ${tx.amount.toFixed(2)}
              </p>
            </div>
            <div className="flex flex-col items-center gap-1">
              <div className="w-8 h-8 rounded-full bg-success-600/20 border border-success-600/30 flex items-center justify-center text-xs font-bold text-success-400">
                {tx.toUserName?.[0]?.toUpperCase() ?? '?'}
              </div>
              <p className="text-xs text-slate-400">{tx.toUserName}</p>
            </div>
          </div>
          {tx.fromUserId === currentUserId && (
            <button
              className="btn-primary"
              onClick={() =>
                recordSettlement.mutate({
                  toUserId: tx.toUserId,
                  amount: tx.amount,
                })
              }
              disabled={recordSettlement.isPending}
            >
              Mark as Paid
            </button>
          )}
        </div>
      ))}
    </div>
  )
}
