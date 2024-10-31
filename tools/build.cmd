@echo off
setlocal enabledelayedexpansion
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: DBeaver Build Script for Windows
::
:: Purpose:
::   Automates the build process for DBeaver by managing dependencies and running
::   the Maven build process. This script handles the cloning of dbeaver-common
::   repository and builds the project with all platforms configuration.
::
:: Requirements:
::   - Git (for cloning repositories)
::   - Maven (for building the project)
::   - Windows Command Prompt
::
:: Directory Structure Expected:
::   /workspace_root/
::     ├── dbeaver/           # Main DBeaver repository
::     │   ├── tools/        # Location of this script
::     │   │   └── build.cmd
::     │   └── product/      # Product directory
::     │       └── aggregate # Build target directory
::     └── dbeaver-common/   # Will be cloned if not present
::
:: Usage:
::   .\tools\build.cmd
::
:: Exit Codes:
::   0 - Success
::   1 - Missing dependencies or invalid directory structure
::
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Function Definitions
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Function to log messages with timestamp
call :define_log_function

:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Dependency Checks
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Check for required commands
for %%x in (git mvn) do (
    where /q %%x
    if !errorlevel! neq 0 (
        call :log "Error: %%x is required but not installed"
        call :log "Please install %%x and try again"
        exit /b 1
    )
)

:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Path Definitions and Validation
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Get script directory and define paths
set "SCRIPT_DIR=%~dp0"
set "WORKSPACE_DIR=%SCRIPT_DIR%..\..\"
set "DBEAVER_COMMON_DIR=%WORKSPACE_DIR%..\dbeaver-common"
set "DBEAVER_JDBC_LIBSQL_DIR=%WORKSPACE_DIR%..\dbeaver-jdbc-libsql"
set "PRODUCT_DIR=%SCRIPT_DIR%..\product"
set "AGGREGATE_DIR=%PRODUCT_DIR%\aggregate"

:: Validate product directory exists
if not exist "%PRODUCT_DIR%" (
    call :log "Error: Product directory not found at %PRODUCT_DIR%"
    exit /b 1
)

:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: DBeaver Common Repository Management
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Clone or verify dbeaver-common repository
if not exist "%DBEAVER_COMMON_DIR%" (
    call :log "Cloning dbeaver-common repository..."
    git clone https://github.com/dbeaver/dbeaver-common.git "%DBEAVER_COMMON_DIR%"
) else (
    call :log "DBeaver common directory already exists at %DBEAVER_COMMON_DIR%"
)

:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: DBeaver Jdbc-Libsql Repository Management
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Clone or verify dbeaver-jdbc-libsql repository
if not exist "%DBEAVER_JDBC_LIBSQL_DIR%" (
    call :log "Cloning dbeaver-jdbc-libsql repository..."
    git clone https://github.com/dbeaver/dbeaver-jdbc-libsql.git "%DBEAVER_JDBC_LIBSQL_DIR%"
) else (
    call :log "DBeaver jdbc-libsql directory already exists at %DBEAVER_JDBC_LIBSQL_DIR%"
)

:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Build Process
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Execute Maven build
call :log "Starting Maven build..."
pushd "%AGGREGATE_DIR%"
call mvn clean install -Pall-platforms -T 1C
if !errorlevel! neq 0 (
    call :log "Error: Maven build failed"
    popd
    exit /b 1
)
popd
call :log "Build completed successfully"

exit /b 0

:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Function Implementations
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:define_log_function
:: Define the log function
set "TIMESTAMP_CMD=powershell -Command "$([DateTime]::Now.ToString('yyyy-MM-dd HH:mm:ss'))""
set "LOG_PREFIX=[%TIMESTAMP_CMD%]"
goto :eof

:log
:: Log message with timestamp
for /f "delims=" %%i in ('%TIMESTAMP_CMD%') do echo [%%i] %~1
goto :eof

:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: End of Script
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
