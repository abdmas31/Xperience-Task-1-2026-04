import { useEffect, useState } from 'react'
import './App.css'
import HostHome from './components/HostHome'
import EventDashboard from './components/EventDashboard'
import RsvpPage from './components/RsvpPage'

function App() {
  const [path, setPath] = useState(window.location.pathname)

  useEffect(() => {
    const onPopState = () => setPath(window.location.pathname)
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])

  const rsvpMatch = path.match(/^\/rsvp\/([^/]+)\/?$/)
  if (rsvpMatch) {
    return <RsvpPage token={rsvpMatch[1]} />
  }

  const hostMatch = path.match(/^\/host\/([^/]+)\/?$/)
  if (hostMatch) {
    return <EventDashboard eventId={hostMatch[1]} />
  }

  return <HostHome />
}

export default App
