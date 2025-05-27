# Accelerometer Data Flow in Wasted App

```mermaid
graph TD
    A[Hardware Accelerometer] --> B[Android SensorManager]
    B --> C[AccelerometerService]
    C --> D[FallDetector - SensorEventListener]
    
    D --> E{onSensorChanged Event}
    E --> F[Extract X, Y, Z values]
    F --> G[Calculate Delta Changes]
    G --> H[Calculate Total Movement]
    
    H --> I{Movement > Fall Threshold?}
    I -->|Yes| J[Set fallDetected = true]
    I -->|No| K[Continue Monitoring]
    
    J --> L[Start Time Window - 2 seconds]
    L --> M{Within Time Window?}
    M -->|Yes| N{Current Movement < Stop Threshold?}
    M -->|No| O[Reset Fall Detection]
    
    N -->|Yes| P[Fall + Stop Detected!]
    N -->|No| Q[Continue Monitoring in Window]
    
    P --> R[FallDetectionListener onFallDetected]
    R --> S[AccelerometerService onFallDetected]
    S --> T[Load Sound URI from SharedPreferences]
    T --> U[SoundPlayer playSound]
    U --> V[MediaPlayer creates and plays sound]
    
    Q --> M
    O --> K
    K --> E
    
    %% Service Management
    W[MainActivity] --> X{Enable Fall Detection}
    X -->|Yes| Y[Start AccelerometerService]
    X -->|No| Z[Stop AccelerometerService]
    
    Y --> AA[Create Foreground Notification]
    AA --> BB[Acquire WakeLock]
    BB --> CC[Register SensorEventListener]
    CC --> C
    
    %% Background Operation
    DD[Device Boot] --> EE[BootReceiver]
    EE --> FF{Fall Detection was Enabled?}
    FF -->|Yes| Y
    FF -->|No| GG[Do Nothing]
    
    %% User Configuration
    HH[User Selects Sound] --> II[SoundPicker Component]
    II --> JJ[Save to SharedPreferences]
    JJ --> KK[sound_uri, sound_name stored]
    
    %% Styling
    classDef hardware fill:#e1f5fe
    classDef android fill:#f3e5f5
    classDef service fill:#e8f5e8
    classDef detection fill:#fff3e0
    classDef action fill:#ffebee
    classDef ui fill:#f1f8e9
    
    class A hardware
    class B android
    class C,Y,AA,BB,CC service
    class D,E,F,G,H,I,J,L,M,N,P,R detection
    class S,T,U,V action
    class W,HH,II,X ui
```

## Data Flow Summary

### 1. **Sensor Data Collection**
- Hardware accelerometer provides continuous X, Y, Z acceleration values
- Android SensorManager delivers sensor events at ~50Hz (SENSOR_DELAY_GAME)
- AccelerometerService runs as a foreground service to maintain background operation

### 2. **Fall Detection Algorithm**
- **FallDetector** processes each sensor event in `onSensorChanged()`
- Calculates movement delta between consecutive readings
- Uses two-stage detection:
  - **Stage 1**: Sudden movement above threshold (15.0f) indicates potential fall
  - **Stage 2**: Within 2-second window, detects if movement drops below stop threshold (2.0f)

### 3. **Sound Playback**
- When fall+stop detected, service retrieves user's selected sound URI from SharedPreferences
- **SoundPlayer** uses MediaPlayer to play the custom audio file
- Supports various audio formats (MP3, WAV, M4A, etc.)

### 4. **Background Persistence**
- Service uses WakeLock to prevent CPU sleep during monitoring
- BootReceiver automatically restarts service after device reboot
- Foreground notification keeps service running and informs user of active monitoring

### 5. **User Interface**
- MainActivity provides controls to enable/disable monitoring
- SoundPicker component allows users to select custom audio files
- All preferences stored in SharedPreferences for persistence