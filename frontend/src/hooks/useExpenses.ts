import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import api from '@/lib/api'
import type { Expense, ExpenseRequest, GroupBalance, Settlement, SettlementRequest } from '@/types'

// ─── Expenses ───────────────────────────────────────────────────────────────

export function useExpenses(
  groupId: number,
  filters?: { category?: string; fromDate?: string; toDate?: string; memberId?: number },
) {
  return useQuery({
    queryKey: ['expenses', groupId, filters],
    queryFn: () => {
      const params = new URLSearchParams()
      if (filters?.category) params.set('category', filters.category)
      if (filters?.fromDate) params.set('fromDate', filters.fromDate)
      if (filters?.toDate) params.set('toDate', filters.toDate)
      if (filters?.memberId) params.set('memberId', String(filters.memberId))
      return api
        .get<Expense[]>(`/groups/${groupId}/expenses?${params}`)
        .then((r) => r.data)
    },
    enabled: !!groupId,
  })
}

export function useAddExpense(groupId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: ExpenseRequest) =>
      api.post<Expense>(`/groups/${groupId}/expenses`, data).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['expenses', groupId] })
      qc.invalidateQueries({ queryKey: ['balance', groupId] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      toast.success('Expense added!')
    },
    onError: (err: unknown) => {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to add expense.'
      toast.error(message)
    },
  })
}

export function useDeleteExpense(groupId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (expenseId: number) =>
      api.delete(`/groups/${groupId}/expenses/${expenseId}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['expenses', groupId] })
      qc.invalidateQueries({ queryKey: ['balance', groupId] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      toast.success('Expense deleted.')
    },
    onError: () => toast.error('Failed to delete expense.'),
  })
}

// ─── Balance ─────────────────────────────────────────────────────────────────

export function useGroupBalance(groupId: number) {
  return useQuery({
    queryKey: ['balance', groupId],
    queryFn: () =>
      api.get<GroupBalance>(`/groups/${groupId}/balance`).then((r) => r.data),
    enabled: !!groupId,
  })
}

// ─── Settlements ─────────────────────────────────────────────────────────────

export function useRecordSettlement(groupId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: SettlementRequest) =>
      api
        .post<Settlement>(`/groups/${groupId}/settlements`, data)
        .then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['balance', groupId] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      toast.success('Settlement recorded! Waiting for confirmation.')
    },
    onError: () => toast.error('Failed to record settlement.'),
  })
}

export function useConfirmSettlement(groupId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (settlementId: number) =>
      api
        .patch<Settlement>(`/groups/${groupId}/settlements/${settlementId}/confirm`)
        .then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['balance', groupId] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      toast.success('Settlement confirmed! ✅')
    },
    onError: () => toast.error('Failed to confirm settlement.'),
  })
}
