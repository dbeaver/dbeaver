#!/bin/bash
###############################################################################
# DBeaver Build Script
# 
# Purpose:
#   Automates the build process for DBeaver by managing dependencies and running
#   the Maven build process. This script handles the cloning of dbeaver-common
#   repository and builds the project with all platforms configuration.
#
# Requirements:
#   - Git (for cloning repositories)
#   - Maven (for building the project)
#   - Bash shell environment
#
# Directory Structure Expected:
#   /workspace_root/
#     ├── dbeaver/           # Main DBeaver repository
#     │   ├── tools/        # Location of this script
#     │   │   └── build.sh
#     │   └── product/      # Product directory
#     │       └── aggregate # Build target directory
#     └── dbeaver-common/   # Will be cloned if not present
#
# Usage:
#   ./tools/build.sh
#
# Exit Codes:
#   0 - Success
#   1 - Missing dependencies or invalid directory structure
#
###############################################################################

# Exit on any error
set -e

###############################################################################
# Function Definitions
###############################################################################

# Get the directory where the script is located
# Returns: Absolute path to script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Function: log
# Purpose: Provides consistent logging format with timestamps
# Arguments:
#   $1 - Message to log
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

# Function: command_exists
# Purpose: Checks if a required command is available
# Arguments:
#   $1 - Command to check
# Returns:
#   0 if command exists, 1 if it doesn't
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

###############################################################################
# Dependency Checks
###############################################################################

# Verify required tools are installed
for cmd in git mvn; do
    if ! command_exists "$cmd"; then
        log "Error: $cmd is required but not installed"
        log "Please install $cmd and try again"
        exit 1
    fi
done

###############################################################################
# Path Definitions and Validation
###############################################################################

# Define paths relative to the script location
WORKSPACE_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"
DBEAVER_COMMON_DIR="${WORKSPACE_DIR}/dbeaver-common"
DBEAVER_JDBC_LIBSQL_DIR="${WORKSPACE_DIR}/dbeaver-jdbc-libsql"
PRODUCT_DIR="${SCRIPT_DIR}/../product"
AGGREGATE_DIR="${PRODUCT_DIR}/aggregate"

# Simple check for product directory
if [ ! -d "$PRODUCT_DIR" ]; then
    log "Error: Product directory not found at $PRODUCT_DIR"
    exit 1
fi

###############################################################################
# DBeaver Common Repository Management
###############################################################################

# Clone or verify dbeaver-common repository
if [ ! -d "$DBEAVER_COMMON_DIR" ]; then
    log "Cloning dbeaver-common repository..."
    git clone https://github.com/dbeaver/dbeaver-common.git "$DBEAVER_COMMON_DIR"
else
    log "DBeaver common directory already exists at $DBEAVER_COMMON_DIR"
fi

###############################################################################
# DBeaver Jdbc-Libsql Repository Management
###############################################################################

# Clone or verify dbeaver-jdbc-libsql repository
if [ ! -d "$DBEAVER_JDBC_LIBSQL_DIR" ]; then
    log "Cloning dbeaver-jdbc-libsql repository..."
    git clone https://github.com/dbeaver/dbeaver-jdbc-libsql.git "$DBEAVER_JDBC_LIBSQL_DIR"
else
    log "DBeaver jdbc-libsql directory already exists at $DBEAVER_JDBC_LIBSQL_DIR"
fi

###############################################################################
# Build Process
###############################################################################

# Execute Maven build
log "Starting Maven build..."

mvn clean install -Pall-platforms -T 1C -f "$AGGREGATE_DIR"

log "Build completed successfully"

###############################################################################
# End of Script
###############################################################################
