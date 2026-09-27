import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { useMyGroups } from '@/hooks/useGroups'
import {
  LayoutDashboard,
  Users,
  PlusCircle,
  LogOut,
  ChevronRight,
  Wallet,
} from 'lucide-react'
import clsx from 'clsx'

export default function Layout() {
  const { user, logout } = useAuthStore()
  const navigate = useNavigate()
  const location = useLocation()
  const { data: groups } = useMyGroups()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const isActive = (path: string) =>
    location.pathname === path || location.pathname.startsWith(path + '/')

  return (
    <div className="flex h-screen overflow-hidden">
      {/* ── Sidebar ── */}
      <aside className="w-64 flex-shrink-0 bg-surface-900/80 backdrop-blur-sm border-r border-surface-700/50 flex flex-col">
        {/* Logo */}
        <div className="p-5 border-b border-surface-700/50">
          <Link to="/dashboard" className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-brand-gradient flex items-center justify-center shadow-glow-blue">
              <Wallet className="w-4 h-4 text-white" />
            </div>
            <span className="font-bold text-lg tracking-tight text-white">SettleUp</span>
          </Link>
        </div>

        {/* Nav */}
        <nav className="flex-1 overflow-y-auto p-3 space-y-1">
          <Link
            to="/dashboard"
            className={clsx(
              isActive('/dashboard') ? 'nav-link-active' : 'nav-link',
            )}
          >
            <LayoutDashboard className="w-4 h-4" />
            Dashboard
          </Link>

          {/* Groups */}
          <div className="pt-3 pb-1">
            <p className="label px-3 text-slate-500">My Groups</p>
          </div>

          {groups?.map((g) => (
            <Link
              key={g.groupId}
              to={`/groups/${g.groupId}`}
              className={clsx(
                isActive(`/groups/${g.groupId}`) ? 'nav-link-active' : 'nav-link',
                'justify-between',
              )}
            >
              <span className="flex items-center gap-2">
                <Users className="w-4 h-4 flex-shrink-0" />
                <span className="truncate max-w-[120px]">{g.groupName}</span>
              </span>
              <span
                className={clsx(
                  'text-xs font-semibold tabular-nums ml-1',
                  g.userNetBalance > 0
                    ? 'text-success-400'
                    : g.userNetBalance < 0
                    ? 'text-danger-400'
                    : 'text-slate-500',
                )}
              >
                {g.userNetBalance > 0
                  ? `+${g.userNetBalance.toFixed(2)}`
                  : g.userNetBalance < 0
                  ? g.userNetBalance.toFixed(2)
                  : '—'}
              </span>
            </Link>
          ))}

          {/* Add / Join */}
          <div className="pt-3 space-y-1">
            <Link to="/groups/create" className="nav-link">
              <PlusCircle className="w-4 h-4" />
              Create Group
            </Link>
            <Link to="/groups/join" className="nav-link">
              <ChevronRight className="w-4 h-4" />
              Join Group
            </Link>
          </div>
        </nav>

        {/* User */}
        <div className="p-3 border-t border-surface-700/50">
          <div className="flex items-center gap-3 p-2 rounded-xl hover:bg-surface-700/30 transition-colors cursor-default">
            <div className="w-8 h-8 rounded-full bg-brand-gradient flex items-center justify-center text-xs font-bold text-white flex-shrink-0">
              {user?.displayName?.[0]?.toUpperCase() ?? 'U'}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-slate-200 truncate">{user?.displayName}</p>
              <p className="text-xs text-slate-500 truncate">{user?.email}</p>
            </div>
            <button
              onClick={handleLogout}
              className="btn-icon text-slate-500 hover:text-danger-400"
              title="Sign out"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </aside>

      {/* ── Main Content ── */}
      <main className="flex-1 overflow-y-auto">
        <div className="page-enter min-h-full">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
