import type { Expense } from '@/types'
import { useDeleteExpense } from '@/hooks/useExpenses'
import { Loader2, Receipt, Trash2, ChevronDown, ChevronUp } from 'lucide-react'
import { useState } from 'react'
import clsx from 'clsx'
import { format } from 'date-fns'

interface Props {
  expenses: Expense[]
  isLoading: boolean
  groupId: number
  currentUserId: number
}

const SPLIT_COLORS: Record<string, string> = {
  EQUAL: 'badge-blue',
  PERCENTAGE: 'badge-yellow',
  EXACT: 'badge-green',
}

export default function ExpenseList({ expenses, isLoading, groupId, currentUserId }: Props) {
  const deleteExpense = useDeleteExpense(groupId)
  const [expanded, setExpanded] = useState<number | null>(null)

  if (isLoading) {
    return (
      <div className="space-y-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="skeleton h-20" />
        ))}
      </div>
    )
  }

  if (expenses.length === 0) {
    return (
      <div className="card p-12 text-center">
        <Receipt className="w-10 h-10 text-slate-600 mx-auto mb-3" />
        <h3 className="font-semibold text-slate-300">No expenses yet</h3>
        <p className="text-slate-500 text-sm mt-1">Add the first expense to get started!</p>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {expenses.map((exp) => {
        const isExpanded = expanded === exp.id
        const userSplit = exp.splits.find((s) => s.userId === currentUserId)
        const isPayer = exp.paidBy.id === currentUserId

        return (
          <div key={exp.id} className="card overflow-hidden">
            <div
              className="p-4 flex items-center gap-4 cursor-pointer hover:bg-surface-700/20 transition-colors"
              onClick={() => setExpanded(isExpanded ? null : exp.id)}
            >
              {/* Icon */}
              <div className="w-10 h-10 rounded-xl bg-brand-900/40 border border-brand-800/40 flex items-center justify-center flex-shrink-0">
                <Receipt className="w-5 h-5 text-brand-400" />
              </div>

              {/* Info */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <p className="text-sm font-semibold text-white truncate">{exp.description}</p>
                  <span className={SPLIT_COLORS[exp.splitType] || 'badge-blue'}>
                    {exp.splitType}
                  </span>
                  {exp.category && (
                    <span className="text-xs text-slate-500">{exp.category}</span>
                  )}
                </div>
                <p className="text-xs text-slate-500 mt-0.5">
                  Paid by{' '}
                  <span className={isPayer ? 'text-brand-400' : 'text-slate-400'}>
                    {isPayer ? 'you' : exp.paidBy.displayName}
                  </span>{' '}
                  · {format(new Date(exp.expenseDate), 'MMM d, yyyy')}
                </p>
              </div>

              {/* Amounts */}
              <div className="text-right flex-shrink-0">
                <p className="text-base font-bold text-white">${exp.amount.toFixed(2)}</p>
                {userSplit && (
                  <p
                    className={clsx(
                      'text-xs',
                      isPayer ? 'text-success-400' : 'text-danger-400',
                    )}
                  >
                    {isPayer ? 'you paid' : `you owe $${userSplit.amount.toFixed(2)}`}
                  </p>
                )}
              </div>

              {/* Expand + Delete */}
              <div className="flex items-center gap-1 flex-shrink-0">
                {exp.paidBy.id === currentUserId && (
                  <button
                    className="btn-icon text-slate-500 hover:text-danger-400"
                    onClick={(e) => {
                      e.stopPropagation()
                      if (confirm('Delete this expense?')) {
                        deleteExpense.mutate(exp.id)
                      }
                    }}
                    disabled={deleteExpense.isPending}
                    title="Delete expense"
                  >
                    {deleteExpense.isPending ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <Trash2 className="w-4 h-4" />
                    )}
                  </button>
                )}
                {isExpanded ? (
                  <ChevronUp className="w-4 h-4 text-slate-500" />
                ) : (
                  <ChevronDown className="w-4 h-4 text-slate-500" />
                )}
              </div>
            </div>

            {/* Expanded split details */}
            {isExpanded && (
              <div className="border-t border-surface-700/50 px-4 py-3 bg-surface-900/30">
                <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 mb-2">
                  Split Details
                </p>
                <div className="space-y-1.5">
                  {exp.splits.map((s) => (
                    <div key={s.userId} className="flex items-center justify-between text-sm">
                      <span className="text-slate-300">{s.displayName}</span>
                      <div className="flex items-center gap-2">
                        {s.percentage != null && (
                          <span className="text-xs text-slate-500">{s.percentage}%</span>
                        )}
                        <span className="font-medium text-white tabular-nums">
                          ${s.amount.toFixed(2)}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}
