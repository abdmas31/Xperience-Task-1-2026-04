export type EffectiveStatus = 'OPEN' | 'CLOSED' | 'CANCELLED' | 'LOCKED'
export type RsvpResponseValue = 'PENDING' | 'YES' | 'NO' | 'MAYBE'
export type ConfirmationState = 'NONE' | 'CONFIRMED' | 'WAITLISTED'

export interface EventDto {
  id: string
  title: string
  description: string | null
  startTime: string
  location: string
  maxCapacity: number | null
  status: EffectiveStatus
  createdAt: string
}

export interface AttendeeDto {
  id: string
  email: string
  token: string
  response: RsvpResponseValue
  confirmationState: ConfirmationState
  invitedAt: string
  respondedAt: string | null
}

export interface CountsDto {
  confirmed: number
  waitlisted: number
  no: number
  maybe: number
  pending: number
}

export interface DashboardDto {
  event: EventDto
  counts: CountsDto
  attendees: AttendeeDto[]
}

export interface InviteResultDto {
  id: string
  email: string
  token: string
  rsvpPath: string
}

export interface PublicEventDto {
  title: string
  description: string | null
  startTime: string
  location: string
  maxCapacity: number | null
  confirmedCount: number
  status: EffectiveStatus
}

export interface RsvpViewDto {
  event: PublicEventDto
  email: string
  response: RsvpResponseValue
  confirmationState: ConfirmationState
  respondedAt: string | null
}

export interface ApiError {
  message: string
  timestamp: string
}
