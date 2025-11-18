# Event-Driven Sync Protocol - Development Recipes
# Use `just <recipe>` to run commands

# Default recipe - show available commands
default:
    @just --list

# 🤖 Android Development
android-build:
    @echo "🤖 Building Android app..."
    cd android && ./gradlew assembleDebug
    @echo "🤖 Android app built successfully!"
    @echo "🌐 View app in: $(pwd)/android/app/build/outputs/apk/debug/"

android-test:
    @echo "🧪 Running Android tests..."
    cd android && ./gradlew test

android-test-unit:
    @echo "🧪 Running Android unit tests..."
    cd android && ./gradlew testDebugUnitTest

android-test-instrumented:
    @echo "🧪 Running Android instrumented tests..."
    cd android && ./gradlew connectedAndroidTest

android-test-ui:
    @echo "🧪 Running Android UI tests..."
    cd android && ./gradlew connectedDebugAndroidTest

test-kotlin-interop:
    @echo "🔗 Testing real Kotlin-JavaScript interoperability..."
    ./test-kotlin-interop.sh

android-run:
    @echo "📱 Building and running Android app..."
    cd android && ./gradlew installDebug

android-lint:
    @echo "🔍 Linting Android code..."
    cd android && ./gradlew lintDebug

android-clean:
    @echo "🧹 Cleaning Android build..."
    cd android && ./gradlew clean

android-install:
    @echo "📱 Installing Android app..."
    cd android && ./gradlew installDebug



test-android:
    just android-test

lint-android:
    just android-lint

# Linting alias (from Makefile)
lint:
    @echo "🔍 Linting Android code..."
    @cd android && ./gradlew lint 2>/dev/null || echo "⚠️  Android lint not configured"


# 🧹 Cleanup
clean:
    @echo "🧹 Cleaning all build artifacts..."
    just android-clean
    @echo "✅ Cleanup complete!"

clean-all:
    @echo "🧹 Deep cleaning everything..."
    just clean
    cd android && ./gradlew clean || true
    @echo "✅ Deep clean complete!"

# Clean alias (from Makefile)
clean-makefile:
    @echo "🧹 Cleaning all build artifacts..."
    rm -rf android/.gradle
    rm -rf android/build
    rm -rf android/app/build
    docker-compose down --volumes --remove-orphans

# 📦 Dependencies
deps-update:
    @echo "📦 Updating dependencies..."
    cd android && ./gradlew dependencies --write-locks
    @echo "✅ Dependencies updated!"

# 📱 Android Specific
android-emulator:
    @echo "📱 Starting Android emulator..."
    emulator -avd Pixel_7_API_35 || echo "❌ No emulator found. Create one in Android Studio."

android-device:
    @echo "📱 Listing connected devices..."
    adb devices
android-push:
    #!/usr/bin/env bash
    adb install android/app/build/outputs/apk/debug/app-debug.apk

android-logcat:
    @echo "📋 Showing Android logs..."
    adb logcat | grep -E "(CivilSunriseAlarm|SyncService|SyncWorker)"


# 🚀 Release Management
release-help:
    @echo "🚀 Creating GitHub release with Android APK..."
    @echo "⚠️  This will build the APK and create a GitHub release"
    @echo "📋 Usage: just release | release-draft | release-prerelease <version>"
    @echo "   - release & release-draft: tag = current git shorthash"
    @echo "📋 Example: just release"
    @echo "📋 Example: just release-draft"
    @echo "📋 Example: just release-prerelease v1.0.0-beta"

release:
    #!/usr/bin/env bash
    set -euo pipefail
    cd "$(git rev-parse --show-toplevel)"
    v=$(git rev-parse --short HEAD)
    echo "🚀 Creating GitHub release"
    echo "🏷️  Version (git shorthash): ${v}"
    just check-github-cli
    just check-github-auth
    echo "🧹 Cleaning previous builds..."
    just android-clean
    echo "🤖 Building Android APK..."
    cd android && ./gradlew assembleRelease
    cd "$(git rev-parse --show-toplevel)"
    APK_PATH="android/app/build/outputs/apk/release/app-release.apk"
    echo "📋 APK location: ${APK_PATH}"
    if [[ ! -f "${APK_PATH}" ]]; then
        echo "❌ APK not found. Build may have failed."
        exit 1
    fi
    echo "✅ APK found, creating GitHub release..."
    gh release create "${v}" "${APK_PATH}" --title "Release ${v}" --generate-notes --target "$(git rev-parse HEAD)"
    echo "✅ Release ${v} created successfully!"
    echo "🌐 View release at: https://github.com/$(gh repo view --json owner,name -q '.owner.login + "/" + .name')/releases/tag/${v}"

