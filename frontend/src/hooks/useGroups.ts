import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import api from '@/lib/api'
import type { Group, GroupSummary, Dashboard } from '@/types'

// ─── Dashboard ──────────────────────────────────────────────────────────────

export function useDashboard() {
  return useQuery({
    queryKey: ['dashboard'],
    queryFn: () => api.get<Dashboard>('/dashboard').then((r) => r.data),
  })
}

// ─── Groups ─────────────────────────────────────────────────────────────────

export function useMyGroups() {
  return useQuery({
    queryKey: ['groups'],
    queryFn: () => api.get<GroupSummary[]>('/groups').then((r) => r.data),
  })
}

export function useGroup(groupId: number) {
  return useQuery({
    queryKey: ['groups', groupId],
    queryFn: () => api.get<Group>(`/groups/${groupId}`).then((r) => r.data),
    enabled: !!groupId,
  })
}

export function useCreateGroup() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: { name: string; description?: string; currency?: string }) =>
      api.post<Group>('/groups', data).then((r) => r.data),
    onSuccess: (group) => {
      qc.invalidateQueries({ queryKey: ['groups'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      toast.success(`Group "${group.name}" created!`)
    },
    onError: () => toast.error('Failed to create group.'),
  })
}

export function useJoinGroup() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (inviteCode: string) =>
      api.post<Group>(`/groups/join/${inviteCode}`).then((r) => r.data),
    onSuccess: (group) => {
      qc.invalidateQueries({ queryKey: ['groups'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      toast.success(`Joined "${group.name}"!`)
    },
    onError: () => toast.error('Invalid invite code or already a member.'),
  })
}
