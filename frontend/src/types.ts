export interface AuthResponse { token: string }
export interface Challenge { id: number; name: string; description: string; startTime: string; endTime: string; status: string; rewardScheme: string; tasks: Task[] }
export interface Task { id: number; name: string; description: string; requiredMetric: string; targetValue: number; rewardPoints: number; status: string }
export interface Device { id: number; deviceName: string; manufacturer: string; model: string; capabilities: string[] }
export interface ActivityEvent { eventId: string; metricType: string; metricValue: number; eventTime: string; processedStatus: string }
export interface ChallengeRank { userId: number; challengeId: number; status: string; finalScore: number | null; rank: number | null; pointsAwarded: number; joinedAt: string }
