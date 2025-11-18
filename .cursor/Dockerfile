# Multi-stage Docker image for Android CI build environment
# Civil Sunrise Alarm - Android CI/CD Pipeline

FROM eclipse-temurin:17-jdk-jammy AS base

# Set environment variables for Android SDK
ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    JAVA_HOME=/opt/java/openjdk \
    PATH=${PATH}:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/build-tools/34.0.0

# Install required packages and Android SDK tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    curl \
    wget \
    unzip \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Create Android SDK directories and accept licenses
RUN mkdir -p ${ANDROID_SDK_ROOT}/licenses && \
    mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    echo "8933bad161af4d0d24f4ba34f83bfd3b4a2e3d35d36a6c9534b75941ee6d5bef" > ${ANDROID_SDK_ROOT}/licenses/android-sdk-license && \
    echo "d56f5187479451eabf01fb78af6dfcb131b33910" > ${ANDROID_SDK_ROOT}/licenses/android-sdk-preview-license && \
    echo "504667f4c0de7973335447fc81aaad6668339117" > ${ANDROID_SDK_ROOT}/licenses/android-sdk-license-extra && \
    echo "e06d67b92b52431ecc7e054fc5364aaad33b13be" > ${ANDROID_SDK_ROOT}/licenses/google-android-sdk-license

# Download and install Android SDK command-line tools
RUN cd ${ANDROID_SDK_ROOT}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip && \
    unzip -q commandlinetools-linux-10406996_latest.zip && \
    rm commandlinetools-linux-10406996_latest.zip && \
    mkdir latest && \
    mv cmdline-tools/* latest/ 2>/dev/null || true

# Install Android SDK packages
RUN yes | ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager \
    "platforms;android-34" \
    "build-tools;34.0.0" \
    "platform-tools" 2>&1 | grep -v "Warning: " || true

# Build stage - compile the Android app
FROM base AS builder

WORKDIR /workspace

# Copy project files
COPY . .

# Build the release APK
# Override the org.gradle.java.home property that's hardcoded for local development
RUN cd android && \
    chmod +x gradlew && \
    ./gradlew clean assembleRelease \
    --no-daemon \
    --gradle-user-home /workspace/.gradle \
    -Dorg.gradle.java.home=$(which java | xargs dirname | xargs dirname)

# Final stage - output image
FROM base

WORKDIR /workspace

# Copy only the build artifacts from builder
COPY --from=builder /workspace/android/app/build/outputs /build/outputs

# Label for identification
LABEL org.opencontainers.image.title="Civil Sunrise Alarm Android CI" \
      org.opencontainers.image.description="CI build environment for Civil Sunrise Alarm Android app" \
      org.opencontainers.image.version="1.0"

# Default command
CMD ["/bin/bash"]
