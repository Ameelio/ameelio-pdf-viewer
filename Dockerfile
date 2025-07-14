FROM docker.io/eclipse-temurin:17-jdk

# Install required packages
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Set up Android SDK
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

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

# Pre-download Gradle distribution to cache it in the image
RUN mkdir -p /opt/gradle && \
    wget -q https://services.gradle.org/distributions/gradle-8.0-bin.zip -O /opt/gradle/gradle-8.0-bin.zip && \
    echo "4159b938ec734a8388ce03f52aa8f3c7ed0d31f5438622545de4f83a89b79788  /opt/gradle/gradle-8.0-bin.zip" | sha256sum -c

# Set Gradle user home to use cached distribution
ENV GRADLE_USER_HOME=/root/.gradle

# Pre-create gradle wrapper directory and extract distribution
RUN mkdir -p /root/.gradle/wrapper/dists/gradle-8.0-bin/cf74e924e60763a5b9e65370c5c82e61 && \
    cp /opt/gradle/gradle-8.0-bin.zip /root/.gradle/wrapper/dists/gradle-8.0-bin/cf74e924e60763a5b9e65370c5c82e61/ && \
    cd /root/.gradle/wrapper/dists/gradle-8.0-bin/cf74e924e60763a5b9e65370c5c82e61 && \
    unzip -q gradle-8.0-bin.zip && \
    touch gradle-8.0-bin.zip.ok

# Set working directory
WORKDIR /app

# Copy project files
COPY . .

# Make gradlew executable
RUN chmod +x ./gradlew

# Command to run when container starts
CMD ["./gradlew", "assembleDebug", "assembleRelease"]
