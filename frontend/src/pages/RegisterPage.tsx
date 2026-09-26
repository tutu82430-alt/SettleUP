import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { useRegister } from '@/hooks/useAuth'
import { Wallet, Mail, Lock, User, ArrowRight, Loader2 } from 'lucide-react'

interface RegisterForm {
  displayName: string
  username: string
  email: string
  password: string
  confirmPassword: string
}

export default function RegisterPage() {
  const { register, handleSubmit, watch, formState: { errors } } = useForm<RegisterForm>()
  const registerUser = useRegister()
  const navigate = useNavigate()

  const onSubmit = async (data: RegisterForm) => {
    try {
      await registerUser.mutateAsync({
        username: data.username,
        email: data.email,
        password: data.password,
        displayName: data.displayName,
      })
      navigate('/dashboard')
    } catch {
      // handled in hook
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-1/4 right-1/3 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl" />
        <div className="absolute bottom-1/3 left-1/4 w-64 h-64 bg-brand-600/8 rounded-full blur-3xl" />
      </div>

      <div className="w-full max-w-md relative">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-brand-gradient shadow-glow-blue mb-4">
            <Wallet className="w-7 h-7 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white mb-2">Join SettleUp</h1>
          <p className="text-slate-400">Start splitting expenses smarter</p>
        </div>

        <div className="card p-8 space-y-5">
          <form onSubmit={handleSubmit(onSubmit)} id="register-form" className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="label" htmlFor="reg-displayname">Display Name</label>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                  <input
                    id="reg-displayname"
                    className={`input pl-10 ${errors.displayName ? 'input-error' : ''}`}
                    placeholder="Alice"
                    {...register('displayName', { required: 'Required' })}
                  />
                </div>
                {errors.displayName && <p className="mt-1 text-xs text-danger-400">{errors.displayName.message}</p>}
              </div>
              <div>
                <label className="label" htmlFor="reg-username">Username</label>
                <input
                  id="reg-username"
                  className={`input ${errors.username ? 'input-error' : ''}`}
                  placeholder="alice99"
                  {...register('username', {
                    required: 'Required',
                    minLength: { value: 3, message: 'Min 3 chars' },
                  })}
                />
                {errors.username && <p className="mt-1 text-xs text-danger-400">{errors.username.message}</p>}
              </div>
            </div>

            <div>
              <label className="label" htmlFor="reg-email">Email</label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                <input
                  id="reg-email"
                  type="email"
                  className={`input pl-10 ${errors.email ? 'input-error' : ''}`}
                  placeholder="alice@example.com"
                  {...register('email', { required: 'Email is required' })}
                />
              </div>
              {errors.email && <p className="mt-1 text-xs text-danger-400">{errors.email.message}</p>}
            </div>

            <div>
              <label className="label" htmlFor="reg-password">Password</label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                <input
                  id="reg-password"
                  type="password"
                  className={`input pl-10 ${errors.password ? 'input-error' : ''}`}
                  placeholder="Min 8 characters"
                  {...register('password', {
                    required: 'Password is required',
                    minLength: { value: 8, message: 'Min 8 characters' },
                  })}
                />
              </div>
              {errors.password && <p className="mt-1 text-xs text-danger-400">{errors.password.message}</p>}
            </div>

            <div>
              <label className="label" htmlFor="reg-confirm">Confirm Password</label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                <input
                  id="reg-confirm"
                  type="password"
                  className={`input pl-10 ${errors.confirmPassword ? 'input-error' : ''}`}
                  placeholder="Repeat password"
                  {...register('confirmPassword', {
                    required: 'Please confirm your password',
                    validate: (val) => val === watch('password') || 'Passwords do not match',
                  })}
                />
              </div>
              {errors.confirmPassword && <p className="mt-1 text-xs text-danger-400">{errors.confirmPassword.message}</p>}
            </div>

            <button
              id="register-submit"
              type="submit"
              disabled={registerUser.isPending}
              className="btn-primary w-full btn-lg mt-2"
            >
              {registerUser.isPending ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <ArrowRight className="w-4 h-4" />
              )}
              {registerUser.isPending ? 'Creating account…' : 'Create Account'}
            </button>
          </form>

          <div className="divider" />

          <p className="text-center text-sm text-slate-400">
            Already have an account?{' '}
            <Link to="/login" className="text-brand-400 hover:text-brand-300 font-medium transition-colors">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
