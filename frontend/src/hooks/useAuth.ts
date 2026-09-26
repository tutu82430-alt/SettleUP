import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import api from '@/lib/api'
import { useAuthStore } from '@/store/authStore'
import type { AuthResponse, ApiError } from '@/types'
import axios from 'axios'

// ─── Login ──────────────────────────────────────────────────────────────────

export function useLogin() {
  const setAuth = useAuthStore((s) => s.setAuth)

  return useMutation({
    mutationFn: (data: { email: string; password: string }) =>
      api.post<AuthResponse>('/auth/login', data).then((r) => r.data),
    onSuccess: (data) => {
      setAuth(data.token, data.user)
      toast.success(`Welcome back, ${data.user.displayName}!`)
    },
    onError: (err) => {
      if (axios.isAxiosError(err)) {
        const apiErr = err.response?.data as ApiError
        toast.error(apiErr?.message || 'Login failed. Check your credentials.')
      }
    },
  })
}

// ─── Register ───────────────────────────────────────────────────────────────

export function useRegister() {
  const setAuth = useAuthStore((s) => s.setAuth)

  return useMutation({
    mutationFn: (data: { username: string; email: string; password: string; displayName?: string }) =>
      api.post<AuthResponse>('/auth/register', data).then((r) => r.data),
    onSuccess: (data) => {
      setAuth(data.token, data.user)
      toast.success('Account created! Welcome to SettleUp 🎉')
    },
    onError: (err) => {
      if (axios.isAxiosError(err)) {
        const apiErr = err.response?.data as ApiError
        toast.error(apiErr?.message || 'Registration failed.')
      }
    },
  })
}
