import { Link } from 'react-router-dom'
import {
  BarChart2,
  TrendingUp,
  TrendingDown,
  Users,
  PlusCircle,
  ArrowRight,
  Loader2,
} from 'lucide-react'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts'
import { useDashboard } from '@/hooks/useGroups'
import { useAuthStore } from '@/store/authStore'
import clsx from 'clsx'

function StatCard({
  label,
  value,
  icon: Icon,
  variant = 'default',
}: {
  label: string
  value: string
  icon: React.ElementType
  variant?: 'default' | 'green' | 'red'
}) {
  return (
    <div
      className={clsx(
        'card p-5 flex items-center gap-4',
        variant === 'green' && 'border-success-600/30',
        variant === 'red' && 'border-danger-600/30',
      )}
    >
      <div
        className={clsx(
          'w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0',
          variant === 'green' && 'bg-success-600/20',
          variant === 'red' && 'bg-danger-600/20',
          variant === 'default' && 'bg-brand-600/20',
        )}
      >
        <Icon
          className={clsx(
            'w-5 h-5',
            variant === 'green' && 'text-success-400',
            variant === 'red' && 'text-danger-400',
            variant === 'default' && 'text-brand-400',
          )}
        />
      </div>
      <div>
        <p className="text-xs text-slate-500 font-medium uppercase tracking-wider">{label}</p>
        <p className="text-2xl font-bold text-white tabular-nums">{value}</p>
      </div>
    </div>
  )
}

export default function DashboardPage() {
  const { user } = useAuthStore()
  const { data: dashboard, isLoading } = useDashboard()

  const chartData =
    dashboard?.groupSummaries.map((g) => ({
      name: g.groupName.length > 10 ? g.groupName.slice(0, 10) + '…' : g.groupName,
      balance: parseFloat(g.userNetBalance.toFixed(2)),
    })) ?? []

  if (isLoading) {
    return (
      <div className="p-8 flex items-center justify-center min-h-[60vh]">
        <Loader2 className="w-8 h-8 animate-spin text-brand-400" />
      </div>
    )
  }

  const fmt = (n: number) => `$${Math.abs(n).toFixed(2)}`

  return (
    <div className="p-6 md:p-8 max-w-6xl mx-auto space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">
            Hey, {user?.displayName?.split(' ')[0]} 👋
          </h1>
          <p className="text-slate-400 mt-0.5">Here's your expense overview</p>
        </div>
        <div className="flex gap-2">
          <Link to="/groups/join" className="btn-secondary">
            Join Group
          </Link>
          <Link to="/groups/create" className="btn-primary">
            <PlusCircle className="w-4 h-4" />
            New Group
          </Link>
        </div>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard
          label="Net Balance"
          value={`${(dashboard?.netBalance ?? 0) >= 0 ? '+' : '-'}${fmt(dashboard?.netBalance ?? 0)}`}
          icon={BarChart2}
          variant={(dashboard?.netBalance ?? 0) >= 0 ? 'green' : 'red'}
        />
        <StatCard
          label="You Are Owed"
          value={fmt(dashboard?.totalOwedToYou ?? 0)}
          icon={TrendingUp}
          variant="green"
        />
        <StatCard
          label="You Owe"
          value={fmt(dashboard?.totalOwed ?? 0)}
          icon={TrendingDown}
          variant="red"
        />
      </div>

      {/* Chart + Groups */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        {/* Chart */}
        <div className="card p-6 lg:col-span-3">
          <h2 className="text-base font-semibold text-white mb-4 flex items-center gap-2">
            <BarChart2 className="w-4 h-4 text-brand-400" />
            Balance by Group
          </h2>
          {chartData.length === 0 ? (
            <div className="h-48 flex items-center justify-center text-slate-500">
              <p>No groups yet. Create one to get started!</p>
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={chartData} barCategoryGap="30%">
                <XAxis
                  dataKey="name"
                  tick={{ fontSize: 11 }}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis tick={{ fontSize: 11 }} axisLine={false} tickLine={false} />
                <Tooltip
                  formatter={(v: number) => [`$${v.toFixed(2)}`, 'Balance']}
                  cursor={{ fill: 'rgba(99,102,241,0.1)' }}
                />
                <Bar dataKey="balance" radius={[6, 6, 0, 0]}>
                  {chartData.map((entry, i) => (
                    <Cell
                      key={i}
                      fill={
                        entry.balance >= 0
                          ? 'rgba(34,197,94,0.7)'
                          : 'rgba(239,68,68,0.7)'
                      }
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Groups List */}
        <div className="card p-6 lg:col-span-2">
          <h2 className="text-base font-semibold text-white mb-4 flex items-center gap-2">
            <Users className="w-4 h-4 text-brand-400" />
            My Groups
          </h2>
          <div className="space-y-2">
            {dashboard?.groupSummaries.length === 0 && (
              <p className="text-slate-500 text-sm py-8 text-center">
                No groups yet.
              </p>
            )}
            {dashboard?.groupSummaries.map((g) => (
              <Link
                key={g.groupId}
                to={`/groups/${g.groupId}`}
                className="flex items-center justify-between p-3 rounded-xl hover:bg-surface-700/50 transition-colors group"
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-brand-900/50 flex items-center justify-center text-brand-400 text-xs font-bold border border-brand-800/50">
                    {g.groupName[0].toUpperCase()}
                  </div>
                  <div>
                    <p className="text-sm font-medium text-slate-200">{g.groupName}</p>
                    <p className="text-xs text-slate-500">{g.memberCount} members · {g.currency}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <span
                    className={clsx(
                      'text-sm font-semibold tabular-nums',
                      g.userNetBalance > 0
                        ? 'amount-positive'
                        : g.userNetBalance < 0
                        ? 'amount-negative'
                        : 'amount-neutral',
                    )}
                  >
                    {g.userNetBalance > 0 ? '+' : ''}
                    {g.userNetBalance.toFixed(2)}
                  </span>
                  <ArrowRight className="w-3.5 h-3.5 text-slate-600 group-hover:text-slate-400 transition-colors" />
                </div>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
