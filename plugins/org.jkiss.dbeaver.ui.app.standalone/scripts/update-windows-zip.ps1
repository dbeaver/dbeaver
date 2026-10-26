# DBeaver - Universal Database Manager
# Copyright (C) 2010-2026 DBeaver Corp and others
# Licensed under the Apache License, Version 2.0 (https://www.apache.org/licenses/LICENSE-2.0).

# Preparation runs while DBeaver is still open. Application runs in a detached process after accepted shutdown.
param([switch]$Prepare)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$updateLock = $null

function Move-UpdateItem([string]$Source, [string]$Destination) {
    if ([IO.Directory]::Exists($Source)) {
        [IO.Directory]::Move($Source, $Destination)
    } else {
        [IO.File]::Move($Source, $Destination)
    }
}

function Invoke-UpdateOperation([scriptblock]$Operation) {
    # The JVM/native launcher and antivirus may retain file locks briefly after workbench shutdown.
    for ($attempt = 0; ; $attempt++) {
        try {
            & $Operation
            return
        } catch {
            if ($attempt -ge 10) { throw }
            Start-Sleep -Seconds 1
        }
    }
}

function Get-UpdateEntries([string]$Payload) {
    # Replace managed directories as units, including ones no longer present in the new distribution.
    # Other top-level entries (e.g. a portable workspace) are not part of the replacement.
    @('configuration', 'plugins', 'features', 'jre', 'p2', 'licenses', 'qmdb', 'drivers', 'maven-local',
        '.eclipseproduct', 'artifacts.xml') + @(Get-ChildItem -LiteralPath $Payload -Force | ForEach-Object { $_.Name }) |
        Sort-Object -Unique
}

function Get-UpdateLauncherName([string]$Launcher) {
    $name = [IO.Path]::GetFileNameWithoutExtension($Launcher)
    # The console launcher shares dbeaver.ini with the GUI launcher; there is no dbeaverc.ini.
    if ($name -eq 'dbeaverc') { return 'dbeaver' }
    return $name
}

function Assert-UpdateLayout([string]$Payload, [string]$Installation, [string]$Launcher, [string]$Workspace) {
    $ini = (Get-UpdateLauncherName $Launcher) + '.ini'
    if (![IO.File]::Exists((Join-Path $Payload $Launcher)) -or
        ![IO.Directory]::Exists((Join-Path $Payload 'plugins')) -or
        ![IO.File]::Exists((Join-Path $Payload $ini))) {
        throw 'The archive does not contain the expected DBeaver distribution.'
    }
    foreach ($name in (Get-UpdateEntries $Payload)) {
        $target = Join-Path $Installation $name
        if ($Workspace.Equals($target, [StringComparison]::OrdinalIgnoreCase) -or
            $Workspace.StartsWith($target + '\', [StringComparison]::OrdinalIgnoreCase)) {
            throw "The workspace is inside an application directory that would be replaced: $target"
        }
        if (Test-Path -LiteralPath $target) {
            $item = Get-Item -LiteralPath $target -Force
            if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) {
                throw "Cannot replace a linked application directory or file: $target"
            }
        }
    }
}

