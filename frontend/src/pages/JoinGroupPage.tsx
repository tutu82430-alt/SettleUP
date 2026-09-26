import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { useJoinGroup } from '@/hooks/useGroups'
import { Loader2, ArrowLeft, Hash } from 'lucide-react'

interface JoinGroupForm {
  inviteCode: string
}

export default function JoinGroupPage() {
  const navigate = useNavigate()
  const joinGroup = useJoinGroup()
  const { register, handleSubmit, formState: { errors } } = useForm<JoinGroupForm>()

  const onSubmit = async (data: JoinGroupForm) => {
    try {
      const group = await joinGroup.mutateAsync(data.inviteCode.trim().toUpperCase())
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
        <h1 className="text-2xl font-bold text-white">Join a Group</h1>
        <p className="text-slate-400 mt-1">Enter an invite code shared by a group admin</p>
      </div>

      <div className="card p-6">
        <form onSubmit={handleSubmit(onSubmit)} id="join-group-form" className="space-y-5">
          <div>
            <label className="label" htmlFor="invite-code">Invite Code *</label>
            <div className="relative">
              <Hash className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
              <input
                id="invite-code"
                className={`input pl-10 font-mono tracking-widest uppercase text-lg ${errors.inviteCode ? 'input-error' : ''}`}
                placeholder="ABCD1234"
                maxLength={8}
                {...register('inviteCode', {
                  required: 'Invite code is required',
                  minLength: { value: 6, message: 'Invite code is too short' },
                })}
              />
            </div>
            {errors.inviteCode && <p className="mt-1 text-xs text-danger-400">{errors.inviteCode.message}</p>}
          </div>

          <div className="p-4 bg-brand-900/20 border border-brand-800/30 rounded-xl">
            <p className="text-xs text-brand-300">
              Ask the group admin for the 8-character invite code. Codes are case-insensitive.
            </p>
          </div>

          <button
            id="join-group-submit"
            type="submit"
            disabled={joinGroup.isPending}
            className="btn-primary w-full btn-lg"
          >
            {joinGroup.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : null}
            {joinGroup.isPending ? 'Joining…' : 'Join Group'}
          </button>
        </form>
      </div>
    </div>
  )
}
