import { ref, watch, onUnmounted, unref } from 'vue';
import { Client, type IMessage } from '@stomp/stompjs';
import type { KeystrokeData, TrustScoreMessage } from '../types/types';


const savedScore = sessionStorage.getItem('current_trust_score');
const parsedScore = savedScore ? parseInt(savedScore, 10) : null;
const globalTrustScore = ref<number | null>(!Number.isNaN(parsedScore) ? parsedScore : null);

watch(globalTrustScore, (newScore) => {
    if (newScore !== null) {
        sessionStorage.setItem('current_trust_score', newScore.toString());
    } else {
        sessionStorage.removeItem('current_trust_score');
    }
});

let globalStompClient: Client | null = null;
let activeSubscribers = 0; 

export const resetBiometricState = () => {
    globalTrustScore.value = null;
    sessionStorage.removeItem('current_trust_score');

    if (globalStompClient) {
        globalStompClient.deactivate();
        globalStompClient = null;
    }
};

export function useBiometricStream(jwtToken: any) {
    activeSubscribers++;

    watch(() => unref(jwtToken), (newToken) => {
        if (!newToken) {
            console.warn("[STOMP] No token available yet.");
            return;
        }

        if (globalStompClient) {
            console.log("[STOMP] Reusing existing global connection.");
            return;
        }

        console.log("[STOMP] Initializing new WebSocket connection...");
        globalStompClient = new Client({
            brokerURL: `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws/keystrokes`,
            connectHeaders: { Authorization: `Bearer ${newToken}` },
            debug: (str: string) => console.log(str), 
            onConnect: () => {
                console.log('[STOMP] Secure connection established!');
                
                globalStompClient?.subscribe('/user/queue/trust-score', (message: IMessage) => {
                    const data: TrustScoreMessage = JSON.parse(message.body);
                    console.log('[STOMP] Received new Trust Score:', data.trustScore);
                    
                    globalTrustScore.value = data.trustScore;

                    if (data.trustScore < 70) {
                        alert("Biometric mismatch detected. Locking session.");
                        sessionStorage.removeItem('jwt_token');
                    }
                });
            },
            onStompError: (frame) => {
                console.error('[STOMP] Broker reported error:', frame.headers['message']);
            }
        });

        globalStompClient.activate();
    }, { immediate: true });

    onUnmounted(() => {
        activeSubscribers--;
        if (activeSubscribers === 0 && globalStompClient) {
            console.log("[STOMP] No active UI components. Severing connection.");
            globalStompClient.deactivate();
            globalStompClient = null;
        }
    });

    const sendKeystrokeBatch = (keystrokeArray: KeystrokeData[]) => {
        if (globalStompClient && globalStompClient.connected) {
            console.log(`[STOMP] Sending batch of ${keystrokeArray.length} keystrokes...`);
            globalStompClient.publish({
                destination: '/app/stream-biometrics',
                body: JSON.stringify({ keystrokes: keystrokeArray }),
            });
        } else {
            console.warn("[STOMP] Dropped keystrokes: Client is not connected.");
        }
    };

    return { trustScore: globalTrustScore, sendKeystrokeBatch };
}
