import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { navigate } from '../router'
import { ApiRequestError, cancelEvent, closeEvent, getDashboard, inviteByEmails } from '../services/api'
import type { DashboardDto, InviteResultDto } from '../types'
import { ResponseBadge, StatusBadge } from './Badges'

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

function linkFor(rsvpPath: string): string {
  return `${window.location.origin}${rsvpPath}`
}

export default function EventDashboard({ eventId }: { eventId: string }) {
  const [dashboard, setDashboard] = useState<DashboardDto | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [emails, setEmails] = useState('')
  const [inviting, setInviting] = useState(false)
  const [newlyInvited, setNewlyInvited] = useState<InviteResultDto[]>([])
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    try {
      setDashboard(await getDashboard(eventId))
      setLoadError(null)
    } catch (err) {
      setLoadError(
        err instanceof ApiRequestError
          ? err.status === 403
            ? "You aren't the host of this event on this browser/device."
            : err.message
          : 'Could not load this event.'
      )
    }
  }, [eventId])

  useEffect(() => {
    load()
  }, [load])

  async function handleInvite(e: FormEvent) {
    e.preventDefault()
    setActionError(null)
    const list = emails
      .split(/[\n,]/)
      .map((s) => s.trim())
      .filter(Boolean)
    if (list.length === 0) return

    setInviting(true)
    try {
      const created = await inviteByEmails(eventId, list)
      setNewlyInvited(created)
      setEmails('')
      await load()
    } catch (err) {
      setActionError(err instanceof ApiRequestError ? err.message : 'Could not invite those people.')
    } finally {
      setInviting(false)
    }
  }

  async function handleClose() {
    if (!confirm('Close this event to further RSVP changes?')) return
    setBusy(true)
    setActionError(null)
    try {
      await closeEvent(eventId)
      await load()
    } catch (err) {
      setActionError(err instanceof ApiRequestError ? err.message : 'Could not close the event.')
    } finally {
      setBusy(false)
    }
  }

  async function handleCancel() {
    if (!confirm('Cancel this event? This cannot be undone.')) return
    setBusy(true)
    setActionError(null)
    try {
      await cancelEvent(eventId)
      await load()
    } catch (err) {
      setActionError(err instanceof ApiRequestError ? err.message : 'Could not cancel the event.')
    } finally {
      setBusy(false)
    }
  }

  if (loadError) {
    return (
      <div className="page">
        <div className="header">
          <h1>Event RSVP Manager</h1>
          <button className="link-button" onClick={() => navigate('/')}>
            ← Back
          </button>
        </div>
        <div className="error-banner">{loadError}</div>
      </div>
    )
  }

  if (!dashboard) {
    return (
      <div className="page">
        <div className="center muted">Loading…</div>
      </div>
    )
  }

  const { event, counts, attendees } = dashboard
  const canManage = event.status === 'OPEN' || event.status === 'CLOSED'

  return (
    <div className="page">
      <div className="header">
        <h1>{event.title}</h1>
        <button className="link-button" onClick={() => navigate('/')}>
          ← All events
        </button>
      </div>

      {actionError && <div className="error-banner">{actionError}</div>}

      <div className="card">
        <div className="row" style={{ alignItems: 'center', marginBottom: '0.75rem' }}>
          <StatusBadge status={event.status} />
          <button className="link-button" onClick={load}>
            Refresh
          </button>
        </div>
        {event.description && <p className="muted" style={{ marginBottom: '0.75rem' }}>{event.description}</p>}
        <p className="muted">
          {formatDateTime(event.startTime)} · {event.location}
          {event.maxCapacity != null && ` · capacity ${event.maxCapacity}`}
        </p>
        <div className="btn-group" style={{ marginTop: '1rem' }}>
          <button className="btn" onClick={handleClose} disabled={busy || event.status !== 'OPEN'}>
            Close to further responses
          </button>
          <button className="btn btn-danger" onClick={handleCancel} disabled={busy || event.status === 'CANCELLED'}>
            Cancel event
          </button>
        </div>
      </div>

      <div className="card">
        <h2>Attendance</h2>
        <div className="counts">
          <div className="count-tile">
            <div className="n">{counts.confirmed}</div>
            <div className="label">Confirmed</div>
          </div>
          <div className="count-tile">
            <div className="n">{counts.waitlisted}</div>
            <div className="label">Waitlisted</div>
          </div>
          <div className="count-tile">
            <div className="n">{counts.maybe}</div>
            <div className="label">Maybe</div>
          </div>
          <div className="count-tile">
            <div className="n">{counts.no}</div>
            <div className="label">No</div>
          </div>
          <div className="count-tile">
            <div className="n">{counts.pending}</div>
            <div className="label">No response</div>
          </div>
        </div>

        {attendees.length === 0 ? (
          <p className="muted">No one invited yet.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Email</th>
                <th>Response</th>
                <th>Responded</th>
                <th>Link</th>
              </tr>
            </thead>
            <tbody>
              {attendees.map((a) => (
                <tr key={a.id}>
                  <td className="email">{a.email}</td>
                  <td>
                    <ResponseBadge response={a.response} confirmationState={a.confirmationState} />
                  </td>
                  <td className="muted">{a.respondedAt ? formatDateTime(a.respondedAt) : '—'}</td>
                  <td>
                    <button className="link-button" onClick={() => navigator.clipboard?.writeText(linkFor(`/rsvp/${a.token}`))}>
                      Copy link
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <h2>Invite people</h2>
        {!canManage && <p className="muted">This event is {event.status.toLowerCase()} — invites are no longer accepted.</p>}
        {canManage && (
          <form onSubmit={handleInvite}>
            <div className="field">
              <label htmlFor="emails">Email addresses (one per line, or comma-separated)</label>
              <textarea
                id="emails"
                rows={3}
                value={emails}
                onChange={(e) => setEmails(e.target.value)}
                placeholder={'ada@example.com\ngrace@example.com'}
              />
            </div>
            <button className="btn btn-primary" type="submit" disabled={inviting}>
              {inviting ? 'Inviting…' : 'Send invites'}
            </button>
          </form>
        )}

        {newlyInvited.length > 0 && (
          <div className="link-list">
            <p className="muted">
              Delivery isn't automated yet — copy each link below and share it with that invitee yourself.
            </p>
            {newlyInvited.map((invite) => (
              <div className="link-row" key={invite.id}>
                <span>{invite.email}</span>
                <code>{linkFor(invite.rsvpPath)}</code>
                <button className="link-button" onClick={() => navigator.clipboard?.writeText(linkFor(invite.rsvpPath))}>
                  Copy
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
