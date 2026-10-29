/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.app.standalone.update.install;

import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class WinZipInstaller implements PlatformInstaller {
    private static final String SCRIPT_NAME = "update-windows-zip.ps1";

    @Nullable
    private ProcessBuilder installerProcess;

    @Override
    public void prepare(@NotNull Path archive, @NotNull DBRProgressMonitor monitor) throws Exception {
        Path installation = RuntimeUtils.getLocalPathFromURL(Platform.getInstallLocation().getURL()).toRealPath();
        if (installation.getParent() == null) {
            throw new IOException("Cannot update an installation in a filesystem root");
        }
        String launcherProperty = System.getProperty("eclipse.launcher");
        if (launcherProperty == null) {
            throw new IOException("Could not locate the DBeaver launcher");
        }
        Path launcher = Path.of(launcherProperty).toRealPath();
        if (!installation.equals(launcher.getParent())) {
            throw new IOException("The launcher is outside the installation directory: " + launcher);
        }
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot == null) {
            throw new IOException("Could not locate Windows PowerShell");
        }
        Path powershell = Path.of(systemRoot, "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
        if (!Files.isRegularFile(powershell)) {
            throw new IOException("Windows PowerShell is not available: " + powershell);
        }

        // Neither the helper nor its log may live in the session temp folder, which is deleted during shutdown.
        Path folder = Files.createTempDirectory(archive.getParent(), "zip-update-");
        Path script = folder.resolve(SCRIPT_NAME);
        Path output = folder.resolve("update.log");
        try (var resource = WinZipInstaller.class.getResourceAsStream("/scripts/" + SCRIPT_NAME)) {
            if (resource == null) {
                throw new IOException("Missing Windows ZIP updater resource");
            }
            Files.copy(resource, script);
        }

        List<String> command = List.of(
            powershell.toString(), "-NoProfile", "-NonInteractive",
            // This applies only to our helper process, not the user's or machine's execution policy.
            "-ExecutionPolicy", "Bypass", "-File", script.toString()
        );
        ProcessBuilder builder = new ProcessBuilder(command)
            .directory(folder.toFile())
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(output.toFile()));
        var environment = builder.environment();
        // Keep paths out of PowerShell source and cmd.exe command strings.
        environment.put("DBEAVER_UPDATE_ARCHIVE", archive.toAbsolutePath().toString());
        environment.put("DBEAVER_UPDATE_HOME", installation.toString());
        environment.put("DBEAVER_UPDATE_LAUNCHER", launcher.getFileName().toString());
        environment.put("DBEAVER_UPDATE_WORKSPACE", DBWorkbench.getPlatform().getWorkspace().getAbsolutePath().toRealPath().toString());
        ProcessHandle current = ProcessHandle.current();
        StringBuilder processIds = new StringBuilder(Long.toString(current.pid()));
        current.parent().filter(parent -> parent.info().command()
            .map(path -> Path.of(path).equals(launcher)).orElse(false))
            .ifPresent(parent -> processIds.append(',').append(parent.pid()));
        environment.put("DBEAVER_UPDATE_PIDS", processIds.toString());

        // Validate/extract before requesting shutdown, so failures (including enforced script policies) are still reported in the UI.
        var prepareCommand = new ArrayList<>(command);
        prepareCommand.add("-Prepare");
        builder.command(prepareCommand);
        Process preparation = builder.start();
        try {
            while (!preparation.waitFor(200, TimeUnit.MILLISECONDS)) {
                if (monitor.isCanceled()) {
                    throw new InterruptedException();
                }
            }
            if (monitor.isCanceled()) {
                throw new InterruptedException();
            }
            if (preparation.exitValue() != 0) {
                throw new IOException("Could not prepare the ZIP update. See " + output);
            }
        } catch (InterruptedException e) {
            try {
                if (preparation.destroyForcibly().waitFor(5, TimeUnit.SECONDS)) {
                    IOUtils.deleteDirectory(folder);
                }
            } catch (IOException | InterruptedException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw e;
        } finally {
            if (preparation.isAlive()) {
                preparation.destroyForcibly();
            }
        }
        builder.command(command);
        installerProcess = builder;
    }

    @Override
    public void run(@NotNull Path executable, @NotNull Log log) throws IOException {
        if (installerProcess == null) {
            throw new IOException("The Windows ZIP update has not been prepared");
        }
        log.info("Starting Windows ZIP update; log: " + installerProcess.redirectOutput().file());
        // Do not wait here: the helper waits for this process to terminate before touching installation files.
        installerProcess.start();
    }
}
