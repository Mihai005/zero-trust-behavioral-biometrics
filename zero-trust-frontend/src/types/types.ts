export interface KeystrokeData {
    key: string;
    timestamp: number;
    type: 'keydown' | 'keyup';
}

export interface TrustScoreMessage {
    userId: string;
    trustScore: number;
}
