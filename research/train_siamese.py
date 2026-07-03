import torch
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
import numpy as np
import pandas as pd
import random
import os
import glob
import torch.nn as nn

from model_architecture import SiameseNetwork


class KeystrokeTripletDataset(Dataset):
    def __init__(self, data_directory, seq_length=20, num_triplets=5000, max_users=300):
        self.seq_length = seq_length
        self.num_triplets = num_triplets
        self.user_chunks = {}

        search_pattern = os.path.join(data_directory, '*_keystrokes.txt')
        all_files = glob.glob(search_pattern)

        files_to_process = all_files[:max_users]
        print(f"Found {len(all_files)} files. Extracting tensors from {len(files_to_process)} users...")

        for file_path in files_to_process:
            try:
                df = pd.read_csv(file_path, sep='\t')

                df = df.sort_values(by=['TEST_SECTION_ID', 'PRESS_TIME'])

                user_id = df['PARTICIPANT_ID'].iloc[0]

                df['dwell_time'] = (df['RELEASE_TIME'] - df['PRESS_TIME']) / 1000.0

                df['flight_time'] = df.groupby('TEST_SECTION_ID')['PRESS_TIME'].diff() / 1000.0
                df['flight_time'] = df['flight_time'].fillna(0)

                df['flight_time'] = df['flight_time'].clip(upper=1.0)
                df['dwell_time'] = df['dwell_time'].clip(upper=1.0)

                dwells = df['dwell_time'].values
                flights = df['flight_time'].values

                chunks = []
                for i in range(0, len(dwells) - seq_length, seq_length):
                    chunk_dwells = dwells[i: i + seq_length]
                    chunk_flights = flights[i: i + seq_length]

                    if len(chunk_dwells) == seq_length:
                        chunk_tensor = np.column_stack((chunk_dwells, chunk_flights))
                        chunks.append(chunk_tensor)

                if len(chunks) >= 2:
                    self.user_chunks[user_id] = chunks

            except Exception as e:
                continue

        self.users = list(self.user_chunks.keys())
        print(f"Successfully processed {len(self.users)} distinct users ready for training.")

    def __len__(self):
        return self.num_triplets

    def __getitem__(self, idx):
        anchor_user = random.choice(self.users)

        anchor_idx, positive_idx = random.sample(range(len(self.user_chunks[anchor_user])), 2)
        anchor_chunk = self.user_chunks[anchor_user][anchor_idx]
        positive_chunk = self.user_chunks[anchor_user][positive_idx]

        negative_user = random.choice(self.users)
        while negative_user == anchor_user:
            negative_user = random.choice(self.users)

        negative_chunk = random.choice(self.user_chunks[negative_user])

        anchor = torch.tensor(anchor_chunk, dtype=torch.float32)
        positive = torch.tensor(positive_chunk, dtype=torch.float32)
        negative = torch.tensor(negative_chunk, dtype=torch.float32)

        return anchor, positive, negative


def train_model():
    print("Initializing Siamese Pre-training...")

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = SiameseNetwork().to(device)

    criterion = nn.TripletMarginLoss(margin=1.0, p=2)
    optimizer = optim.Adam(model.parameters(), lr=0.001)

    absolute_path = os.getenv("DATASET_PATH")

    dataset = KeystrokeTripletDataset(data_directory=absolute_path, max_users=300, num_triplets=5000)
    dataloader = DataLoader(dataset, batch_size=64, shuffle=True)

    epochs = 10

    for epoch in range(epochs):
        model.train()
        total_loss = 0

        for anchor, positive, negative in dataloader:
            anchor, positive, negative = anchor.to(device), positive.to(device), negative.to(device)

            optimizer.zero_grad()

            emb_anchor = model(anchor)
            emb_positive = model(positive)
            emb_negative = model(negative)

            loss = criterion(emb_anchor, emb_positive, emb_negative)

            loss.backward()
            optimizer.step()

            total_loss += loss.item()

        print(f"Epoch {epoch + 1}/{epochs} | Loss: {total_loss / len(dataloader):.4f}")

    model_output_path = os.environ.get('MODEL_WEIGHTS_PATH', 'keystroke_sota_weights.pth')
    torch.save(model.state_dict(), model_output_path)
    print(f"Pre-training complete! Saved weights to '{model_output_path}'")


if __name__ == "__main__":
    train_model()
