import { useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/authStore'

/**
 * Subscribes to /topic/groups/{groupId}/balances and invalidates
 * the balance query whenever a balance update is broadcast.
 */
export function useGroupWebSocket(groupId: number) {
  const qc = useQueryClient()
  const token = useAuthStore((s) => s.token)
  const clientRef = useRef<Client | null>(null)

  useEffect(() => {
    if (!groupId || !token) return

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/groups/${groupId}/balances`, () => {
          qc.invalidateQueries({ queryKey: ['balance', groupId] })
          qc.invalidateQueries({ queryKey: ['expenses', groupId] })
          qc.invalidateQueries({ queryKey: ['dashboard'] })
        })
      },
      onStompError: (frame) => {
        console.warn('WebSocket STOMP error:', frame)
      },
    })

    client.activate()
    clientRef.current = client

    return () => {
      client.deactivate()
    }
  }, [groupId, token, qc])

  return clientRef
}