release-draft:
    #!/usr/bin/env bash
    set -euo pipefail
    cd "$(git rev-parse --show-toplevel)"
    v=$(git rev-parse --short HEAD)
    echo "🚀 Creating draft GitHub release"
    echo "🏷️  Version (git shorthash): ${v}"
    just check-github-cli
    just check-github-auth
    echo "🧹 Cleaning previous builds..."
    just android-clean
    echo "🤖 Building Android APK..."
    cd android && ./gradlew assembleRelease
    cd "$(git rev-parse --show-toplevel)"
    APK_PATH="android/app/build/outputs/apk/release/app-release.apk"
    echo "📋 APK location: ${APK_PATH}"
    if [[ ! -f "${APK_PATH}" ]]; then
        echo "❌ APK not found. Build may have failed."
        exit 1
    fi
    echo "✅ APK found, creating draft release..."
    gh release create "${v}" "${APK_PATH}" --title "Release ${v}" --generate-notes --draft --target "$(git rev-parse HEAD)"
    echo "✅ Draft release ${v} created successfully!"
    echo "🌐 View draft at: https://github.com/$(gh repo view --json owner,name -q '.owner.login + "/" + .name')/releases/tag/${v}"

release-prerelease:
    #!/usr/bin/env bash
    set -euo pipefail
    cd "$(git rev-parse --show-toplevel)"
    v=$(git rev-parse --short HEAD)
    echo "🚀 Creating prerelease GitHub release"
    echo "🏷️  Version (git shorthash): ${v}"
    just check-github-cli
    just check-github-auth
    echo "🧹 Cleaning previous builds..."
    just android-clean
    echo "🤖 Building Android APK..."
    cd android && ./gradlew assembleRelease
    cd "$(git rev-parse --show-toplevel)"
    APK_PATH="android/app/build/outputs/apk/release/app-release.apk"
    echo "📋 APK location: ${APK_PATH}"
    if [[ ! -f "${APK_PATH}" ]]; then
        echo "❌ APK not found. Build may have failed."
        exit 1
    fi
    echo "✅ APK found, creating prerelease..."
    gh release create "${v}" "${APK_PATH}" --title "Prerelease ${v}" --generate-notes --prerelease --target "$(git rev-parse HEAD)"
    echo "✅ Prerelease ${v} created successfully!"
    echo "🌐 View prerelease at: https://github.com/$(gh repo view --json owner,name -q '.owner.login + "/" + .name')/releases/tag/${v}"

# GitHub CLI helpers
check-github-cli:
    #!/usr/bin/env bash
    echo "🔍 Checking GitHub CLI installation..."
    if ! command -v gh > /dev/null; then
        echo "❌ GitHub CLI not found. Attempting installation..."
        just install-github-cli
    fi
    if ! command -v gh > /dev/null; then
        echo "❌ Failed to install GitHub CLI. Please install manually: https://cli.github.com/"
        exit 1
    fi
    echo "✅ GitHub CLI found"

check-github-auth:
    #!/usr/bin/env bash
    echo "🔐 Checking GitHub authentication..."
    if ! gh auth status > /dev/null 2>&1; then
        echo "❌ Not authenticated with GitHub. Please run:"
        echo "   gh auth login"
        exit 1
    fi
    echo "✅ GitHub authentication verified"