function Expand-UpdateArchive([string]$Archive, [string]$Destination) {
    Write-Output 'Extracting the update.'
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($Archive)
    try {
        # Reject absolute paths, traversal, alternate data streams and symbolic links before extracting anything.
        foreach ($entry in $zip.Entries) {
            if ([IO.Path]::IsPathRooted($entry.FullName) -or $entry.FullName.Contains(':') -or
                (($entry.ExternalAttributes -shr 16) -band 0xF000) -eq 0xA000) {
                throw "Invalid archive entry: $($entry.FullName)"
            }
            $path = [IO.Path]::GetFullPath((Join-Path $Destination $entry.FullName))
            if (!$path.StartsWith($Destination + '\', [StringComparison]::OrdinalIgnoreCase)) {
                throw "Invalid archive entry: $($entry.FullName)"
            }
        }
        [IO.Compression.ZipFileExtensions]::ExtractToDirectory($zip, $Destination)
    } finally {
        $zip.Dispose()
    }
}

function Install-UpdatePayload([string]$Payload, [string]$Installation, [string]$Backup) {
    [IO.Directory]::CreateDirectory($Backup) | Out-Null
    $saved = [Collections.Generic.List[string]]::new()
    $installed = [Collections.Generic.List[string]]::new()
    try {
        Write-Output "Backing up application files to $Backup"
        foreach ($name in (Get-UpdateEntries $Payload)) {
            $target = Join-Path $Installation $name
            if (Test-Path -LiteralPath $target) {
                Invoke-UpdateOperation { Move-UpdateItem $target (Join-Path $Backup $name) }
                $saved.Add($name)
            }
        }

        Write-Output 'Replacing application files.'
        foreach ($item in (Get-ChildItem -LiteralPath $Payload -Force)) {
            # Register before copying so rollback also removes a partially copied directory.
            $installed.Add($item.Name)
            Copy-Item -LiteralPath $item.FullName -Destination (Join-Path $Installation $item.Name) -Recurse -Force
        }
    } catch {
        $failure = $_
        Write-Output "Replacement failed; restoring the backup: $failure"
        $restoreFailures = [Collections.Generic.List[string]]::new()
        foreach ($name in $installed) {
            $target = Join-Path $Installation $name
            try {
                Invoke-UpdateOperation {
                    if (Test-Path -LiteralPath $target) {
                        Remove-Item -LiteralPath $target -Recurse -Force
                    }
                }
            } catch {
                $restoreFailures.Add($name)
                Write-Output "Cannot remove the incomplete update entry ${name}: $_"
            }
        }
        foreach ($name in $saved) {
            try {
                Invoke-UpdateOperation { Move-UpdateItem (Join-Path $Backup $name) (Join-Path $Installation $name) }
            } catch {
                $restoreFailures.Add($name)
                Write-Output "Cannot restore ${name}: $_"
            }
        }
        if ($restoreFailures.Count -gt 0) {
            throw "Update failed. Manual recovery is required for $($restoreFailures -join ', '). Backup: ${Backup}. Original error: $failure"
        }
        Write-Output 'Previous application files restored.'
        throw $failure
    }
}

function Remove-UpdateStaging([string]$Archive, [string]$Extracted) {
    # Installation and relaunch have succeeded; leftover temporary files must not turn success into failure.
    foreach ($path in @($Archive, $Extracted)) {
        try {
            Remove-Item -LiteralPath $path -Recurse -Force
        } catch {
            Write-Warning "Could not remove update staging '${path}': $_"
        }
    }
}

try {
    foreach ($name in @('ARCHIVE', 'HOME', 'LAUNCHER', 'WORKSPACE', 'PIDS')) {
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable('DBEAVER_UPDATE_' + $name))) {
            throw "Missing update parameter: $name"
        }
    }
    $archive = [IO.Path]::GetFullPath($env:DBEAVER_UPDATE_ARCHIVE)
    $installation = [IO.Path]::GetFullPath($env:DBEAVER_UPDATE_HOME)
    if ($installation -eq [IO.Path]::GetPathRoot($installation)) {
        throw 'Cannot update an installation in a filesystem root.'
    }
    $installation = $installation.TrimEnd('\')
    $workspace = [IO.Path]::GetFullPath($env:DBEAVER_UPDATE_WORKSPACE).TrimEnd('\')
    $launcher = $env:DBEAVER_UPDATE_LAUNCHER
    if ([IO.Path]::GetFileName($launcher) -ne $launcher -or ![IO.File]::Exists((Join-Path $installation $launcher))) {
        throw 'Invalid installation launcher.'
    }
    if ($PSScriptRoot.Equals($installation, [StringComparison]::OrdinalIgnoreCase) -or
        $PSScriptRoot.StartsWith($installation + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Update staging must be outside the installation directory.'
    }
    $extracted = Join-Path $PSScriptRoot 'payload'

    if ($Prepare) {
        Expand-UpdateArchive $archive $extracted
    }

    # Windows distributions contain one product directory; the user's installation directory can have any name.
    $roots = @(Get-ChildItem -LiteralPath $extracted -Force)
    if ($roots.Count -ne 1 -or !$roots[0].PSIsContainer) {
        throw 'Unexpected ZIP directory layout.'
    }
    $payload = $roots[0].FullName
    Assert-UpdateLayout $payload $installation $launcher $workspace

    if ($Prepare) {
        # Check write access without changing application files.
        $probe = Join-Path $installation ('.update-check-' + [Guid]::NewGuid().ToString('N'))
        [IO.Directory]::CreateDirectory($probe) | Out-Null
        [IO.Directory]::Delete($probe)
        Write-Output 'Update prepared.'
        exit 0
    }

    Write-Output 'Waiting for DBeaver to exit.'
    foreach ($processId in $env:DBEAVER_UPDATE_PIDS.Split(',')) {
        $process = Get-Process -Id ([int]$processId) -ErrorAction SilentlyContinue
        if ($null -ne $process -and !$process.WaitForExit(120000)) {
            throw 'DBeaver did not exit in time. The installation has not been changed.'
        }
    }

    # Fail fast if another updater is already replacing this installation.
    $lockPath = Join-Path $installation '.update.lock'
    $updateLock = [IO.File]::Open($lockPath, [IO.FileMode]::OpenOrCreate, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
    # A different workspace can have another instance running from the same installation.
    $launcherName = Get-UpdateLauncherName $launcher
    foreach ($running in (Get-Process -Name @($launcherName, ($launcherName + 'c'), 'java', 'javaw') -ErrorAction SilentlyContinue)) {
        $executable = $running.Path
        if ($executable -and $executable.StartsWith($installation + '\', [StringComparison]::OrdinalIgnoreCase)) {
            throw "Another process is using this installation: $($running.Id). Close it before updating."
        }
    }
    $backup = Join-Path $installation ('.update-backup-' + [Guid]::NewGuid().ToString('N'))
    Install-UpdatePayload $payload $installation $backup

    Write-Output 'Starting DBeaver.'
    # Appending \. avoids a trailing backslash escaping the closing quote in Windows command-line parsing.
    $arguments = '-data "' + $workspace + '\."'
    # Start-Process interprets wildcard characters in WorkingDirectory. The .NET API treats all paths literally.
    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = Join-Path $installation $launcher
    $startInfo.Arguments = $arguments
    $startInfo.WorkingDirectory = $installation
    $startInfo.UseShellExecute = $false
    try {
        [Diagnostics.Process]::Start($startInfo).Dispose()
    } catch {
        throw "The update was installed, but DBeaver could not be restarted. Previous application files: ${backup}. Error: $_"
    }
    # Keep the archive and staging when replacement or relaunch fails, along with the backup and redirected log.
    Remove-UpdateStaging $archive $extracted
    Write-Output "Update complete. Previous application files are retained in $backup"
    exit 0
} catch {
    Write-Output "ZIP update failed: $_"
    exit 1
} finally {
    if ($null -ne $updateLock) {
        $updateLock.Dispose()
        Remove-Item -LiteralPath $lockPath -Force -ErrorAction SilentlyContinue
    }
}
