import { useForm } from 'react-hook-form'
import { useAddExpense } from '@/hooks/useExpenses'
import type { GroupMember, SplitType } from '@/types'
import { X, Loader2, DollarSign } from 'lucide-react'

interface Props {
  groupId: number
  members: GroupMember[]
  currentUserId: number
  onClose: () => void
}

interface ExpenseFormData {
  description: string
  amount: string
  splitType: SplitType
  category: string
  expenseDate: string
  participantIds: number[]
  percentageSplits: Record<string, string>
  exactSplits: Record<string, string>
}

const CATEGORIES = [
  'Food & Drink', 'Transport', 'Accommodation', 'Entertainment',
  'Shopping', 'Utilities', 'Healthcare', 'Other',
]

export default function AddExpenseModal({ groupId, members, currentUserId, onClose }: Props) {
  const addExpense = useAddExpense(groupId)
  const { register, handleSubmit, watch, formState: { errors } } = useForm<ExpenseFormData>({
    defaultValues: {
      splitType: 'EQUAL',
      expenseDate: new Date().toISOString().split('T')[0],
      participantIds: members.map((m) => m.userId),
    },
  })

  const splitType = watch('splitType')
  const participantIds = watch('participantIds') ?? []

  const onSubmit = async (data: ExpenseFormData) => {
    const payload: Parameters<typeof addExpense.mutate>[0] = {
      description: data.description,
      amount: parseFloat(data.amount),
      splitType: data.splitType,
      category: data.category || undefined,
      expenseDate: data.expenseDate,
      participantIds: data.participantIds.map(Number),
    }

    if (data.splitType === 'PERCENTAGE') {
      payload.percentageSplits = Object.fromEntries(
        Object.entries(data.percentageSplits).map(([k, v]) => [Number(k), parseFloat(v)]),
      )
    } else if (data.splitType === 'EXACT') {
      payload.exactSplits = Object.fromEntries(
        Object.entries(data.exactSplits).map(([k, v]) => [Number(k), parseFloat(v)]),
      )
    }

    try {
      await addExpense.mutateAsync(payload)
      onClose()
    } catch {
      // handled in hook
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="relative card w-full max-w-lg max-h-[90vh] overflow-y-auto p-6 z-10 animate-slide-up">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-bold text-white">Add Expense</h2>
          <button onClick={onClose} className="btn-icon text-slate-500 hover:text-slate-200">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} id="add-expense-form" className="space-y-4">
          {/* Description */}
          <div>
            <label className="label" htmlFor="exp-description">Description *</label>
            <input
              id="exp-description"
              className={`input ${errors.description ? 'input-error' : ''}`}
              placeholder="Dinner at restaurant"
              {...register('description', { required: 'Description is required' })}
            />
            {errors.description && <p className="mt-1 text-xs text-danger-400">{errors.description.message}</p>}
          </div>

          {/* Amount + Date */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label" htmlFor="exp-amount">Amount *</label>
              <div className="relative">
                <DollarSign className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                <input
                  id="exp-amount"
                  type="number"
                  step="0.01"
                  min="0.01"
                  className={`input pl-9 ${errors.amount ? 'input-error' : ''}`}
                  placeholder="0.00"
                  {...register('amount', { required: 'Amount required', min: { value: 0.01, message: 'Must be > 0' } })}
                />
              </div>
              {errors.amount && <p className="mt-1 text-xs text-danger-400">{errors.amount.message}</p>}
            </div>
            <div>
              <label className="label" htmlFor="exp-date">Date *</label>
              <input
                id="exp-date"
                type="date"
                className="input"
                {...register('expenseDate', { required: true })}
              />
            </div>
          </div>

          {/* Category + Split Type */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label" htmlFor="exp-category">Category</label>
              <select id="exp-category" className="input" {...register('category')}>
                <option value="">— None —</option>
                {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
            <div>
              <label className="label" htmlFor="exp-split-type">Split Type *</label>
              <select id="exp-split-type" className="input" {...register('splitType')}>
                <option value="EQUAL">Equal</option>
                <option value="PERCENTAGE">Percentage</option>
                <option value="EXACT">Exact Amount</option>
              </select>
            </div>
          </div>

          {/* Participants */}
          <div>
            <label className="label">Participants *</label>
            <div className="space-y-2">
              {members.map((m) => (
                <label key={m.userId} className="flex items-center gap-3 p-2 rounded-lg hover:bg-surface-700/30 cursor-pointer">
                  <input
                    type="checkbox"
                    value={m.userId}
                    defaultChecked
                    className="w-4 h-4 rounded accent-brand-500"
                    {...register('participantIds', {
                      validate: (val) => (val && val.length > 0) || 'Select at least one participant',
                    })}
                  />
                  <span className="w-7 h-7 rounded-full bg-brand-900/50 flex items-center justify-center text-xs font-bold text-brand-400">
                    {m.displayName?.[0]?.toUpperCase() ?? '?'}
                  </span>
                  <span className="text-sm text-slate-200">{m.displayName}</span>
                  {m.userId === currentUserId && <span className="badge-blue text-[10px]">You</span>}
                </label>
              ))}
            </div>
            {errors.participantIds && (
              <p className="mt-1 text-xs text-danger-400">{errors.participantIds.message}</p>
            )}
          </div>

          {/* Dynamic split inputs */}
          {splitType === 'PERCENTAGE' && (
            <div>
              <label className="label">Percentages (must sum to 100)</label>
              <div className="space-y-2">
                {members
                  .filter((m) => participantIds.includes(m.userId))
                  .map((m) => (
                    <div key={m.userId} className="flex items-center gap-3">
                      <span className="text-sm text-slate-300 w-24 truncate">{m.displayName}</span>
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        max="100"
                        className="input w-24 text-right"
                        placeholder="0"
                        {...register(`percentageSplits.${m.userId}` as const)}
                      />
                      <span className="text-slate-500 text-sm">%</span>
                    </div>
                  ))}
              </div>
            </div>
          )}

          {splitType === 'EXACT' && (
            <div>
              <label className="label">Exact Amounts (must sum to total)</label>
              <div className="space-y-2">
                {members
                  .filter((m) => participantIds.includes(m.userId))
                  .map((m) => (
                    <div key={m.userId} className="flex items-center gap-3">
                      <span className="text-sm text-slate-300 w-24 truncate">{m.displayName}</span>
                      <div className="relative">
                        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500">$</span>
                        <input
                          type="number"
                          step="0.01"
                          min="0"
                          className="input pl-7 w-28 text-right"
                          placeholder="0.00"
                          {...register(`exactSplits.${m.userId}` as const)}
                        />
                      </div>
                    </div>
                  ))}
              </div>
            </div>
          )}

          {/* Submit */}
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary flex-1">
              Cancel
            </button>
            <button
              id="add-expense-submit"
              type="submit"
              disabled={addExpense.isPending}
              className="btn-primary flex-1"
            >
              {addExpense.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
              {addExpense.isPending ? 'Adding…' : 'Add Expense'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
