import torch.nn as nn


class SiameseNetwork(nn.Module):
    def __init__(self, input_features=2, hidden_size=64, latent_dim=128):
        super(SiameseNetwork, self).__init__()

        self.conv_layer = nn.Sequential(
            nn.Conv1d(in_channels=input_features, out_channels=32, kernel_size=3, padding=1),
            nn.ReLU(),
            nn.BatchNorm1d(32),
            nn.Conv1d(in_channels=32, out_channels=64, kernel_size=3, padding=1),
            nn.ReLU(),
            nn.BatchNorm1d(64)
        )

        self.gru = nn.GRU(input_size=64, hidden_size=hidden_size, batch_first=True)

        self.fc = nn.Sequential(
            nn.Linear(hidden_size, latent_dim)
        )

    def forward(self, x):
        # Expected input shape: (batch_size, sequence_length, features)
        x = x.transpose(1, 2)
        x = self.conv_layer(x)
        x = x.transpose(1, 2)
        _, hidden = self.gru(x)
        embedding = self.fc(hidden.squeeze(0))
        return embedding
