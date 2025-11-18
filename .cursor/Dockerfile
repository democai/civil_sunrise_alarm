# Multi-stage Docker image for Android CI build environment
# Based on official Android image for building the Civil Sunrise Alarm app

FROM android:34-jdk17 AS base

# Set environment variables
ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    JAVA_HOME=/usr/local/openjdk-17 \
    PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/tools/bin

# Install required packages
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    curl \
    wget \
    unzip \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Accept Android SDK licenses
RUN mkdir -p ${ANDROID_SDK_ROOT}/licenses && \
    echo "8933bad161af4d0d24f4ba34f83bfd3b4a2e3d35d36a6c9534b75941ee6d5bef" > ${ANDROID_SDK_ROOT}/licenses/android-sdk-license && \
    echo "d56f5187479451eabf01fb78af6dfcb131b33910" > ${ANDROID_SDK_ROOT}/licenses/android-sdk-preview-license && \
    echo "504667f4c0de7973335447fc81aaad6668339117" > ${ANDROID_SDK_ROOT}/licenses/android-sdk-license-extra

# Build stage - compile the Android app
FROM base AS builder

WORKDIR /workspace

# Copy project files
COPY . .

# Build the release APK
RUN cd android && \
    chmod +x gradlew && \
    ./gradlew clean assembleRelease --no-daemon --gradle-user-home /workspace/.gradle

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
