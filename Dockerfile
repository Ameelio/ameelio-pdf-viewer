FROM docker.io/eclipse-temurin:17-jdk AS gradle-cache
WORKDIR /cache
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"
COPY gradlew gradlew
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon --version

FROM docker.io/eclipse-temurin:17-jdk

# Install required packages
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Set up Android SDK
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools
ENV GRADLE_USER_HOME=/root/.gradle
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"

# Seed Gradle wrapper cache from the previous stage
COPY --from=gradle-cache /root/.gradle /root/.gradle

# Download and install Android command line tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip && \
    echo "bd1aa17c7ef10066949c88dc6c9c8d536be27f992a1f3b5a584f9bd2ba5646a0  commandlinetools-linux-9477386_latest.zip" | sha256sum -c && \
    unzip commandlinetools-linux-9477386_latest.zip && \
    mv cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm commandlinetools-linux-9477386_latest.zip

# Accept licenses and install SDK components
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Set working directory
WORKDIR /app

# Copy project files
COPY . .

# Make gradlew executable
RUN chmod +x ./gradlew

# Command to run when container starts
CMD ["./gradlew", "--no-daemon", "assembleDebug", "assembleRelease"]
