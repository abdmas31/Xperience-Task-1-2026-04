import type { ConfirmationState, EffectiveStatus, RsvpResponseValue } from '../types'

export function StatusBadge({ status }: { status: EffectiveStatus }) {
  const cls =
    status === 'OPEN' ? 'badge-open' : status === 'CLOSED' ? 'badge-closed' : status === 'LOCKED' ? 'badge-locked' : 'badge-cancelled'
  return <span className={`badge ${cls}`}>{status}</span>
}

export function ResponseBadge({ response, confirmationState }: { response: RsvpResponseValue; confirmationState: ConfirmationState }) {
  if (response === 'YES' && confirmationState === 'CONFIRMED') {
    return <span className="badge badge-confirmed">Confirmed</span>
  }
  if (response === 'YES' && confirmationState === 'WAITLISTED') {
    return <span className="badge badge-waitlisted">Waitlisted</span>
  }
  if (response === 'NO') {
    return <span className="badge badge-no">No</span>
  }
  if (response === 'MAYBE') {
    return <span className="badge badge-maybe">Maybe</span>
  }
  return <span className="badge badge-pending">No response</span>
}
