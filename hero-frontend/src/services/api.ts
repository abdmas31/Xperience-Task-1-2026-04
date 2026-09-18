import type {
  ApiError,
  DashboardDto,
  EventDto,
  InviteResultDto,
  RsvpResponseValue,
  RsvpViewDto,
} from '../types'
import { getHostId } from './hostId'

const API_BASE = 'http://localhost:8280/api'

export class ApiRequestError extends Error {
  constructor(message: string, public status: number) {
    super(message)
  }
}

async function request<T>(path: string, options: RequestInit & { asHost?: boolean } = {}): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  }
  if (options.asHost) {
    headers['X-Host-Id'] = getHostId()
  }

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers })

  if (!res.ok) {
    let message = `Request failed (${res.status})`
    try {
      const body = (await res.json()) as ApiError
      if (body?.message) message = body.message
    } catch {
      // response had no JSON body
    }
    throw new ApiRequestError(message, res.status)
  }

  if (res.status === 204) {
    return undefined as T
  }
  return (await res.json()) as T
}

export interface CreateEventInput {
  title: string
  description: string
  startTime: string
  location: string
  maxCapacity: number | null
}

export function createEvent(input: CreateEventInput): Promise<EventDto> {
  return request<EventDto>('/events', {
    method: 'POST',
    asHost: true,
    body: JSON.stringify(input),
  })
}

export function getDashboard(eventId: string): Promise<DashboardDto> {
  return request<DashboardDto>(`/events/${eventId}`, { asHost: true })
}

export function closeEvent(eventId: string): Promise<EventDto> {
  return request<EventDto>(`/events/${eventId}/close`, { method: 'POST', asHost: true })
}

export function cancelEvent(eventId: string): Promise<EventDto> {
  return request<EventDto>(`/events/${eventId}/cancel`, { method: 'POST', asHost: true })
}

export function inviteByEmails(eventId: string, emails: string[]): Promise<InviteResultDto[]> {
  return request<InviteResultDto[]>(`/events/${eventId}/invitees`, {
    method: 'POST',
    asHost: true,
    body: JSON.stringify({ emails }),
  })
}

export function getRsvp(token: string): Promise<RsvpViewDto> {
  return request<RsvpViewDto>(`/rsvp/${token}`)
}

export function submitRsvp(token: string, response: RsvpResponseValue): Promise<RsvpViewDto> {
  return request<RsvpViewDto>(`/rsvp/${token}`, {
    method: 'POST',
    body: JSON.stringify({ response }),
  })
}
