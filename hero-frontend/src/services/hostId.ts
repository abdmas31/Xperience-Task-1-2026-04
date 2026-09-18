const STORAGE_KEY = 'hero_host_id'

/**
 * Stubbed host identity (DESIGN.md OQ1 / Step 11): a random id generated once per browser and
 * sent as the X-Host-Id header. There is no real authentication — this only lets the backend
 * enforce "only the creator can manage their own event" (INV-6) within this prototype's scope.
 */
export function getHostId(): string {
  let id = localStorage.getItem(STORAGE_KEY)
  if (!id) {
    id = crypto.randomUUID()
    localStorage.setItem(STORAGE_KEY, id)
  }
  return id
}
