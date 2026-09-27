import type { GroupBalance, GroupMember } from '@/types'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Cell,
  ReferenceLine,
} from 'recharts'
import { BarChart2 } from 'lucide-react'
import clsx from 'clsx'

interface Props {
  balance: GroupBalance | undefined
  isLoading: boolean
  members: GroupMember[]
}

export default function BalancePanel({ balance, isLoading, members }: Props) {
  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="skeleton h-48" />
        <div className="skeleton h-32" />
      </div>
    )
  }

  if (!balance) return null

  const memberMap = Object.fromEntries(members.map((m) => [m.userId, m]))

  const chartData = Object.entries(balance.memberBalances).map(([uid, bal]) => ({
    name: balance.memberDisplayNames[Number(uid)] ?? `User ${uid}`,
    balance: parseFloat(Number(bal).toFixed(2)),
  }))

  return (
    <div className="space-y-5">
      {/* Chart */}
      <div className="card p-6">
        <h3 className="text-sm font-semibold text-white mb-4 flex items-center gap-2">
          <BarChart2 className="w-4 h-4 text-brand-400" />
          Net Balances
        </h3>
        {chartData.length === 0 ? (
          <div className="h-48 flex items-center justify-center text-slate-500 text-sm">
            No expenses recorded yet.
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={180}>
            <BarChart data={chartData} barCategoryGap="40%">
              <XAxis dataKey="name" tick={{ fontSize: 11 }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11 }} axisLine={false} tickLine={false} />
              <Tooltip
                formatter={(v: number) => [`$${v.toFixed(2)}`, 'Net Balance']}
                cursor={{ fill: 'rgba(99,102,241,0.08)' }}
              />
              <ReferenceLine y={0} stroke="rgba(148,163,184,0.3)" />
              <Bar dataKey="balance" radius={[5, 5, 0, 0]}>
                {chartData.map((entry, i) => (
                  <Cell
                    key={i}
                    fill={entry.balance >= 0 ? 'rgba(34,197,94,0.75)' : 'rgba(239,68,68,0.75)'}
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>

      {/* Member balance list */}
      <div className="card divide-y divide-surface-700/50">
        {Object.entries(balance.memberBalances).length === 0 ? (
          <div className="p-8 text-center text-slate-500 text-sm">All settled up!</div>
        ) : (
          Object.entries(balance.memberBalances).map(([uid, bal]) => {
            const numBal = Number(bal)
            const member = memberMap[Number(uid)]
            return (
              <div key={uid} className="flex items-center justify-between p-4">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-full bg-brand-900/50 border border-brand-800/40 flex items-center justify-center text-xs font-bold text-brand-400">
                    {(balance.memberDisplayNames[Number(uid)] ?? '?')[0].toUpperCase()}
                  </div>
                  <div>
                    <p className="text-sm font-medium text-slate-200">
                      {balance.memberDisplayNames[Number(uid)] ?? `User ${uid}`}
                    </p>
                    <p className="text-xs text-slate-500">{member?.role ?? ''}</p>
                  </div>
                </div>
                <div className="text-right">
                  <p
                    className={clsx(
                      'text-sm font-bold tabular-nums',
                      numBal > 0 ? 'amount-positive' : numBal < 0 ? 'amount-negative' : 'amount-neutral',
                    )}
                  >
                    {numBal > 0 ? '+' : ''}{numBal.toFixed(2)}
                  </p>
                  <p className="text-xs text-slate-500">
                    {numBal > 0 ? 'gets back' : numBal < 0 ? 'owes' : 'settled'}
                  </p>
                </div>
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}
