# Continuous Zero-Trust Authentication via Keystroke Dynamics

> **Academic Achievement:** This architecture was developed and successfully defended as a Bachelor's thesis in Computer Science at Babeș-Bolyai University of Cluj-Napoca, receiving a 10/10 grade.

**For full mathematical proofs, detailed system architecture, and evaluation metrics, please read the [Complete Thesis Paper](docs/thesis_paper.pdf) located in the `/docs` directory.**

## Quick Start

1. Clone the repository and configure your environment:
   ```bash
   git clone https://github.com/Mihai005/zero-trust-behavioral-biometrics.git
   cd zero-trust-behavioral-biometrics
   cp .env.example .env 
   ```
2. Add your database credentials, redis configuration and jwt data to the new `.env` file.
3. Launch the microservices network:
   ```bash
   docker compose up --build
   ```

## License

* **Software:** [MIT License](LICENSE)
* **Academic Documentation:** [Creative Commons CC BY-NC-ND 4.0](docs/LICENSE)
