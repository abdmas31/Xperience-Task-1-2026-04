import { useCallback, useEffect, useState } from 'react'
import { ApiRequestError, getRsvp, submitRsvp } from '../services/api'
import type { RsvpResponseValue, RsvpViewDto } from '../types'
import { ResponseBadge, StatusBadge } from './Badges'

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

const CHOICES: { value: RsvpResponseValue; label: string }[] = [
  { value: 'YES', label: 'Yes, I’ll be there' },
  { value: 'MAYBE', label: 'Maybe' },
  { value: 'NO', label: "Can't make it" },
]

export default function RsvpPage({ token }: { token: string }) {
  const [view, setView] = useState<RsvpViewDto | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState<RsvpResponseValue | null>(null)

  const load = useCallback(async () => {
    try {
      setView(await getRsvp(token))
      setLoadError(null)
    } catch (err) {
      setLoadError(err instanceof ApiRequestError ? err.message : 'Could not load this invitation.')
    }
  }, [token])

  useEffect(() => {
    load()
  }, [load])

  async function respond(value: RsvpResponseValue) {
    setSubmitting(value)
    setActionError(null)
    try {
      setView(await submitRsvp(token, value))
    } catch (err) {
      setActionError(err instanceof ApiRequestError ? err.message : 'Could not save your response.')
    } finally {
      setSubmitting(null)
    }
  }

  if (loadError) {
    return (
      <div className="page">
        <div className="center">
          <h1>Invitation not found</h1>
          <p className="muted" style={{ marginTop: '0.5rem' }}>
            {loadError}
          </p>
        </div>
      </div>
    )
  }

  if (!view) {
    return (
      <div className="page">
        <div className="center muted">Loading…</div>
      </div>
    )
  }

  const { event } = view
  const locked = event.status !== 'OPEN'

  return (
    <div className="page">
      <div className="header">
        <h1>{event.title}</h1>
        <StatusBadge status={event.status} />
      </div>

      <div className="card">
        {event.description && <p className="muted" style={{ marginBottom: '0.75rem' }}>{event.description}</p>}
        <p className="muted">
          {formatDateTime(event.startTime)} · {event.location}
        </p>
        {event.maxCapacity != null && (
          <p className="muted" style={{ marginTop: '0.4rem' }}>
            {event.confirmedCount} / {event.maxCapacity} confirmed
          </p>
        )}
      </div>

      <div className="card">
        <h2>Your response</h2>
        <p className="muted">
          Invited as <strong>{view.email}</strong> — current status: <ResponseBadge response={view.response} confirmationState={view.confirmationState} />
        </p>

        {view.response === 'YES' && view.confirmationState === 'WAITLISTED' && (
          <p className="muted" style={{ marginTop: '0.5rem' }}>
            The event is at capacity — you'll move to confirmed automatically if a spot opens up.
          </p>
        )}

        {actionError && <div className="error-banner" style={{ marginTop: '1rem' }}>{actionError}</div>}

        {locked ? (
          <p className="muted" style={{ marginTop: '1rem' }}>
            {event.status === 'CANCELLED'
              ? 'This event has been cancelled.'
              : event.status === 'LOCKED'
                ? 'This event has already started, so responses are locked.'
                : 'The host has closed this event to further responses.'}
          </p>
        ) : (
          <div className="rsvp-choices">
            {CHOICES.map((choice) => (
              <button
                key={choice.value}
                className={`rsvp-choice${view.response === choice.value ? ' active' : ''}`}
                disabled={submitting !== null}
                onClick={() => respond(choice.value)}
              >
                {submitting === choice.value ? 'Saving…' : choice.label}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
