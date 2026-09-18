const STORAGE_KEY = 'hero_my_events'

export interface MyEventRef {
  id: string
  title: string
}

/** Local convenience list only — the backend has no "list events for host" endpoint (not in DESIGN.md scope). */
export function getMyEvents(): MyEventRef[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as MyEventRef[]) : []
  } catch {
    return []
  }
}

export function addMyEvent(ref: MyEventRef): void {
  const existing = getMyEvents().filter((e) => e.id !== ref.id)
  localStorage.setItem(STORAGE_KEY, JSON.stringify([ref, ...existing]))
}
