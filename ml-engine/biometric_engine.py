import json
import redis
import torch
import numpy as np
import os
import time
from scipy.spatial.distance import euclidean
from model_architecture import SiameseNetwork


class BiometricEngine:
    def __init__(self):
        redis_host = os.environ.get('REDIS_HOST', 'localhost')
        redis_port = int(os.environ.get('REDIS_PORT', '6379'))

        self.redis_client = self._connect_to_redis(redis_host, redis_port)
        self.pubsub = self.redis_client.pubsub()
        self.pubsub.subscribe('biometric_stream')

        print("SOTA Siamese Deep Learning Engine Online...")

        self.model = SiameseNetwork()

        configured_path = os.environ.get('MODEL_WEIGHTS_PATH')
        candidates = [configured_path, 'Aalto_HardMining_Tuned_best.pth']
        candidates = [path for path in candidates if path]

        loaded = False
        for model_path in candidates:
            if not os.path.exists(model_path):
                continue
            try:
                self.model.load_state_dict(torch.load(model_path, map_location=torch.device('cpu')))
                print(f"Successfully loaded pre-trained Siamese weights from '{model_path}'")
                loaded = True
                break
            except Exception as e:
                print(f"Warning: Could not load weights from '{model_path}': {e}")

        if not loaded:
            print("Warning: No compatible weights found. Using random initialization.")

        self.model.eval()

        self.user_baselines = {}
        self.TRAINING_BATCHES = 5
        self.PAUSE_THRESHOLD_MS = 1000

    def _connect_to_redis(self, redis_host, redis_port):
        last_error = None

        for attempt in range(1, 11):
            try:
                client = redis.Redis(
                    host=redis_host,
                    port=redis_port,
                    decode_responses=True,
                    socket_connect_timeout=5,
                    socket_timeout=5,
                )
                client.ping()
                print(f"Connected to Redis at {redis_host}:{redis_port}")
                return client
            except Exception as error:
                last_error = error
                print(f"Redis connection attempt {attempt}/10 failed: {error}")
                time.sleep(2)

        raise last_error

    def extract_sequential_tensors(self, keystrokes):
        """
        Instead of averaging, we keep the raw sequence for the Neural Network.
        """
        dwells, flights = [], []
        active_keys = {}
        last_keydown_time = None

        for event in keystrokes:
            key, timestamp, ev_type = event['key'], event['timestamp'], event['type']

            if ev_type == 'keydown':
                if last_keydown_time is not None:
                    flight = timestamp - last_keydown_time
                    if flight < self.PAUSE_THRESHOLD_MS:
                        flights.append(flight / 1000.0)
                    else:
                        flights.append(1.0)
                last_keydown_time = timestamp
                active_keys[key] = timestamp

            elif ev_type == 'keyup':
                if key in active_keys:
                    dwell = timestamp - active_keys[key]
                    if dwell < self.PAUSE_THRESHOLD_MS:
                        dwells.append(dwell / 1000.0)
                    else:
                        dwells.append(1.0)
                    del active_keys[key]

        seq_length = 20
        dwells = (dwells + [0.0] * seq_length)[:seq_length]
        flights = (flights + [0.0] * seq_length)[:seq_length]

        sequence_pairs = list(zip(dwells, flights))

        tensor_data = torch.tensor([sequence_pairs], dtype=torch.float32)
        return tensor_data

    def process_payload(self, message_data):
        payload = json.loads(message_data)
        user_id = payload['userId']

        reset_key = f"biometric_reset:{user_id}"
        try:
            reset_flag = self.redis_client.get(reset_key)
            if reset_flag is not None:
                if user_id in self.user_baselines:
                    del self.user_baselines[user_id]
                self.redis_client.delete(reset_key)
                print(f"[{user_id}] Biometric baseline reset requested. Starting new profiling session.")
        except Exception as e:
            print(f"[{user_id}] Warning: failed to read reset flag: {e}")

        tensor_data = self.extract_sequential_tensors(payload['keystrokes'])

        with torch.no_grad():
            live_embedding = self.model(tensor_data).numpy()[0]

        if user_id not in self.user_baselines or len(self.user_baselines[user_id]['history']) < self.TRAINING_BATCHES:
            if user_id not in self.user_baselines:
                self.user_baselines[user_id] = {'history': [], 'master': None}

            self.user_baselines[user_id]['history'].append(live_embedding)
            current = len(self.user_baselines[user_id]['history'])

            print(f"[{user_id}] Profiling deep embedding... ({current}/{self.TRAINING_BATCHES})")

            if current == self.TRAINING_BATCHES:
                self.user_baselines[user_id]['master'] = np.mean(self.user_baselines[user_id]['history'], axis=0)
                print(f"[{user_id}] Siamese Baseline acquired!")

                try:
                    self.redis_client.delete(f"profile_in_progress:{user_id}")
                    print(f"[{user_id}] Profiling window closed. Strict Zero-Trust activated.")
                except Exception as e:
                    print(f"[{user_id}] Failed to delete profiling flag: {e}")

                self.publish_score(user_id, 100)
            return

        master_embedding = self.user_baselines[user_id]['master']

        distance = euclidean(master_embedding, live_embedding)

        trust_score = int(max(100 - (distance * 18.75), 0))

        print(f"[{user_id}] Trust Score: {trust_score} (Euclidean Distance: {distance:.3f})")

        if trust_score > 85:
            self.user_baselines[user_id]['master'] = (master_embedding * 0.9) + (live_embedding * 0.1)
            print(f"[{user_id}] Master embedding updated.")

        self.publish_score(user_id, trust_score)

    def publish_score(self, user_id, trust_score):
        try:
            self.redis_client.setex(f"trust_score:{user_id}", 300, trust_score)
        except Exception as e:
            print(f"[{user_id}] Error setting trust score in Redis KV: {e}")

        self.redis_client.publish('auth_results', json.dumps({"userId": user_id, "trustScore": trust_score}))

    def run(self):
        for message in self.pubsub.listen():
            if message['type'] == 'message':
                try:
                    self.process_payload(message['data'])
                except Exception as e:
                    print(f"Error: {e}")


if __name__ == "__main__":
    BiometricEngine().run()