# GitHub CLI setup helper
github-setup:
    @echo "🔧 Setting up GitHub CLI..."
    @echo "📋 Installing GitHub CLI..."
    @if ! command -v gh >/dev/null 2>&1; then \
        if command -v port >/dev/null 2>&1; then \
            echo "⚙️  Using MacPorts to install GitHub CLI..."; \
            sudo port -N install gh; \
        elif command -v brew >/dev/null 2>&1; then \
            echo "🍺 Using Homebrew to install GitHub CLI..."; \
            brew install gh; \
        else \
            echo "❌ Neither MacPorts nor Homebrew found. Install GitHub CLI manually:"; \
            echo "   👉 https://cli.github.com/"; \
            exit 1; \
        fi; \
    fi
    @echo "🔐 Authenticating with GitHub..."
    @echo "📋 Please follow the prompts to authenticate:"
    gh auth login
    @echo "✅ GitHub CLI setup complete!"

# GitHub Actions logs
github-logs:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "📋 Fetching latest GitHub Actions logs for android-build workflow..."
    just check-github-cli
    just check-github-auth

    # Get the latest run for android-build workflow
    echo "🔍 Finding latest android-build workflow run..."
    RUN_ID=$(gh run list --workflow=android-build.yml --limit=1 --json databaseId --jq '.[0].databaseId')

    if [[ -z "$RUN_ID" || "$RUN_ID" == "null" ]]; then
        echo "❌ No android-build workflow runs found"
        echo "💡 Make sure you have a .github/workflows/android-build.yml file and at least one run"
        exit 1
    fi

    echo "📋 Latest run ID: $RUN_ID"
    echo "🌐 Opening logs in browser..."
    gh run view "$RUN_ID" --log

    echo ""
    echo "💡 To follow logs in real-time, use: just github-logs-follow"
    echo "💡 To watch a run until completion, use: just github-logs-watch"
    echo "💡 To follow logs for current job, use: just github-logs-job"
    echo "💡 To follow logs for specific step, use: just github-logs-step [step-name]"
    echo "💡 To check job status, use: just github-logs-job-status"

github-logs-follow:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "📋 Following latest GitHub Actions logs for android-build workflow..."
    just check-github-cli
    just check-github-auth

    # Get the latest run for android-build workflow
    echo "🔍 Finding latest android-build workflow run..."
    RUN_ID=$(gh run list --workflow=android-build.yml --limit=1 --json databaseId --jq '.[0].databaseId')

    if [[ -z "$RUN_ID" || "$RUN_ID" == "null" ]]; then
        echo "❌ No android-build workflow runs found"
        echo "💡 Make sure you have a .github/workflows/android-build.yml file and at least one run"
        exit 1
    fi

    echo "📋 Following logs for run ID: $RUN_ID"
    echo "🔄 Press Ctrl+C to stop following logs"
    echo "ℹ️  If the run is still in progress, logs will stream in real-time"
    gh run watch "$RUN_ID"

github-logs-job:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "📋 Following logs for the currently running job in android-build workflow..."
    just check-github-cli
    just check-github-auth

    # Get the latest run for android-build workflow
    echo "🔍 Finding latest android-build workflow run..."
    RUN_ID=$(gh run list --workflow=android-build.yml --limit=1 --json databaseId --jq '.[0].databaseId')

    if [[ -z "$RUN_ID" || "$RUN_ID" == "null" ]]; then
        echo "❌ No android-build workflow runs found"
        echo "💡 Make sure you have a .github/workflows/android-build.yml file and at least one run"
        exit 1
    fi

    echo "🔍 Finding the currently running job..."
    # Get the first job (there's only one job in our workflow: build-release)
    JOB_ID=$(gh run view "$RUN_ID" --json jobs --jq '.jobs[0].databaseId')
    JOB_NAME=$(gh run view "$RUN_ID" --json jobs --jq '.jobs[0].name')

    if [[ -z "$JOB_ID" || "$JOB_ID" == "null" ]]; then
        echo "❌ No jobs found for run $RUN_ID"
        exit 1
    fi

    echo "📋 Following logs for job: $JOB_NAME (ID: $JOB_ID)"
    echo "🔄 This will show logs for the current step"
    echo "🔄 Press Ctrl+C to stop following logs"
    echo ""

    # Check if job is still in progress
    JOB_STATUS=$(gh run view "$RUN_ID" --json jobs --jq '.jobs[0].status')
    if [[ "$JOB_STATUS" == "in_progress" ]]; then
        echo "⏳ Job is still in progress. Waiting for completion..."
        echo "🔄 You can watch the overall progress with: just github-logs-watch"
        echo "🔄 Or check status periodically with: just github-logs-job-status"
        echo ""
        echo "📋 Current job status: $JOB_STATUS"
        echo "🔄 Will show logs once the job completes..."

        # Wait for job to complete, then show logs
        while [[ "$JOB_STATUS" == "in_progress" ]]; do
            sleep 10
            JOB_STATUS=$(gh run view "$RUN_ID" --json jobs --jq '.jobs[0].status')
            echo "⏳ Job status: $JOB_STATUS (checking every 10 seconds...)"
        done

        echo "✅ Job completed! Showing logs..."
    fi

    # Show the logs
    gh run view --job="$JOB_ID" --log

