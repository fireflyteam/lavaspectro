# Plan: Lavalink Spectrogram Plugin

## Objective
Create a Lavalink plugin that pre-calculates and caches spectrogram data for tracks. This allows the TUI (`jorik-cli`) to render a high-quality real-time visualizer without the overhead of streaming raw audio or performing FFT math on the client side.

## Core Features
1.  **Server-Side Analysis**:
    - Perform FFT (Fast Fourier Transform) on the audio stream within Lavalink.
    - Downsample the data into a "Visualizer Summary":
        - **Frequency Resolution**: 32 to 64 bands (perfect for TUI columns).
        - **Temporal Resolution**: 10 to 20 snapshots per second.
        - **Normalization**: Values scaled to 0-255 (1 byte per band).
2.  **Efficient Data Format**:
    - Store data as a compressed binary blob or a compact JSON array of integers.
    - Target file size: ~50KB - 150KB for a standard 3-minute song.
3.  **Caching Mechanism**:
    - Cache results by Track Identifier/Hash to avoid redundant processing.
    - Provide a persistent storage option (local disk) for the cache.
4.  **REST API**:
    - Add a new endpoint to Lavalink: `GET /v4/plugins/spectrogram/{trackId}`.
    - Returns the full "map" of the song's frequencies.

## Integration Workflow
1.  **Bot Client**:
    - When a track starts, the Bot notifies the TUI via the existing WebSocket.
2.  **TUI (`jorik-cli`)**:
    - Fetches the spectrogram blob from the Bot (which proxies to Lavalink).
    - Maps the `elapsedMs` from the WebSocket state updates to the corresponding index in the spectrogram array.
    - Renders the bars in the terminal using Unicode characters or block elements.

## Tech Stack
- **Language**: Kotlin (Lavalink standard).
- **Build System**: Gradle.
- **Dependencies**: Lavalink Server API, standard audio processing libraries (if not provided by Lavalink core).

## Hardware Constraints Alignment
- **Lightweight**: By calculating once and caching, we save CPU cycles over time.
- **Bandwidth**: One-time fetch of ~100KB per song is negligible on a 300mbit connection compared to continuous audio streaming.
