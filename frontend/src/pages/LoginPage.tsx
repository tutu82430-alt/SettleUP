import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { useLogin } from '@/hooks/useAuth'
import { Wallet, Mail, Lock, ArrowRight, Loader2 } from 'lucide-react'

interface LoginForm {
  email: string
  password: string
}

export default function LoginPage() {
  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>()
  const login = useLogin()
  const navigate = useNavigate()

  const onSubmit = async (data: LoginForm) => {
    try {
      await login.mutateAsync(data)
      navigate('/dashboard')
    } catch {
      // handled in hook
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      {/* Background glow */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 w-96 h-96 bg-brand-600/10 rounded-full blur-3xl" />
        <div className="absolute bottom-1/4 left-1/3 w-64 h-64 bg-purple-600/8 rounded-full blur-3xl" />
      </div>

      <div className="w-full max-w-md relative">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-brand-gradient shadow-glow-blue mb-4">
            <Wallet className="w-7 h-7 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white mb-2">Welcome back</h1>
          <p className="text-slate-400">Sign in to your SettleUp account</p>
        </div>

        {/* Form */}
        <div className="card p-8 space-y-5">
          <form onSubmit={handleSubmit(onSubmit)} id="login-form" className="space-y-5">
            {/* Email */}
            <div>
              <label className="label" htmlFor="login-email">Email</label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                <input
                  id="login-email"
                  type="email"
                  className={`input pl-10 ${errors.email ? 'input-error' : ''}`}
                  placeholder="you@example.com"
                  {...register('email', { required: 'Email is required' })}
                />
              </div>
              {errors.email && (
                <p className="mt-1 text-xs text-danger-400">{errors.email.message}</p>
              )}
            </div>

            {/* Password */}
            <div>
              <label className="label" htmlFor="login-password">Password</label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                <input
                  id="login-password"
                  type="password"
                  className={`input pl-10 ${errors.password ? 'input-error' : ''}`}
                  placeholder="••••••••"
                  {...register('password', { required: 'Password is required' })}
                />
              </div>
              {errors.password && (
                <p className="mt-1 text-xs text-danger-400">{errors.password.message}</p>
              )}
            </div>

            <button
              id="login-submit"
              type="submit"
              form="login-form"
              disabled={login.isPending}
              className="btn-primary w-full btn-lg"
            >
              {login.isPending ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <ArrowRight className="w-4 h-4" />
              )}
              {login.isPending ? 'Signing in…' : 'Sign In'}
            </button>
          </form>

          <div className="divider" />

          <p className="text-center text-sm text-slate-400">
            Don't have an account?{' '}
            <Link to="/register" className="text-brand-400 hover:text-brand-300 font-medium transition-colors">
              Create one
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
