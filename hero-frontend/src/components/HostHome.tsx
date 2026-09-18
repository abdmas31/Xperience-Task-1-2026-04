import { useState, type FormEvent } from 'react'
import { navigate } from '../router'
import { ApiRequestError, createEvent } from '../services/api'
import { addMyEvent, getMyEvents } from '../services/myEvents'

function toIsoOrNull(localDateTime: string): string | null {
  if (!localDateTime) return null
  const d = new Date(localDateTime)
  if (Number.isNaN(d.getTime())) return null
  return d.toISOString()
}

export default function HostHome() {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [startTime, setStartTime] = useState('')
  const [location, setLocation] = useState('')
  const [maxCapacity, setMaxCapacity] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const myEvents = getMyEvents()

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)

    const startTimeIso = toIsoOrNull(startTime)
    if (!startTimeIso) {
      setError('Please choose a valid start date/time.')
      return
    }

    setSubmitting(true)
    try {
      const event = await createEvent({
        title,
        description,
        startTime: startTimeIso,
        location,
        maxCapacity: maxCapacity.trim() === '' ? null : Number(maxCapacity),
      })
      addMyEvent({ id: event.id, title: event.title })
      navigate(`/host/${event.id}`)
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Could not create the event.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="page">
      <div className="header">
        <h1>Event RSVP Manager</h1>
        <span className="brand">Host view</span>
      </div>

      {myEvents.length > 0 && (
        <div className="card">
          <h2>Your events</h2>
          <div className="link-list">
            {myEvents.map((e) => (
              <div className="link-row" key={e.id}>
                <span>{e.title}</span>
                <button className="link-button" onClick={() => navigate(`/host/${e.id}`)}>
                  Open dashboard
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="card">
        <h2>Create an event</h2>
        {error && <div className="error-banner">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="title">Title</label>
            <input id="title" required value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="description">Description</label>
            <textarea id="description" rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>
          <div className="row">
            <div className="field">
              <label htmlFor="startTime">Start date &amp; time</label>
              <input
                id="startTime"
                type="datetime-local"
                required
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="maxCapacity">Max capacity (optional)</label>
              <input
                id="maxCapacity"
                type="number"
                min={1}
                value={maxCapacity}
                onChange={(e) => setMaxCapacity(e.target.value)}
                placeholder="Unlimited"
              />
            </div>
          </div>
          <div className="field">
            <label htmlFor="location">Location</label>
            <input id="location" required value={location} onChange={(e) => setLocation(e.target.value)} />
          </div>
          <button className="btn btn-primary" type="submit" disabled={submitting}>
            {submitting ? 'Creating…' : 'Create event'}
          </button>
        </form>
      </div>
    </div>
  )
}
