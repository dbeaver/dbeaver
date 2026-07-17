@echo off
setlocal EnableExtensions

rem Local DBeaver CE build with Eclipse P2 mirror (workaround for download.eclipse.org failures).
rem Usage:
rem   build-local.cmd              - incremental package build
rem   build-local.cmd clean        - clean package build
rem   build-local.cmd clean-cache  - clear Tycho cache, then package build
rem   build-local.cmd clean-cache clean - clear cache and clean package build

set "scriptDir=%~dp0"
set "workspaceDir=%scriptDir%..\.."
set "dbeaverDir=%workspaceDir%\dbeaver"
set "ECLIPSE_MIRROR=https://mirror.umd.edu/eclipse/releases"
set "MAVEN_OPTS=-Dtycho.p2.httptransport.type=JavaUrl"

set "CLEAR_CACHE=0"
set "MAVEN_GOAL=package"

:parseArgs
if "%~1"=="" goto argsDone
if /I "%~1"=="clean-cache" set "CLEAR_CACHE=1"
if /I "%~1"=="clean" set "MAVEN_GOAL=clean package"
shift
goto parseArgs

:argsDone
for /f "tokens=2 delims=," %%P in ('tasklist /fi "imagename eq java.exe" /fo csv /nh 2^>nul') do (
    for /f "tokens=* delims= " %%Q in ("%%~P") do (
        wmic process where "ProcessId=%%Q" get CommandLine /format:list 2>nul | findstr /i /c:"maven" /c:"tycho" /c:"dbeaver\product\aggregate" >nul
        if not errorlevel 1 (
            echo ERROR: Another Maven build appears to be running ^(PID %%Q^).
            echo Stop it first, or run: taskkill /PID %%Q /F
            exit /b 1
        )
    )
)

if not exist "%workspaceDir%\dbeaver-common" (
    echo Cloning dbeaver-common...
    git clone https://github.com/dbeaver/dbeaver-common.git "%workspaceDir%\dbeaver-common"
    if errorlevel 1 exit /b 1
)

if "%CLEAR_CACHE%"=="1" (
    echo Clearing Tycho P2 cache...
    if exist "%USERPROFILE%\.m2\repository\.cache\tycho" rmdir /s /q "%USERPROFILE%\.m2\repository\.cache\tycho"
)

echo Building DBeaver CE with Eclipse mirror: %ECLIPSE_MIRROR%
call "%workspaceDir%\dbeaver-common\mvnw.cmd" %MAVEN_GOAL% -Pproduct-dbeaver-ce,product-dbeaver-eclipse-ce -T 1C -f "%dbeaverDir%\product\aggregate" "-Declipse-repo-url=%ECLIPSE_MIRROR%"
set "BUILD_EXIT=%ERRORLEVEL%"

if "%BUILD_EXIT%"=="0" (
    echo.
    echo Build completed successfully.
    echo Run:
    echo   "%dbeaverDir%\product\community\target\products\org.jkiss.dbeaver.core.product\win32\win32\x86_64\dbeaver.exe"
) else (
    echo.
    echo Build failed with exit code %BUILD_EXIT%.
)

endlocal & exit /b %BUILD_EXIT%