github-logs-step:
    #!/usr/bin/env bash
    set -euo pipefail
    STEP_NAME="${1:-Build Release APK}"
    echo "📋 Following logs for step: '$STEP_NAME' in android-build workflow..."
    just check-github-cli
    just check-github-auth

    # Get the latest run for android-build workflow
    echo "🔍 Finding latest android-build workflow run..."
    RUN_ID=$(gh run list --workflow=android-build.yml --limit=1 --json databaseId --jq '.[0].databaseId')

    if [[ -z "$RUN_ID" || "$RUN_ID" == "null" ]]; then
        echo "❌ No android-build workflow runs found"
        echo "💡 Make sure you have a .github/workflows/android-build.yml file and at least one run"
        exit 1
    fi

    echo "🔍 Finding job containing step: '$STEP_NAME'..."
    # Get the first job (there's only one job in our workflow: build-release)
    JOB_ID=$(gh run view "$RUN_ID" --json jobs --jq '.jobs[0].databaseId')
    JOB_NAME=$(gh run view "$RUN_ID" --json jobs --jq '.jobs[0].name')

    if [[ -z "$JOB_ID" || "$JOB_ID" == "null" ]]; then
        echo "❌ No jobs found for run $RUN_ID"
        exit 1
    fi

    echo "📋 Following logs for job: $JOB_NAME (contains step: '$STEP_NAME')"
    echo "🔄 This will show logs for the current step"
    echo "🔄 Press Ctrl+C to stop following logs"
    echo "💡 Note: This follows the entire job - the step logs will appear when that step runs"
    echo ""

    # Check if job is still in progress
    JOB_STATUS=$(gh run view "$RUN_ID" --json jobs --jq '.jobs[0].status')
    if [[ "$JOB_STATUS" == "in_progress" ]]; then
        echo "⏳ Job is still in progress. Waiting for completion..."
        echo "🔄 You can watch the overall progress with: just github-logs-watch"
        echo "🔄 Or check status periodically with: just github-logs-job-status"
        echo ""
        echo "📋 Current job status: $JOB_STATUS"
        echo "🔄 Will show logs once the job completes..."

        # Wait for job to complete, then show logs
        while [[ "$JOB_STATUS" == "in_progress" ]]; do
            sleep 10
            JOB_STATUS=$(gh run view "$RUN_ID" --json jobs --jq '.jobs[0].status')
            echo "⏳ Job status: $JOB_STATUS (checking every 10 seconds...)"
        done

        echo "✅ Job completed! Showing logs..."
    fi

    # Show the logs
    gh run view --job="$JOB_ID" --log

github-logs-job-status:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "📋 Checking status of latest android-build workflow job..."
    just check-github-cli
    just check-github-auth

    # Get the latest run for android-build workflow
    echo "🔍 Finding latest android-build workflow run..."
    RUN_ID=$(gh run list --workflow=android-build.yml --limit=1 --json databaseId --jq '.[0].databaseId')

    if [[ -z "$RUN_ID" || "$RUN_ID" == "null" ]]; then
        echo "❌ No android-build workflow runs found"
        echo "💡 Make sure you have a .github/workflows/android-build.yml file and at least one run"
        exit 1
    fi

    echo "🔍 Getting job status..."
    JOB_STATUS=$(gh run view "$RUN_ID" --json jobs --jq '.jobs[0].status')
    JOB_NAME=$(gh run view "$RUN_ID" --json jobs --jq '.jobs[0].name')
    JOB_CONCLUSION=$(gh run view "$RUN_ID" --json jobs --jq '.jobs[0].conclusion // "N/A"')

    echo "📋 Job: $JOB_NAME"
    echo "📊 Status: $JOB_STATUS"
    echo "📊 Conclusion: $JOB_CONCLUSION"

    if [[ "$JOB_STATUS" == "in_progress" ]]; then
        echo "⏳ Job is currently running..."
        echo "🔄 To watch progress: just github-logs-watch"
        echo "🔄 To get logs when complete: just github-logs-job"
    elif [[ "$JOB_STATUS" == "completed" ]]; then
        echo "✅ Job has completed!"
        echo "🔄 To view logs: just github-logs-job"
    else
        echo "❓ Job status: $JOB_STATUS"
    fi

