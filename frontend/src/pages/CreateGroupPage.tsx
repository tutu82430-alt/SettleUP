import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { useCreateGroup } from '@/hooks/useGroups'
import { Loader2, Users, ArrowLeft } from 'lucide-react'

interface CreateGroupForm {
  name: string
  description: string
  currency: string
}

const CURRENCIES = ['USD', 'EUR', 'GBP', 'INR', 'JPY', 'CAD', 'AUD']

export default function CreateGroupPage() {
  const navigate = useNavigate()
  const createGroup = useCreateGroup()
  const { register, handleSubmit, formState: { errors } } = useForm<CreateGroupForm>({
    defaultValues: { currency: 'USD' },
  })

  const onSubmit = async (data: CreateGroupForm) => {
    try {
      const group = await createGroup.mutateAsync(data)
      navigate(`/groups/${group.id}`)
    } catch {
      // handled in hook
    }
  }

  return (
    <div className="p-6 md:p-8 max-w-lg mx-auto">
      <button onClick={() => navigate(-1)} className="btn-ghost mb-6 -ml-2">
        <ArrowLeft className="w-4 h-4" />
        Back
      </button>

      <div className="mb-6">
        <h1 className="text-2xl font-bold text-white">Create a Group</h1>
        <p className="text-slate-400 mt-1">Invite friends to split expenses together</p>
      </div>

      <div className="card p-6">
        <form onSubmit={handleSubmit(onSubmit)} id="create-group-form" className="space-y-5">
          <div>
            <label className="label" htmlFor="group-name">Group Name *</label>
            <input
              id="group-name"
              className={`input ${errors.name ? 'input-error' : ''}`}
              placeholder="e.g. Summer Trip 2024"
              {...register('name', { required: 'Group name is required', minLength: { value: 2, message: 'Min 2 characters' } })}
            />
            {errors.name && <p className="mt-1 text-xs text-danger-400">{errors.name.message}</p>}
          </div>

          <div>
            <label className="label" htmlFor="group-desc">Description</label>
            <textarea
              id="group-desc"
              className="input h-24 resize-none"
              placeholder="What's this group for?"
              {...register('description')}
            />
          </div>

          <div>
            <label className="label" htmlFor="group-currency">Currency</label>
            <select id="group-currency" className="input" {...register('currency')}>
              {CURRENCIES.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>

          <button
            id="create-group-submit"
            type="submit"
            disabled={createGroup.isPending}
            className="btn-primary w-full btn-lg"
          >
            {createGroup.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Users className="w-4 h-4" />
            )}
            {createGroup.isPending ? 'Creating…' : 'Create Group'}
          </button>
        </form>
      </div>
    </div>
  )
}