github-logs-watch:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "📋 Watching latest GitHub Actions run for android-build workflow..."
    just check-github-cli
    just check-github-auth

    # Get the latest run for android-build workflow
    echo "🔍 Finding latest android-build workflow run..."
    RUN_ID=$(gh run list --workflow=android-build.yml --limit=1 --json databaseId --jq '.[0].databaseId')

    if [[ -z "$RUN_ID" || "$RUN_ID" == "null" ]]; then
        echo "❌ No android-build workflow runs found"
        echo "💡 Make sure you have a .github/workflows/android-build.yml file and at least one run"
        exit 1
    fi

    echo "📋 Watching run ID: $RUN_ID"
    echo "🔄 This will show real-time progress and logs until completion"
    echo "🔄 Press Ctrl+C to stop watching"
    gh run watch "$RUN_ID"

github-logs-latest:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "📋 Getting latest GitHub Actions logs for android-build workflow..."
    just check-github-cli
    just check-github-auth

    # Get the latest run for android-build workflow
    echo "🔍 Finding latest android-build workflow run..."
    RUN_ID=$(gh run list --workflow=android-build.yml --limit=1 --json databaseId --jq '.[0].databaseId')

    if [[ -z "$RUN_ID" || "$RUN_ID" == "null" ]]; then
        echo "❌ No android-build workflow runs found"
        echo "💡 Make sure you have a .github/workflows/android-build.yml file and at least one run"
        exit 1
    fi

    echo "📋 Latest run ID: $RUN_ID"
    echo "📄 Full logs for latest run:"
    gh run view "$RUN_ID" --log

install-github-cli:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "🔧 Installing GitHub CLI..."
    if command -v gh >/dev/null 2>&1; then
        echo "✅ GitHub CLI already installed"
        exit 0
    fi
    if command -v port >/dev/null 2>&1; then
        echo "⚙️  Using MacPorts to install GitHub CLI..."
        sudo port -N install gh
    elif command -v brew >/dev/null 2>&1; then
        echo "🍺 Using Homebrew to install GitHub CLI..."
        brew install gh
    else
        echo "❌ Neither MacPorts nor Homebrew found. Please install GitHub CLI manually:" >&2
        echo "   👉 https://cli.github.com/" >&2
        exit 1
    fi
    echo "✅ GitHub CLI installed"

# 🆘 Help
help:
    @echo "🆘 Available commands:"
    @just --list
    @echo ""
    @echo "💡 Common workflows:"
    @echo "  just setup              - Initial setup"
    @echo "  just dev                - Start development"
    @echo "  just test-all           - Run all tests"
    @echo "  just lint-all           - Lint all code"
    @echo "  just clean              - Clean build artifacts"
    @echo "  just release            - Create GitHub release (tag = git shorthash)"
    @echo "  just release-draft      - Create draft release (tag = git shorthash)"
    @echo "  just github-setup       - Install and setup GitHub CLI"
    @echo "  just install-github-cli - Install GitHub CLI if missing"
    @echo "  just github-logs        - View latest GitHub Actions logs for android-build"
    @echo "  just github-logs-follow - Follow GitHub Actions logs in real-time (watch mode)"
    @echo "  just github-logs-watch  - Watch GitHub Actions run until completion"
    @echo "  just github-logs-job    - Follow logs for currently running job"
    @echo "  just github-logs-step   - Follow logs for specific step (default: 'Build Release APK')"
    @echo "  just github-logs-job-status - Check status of latest job"
    @echo "  just github-logs-latest - Get full logs for latest android-build run"
