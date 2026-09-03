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
package com.dbeaver.db.cdata.registry;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

final class CDataProcessExecutor {
    private static final int MAX_OUTPUT_LENGTH = 64 * 1024;
    private static final long PROCESS_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(5);

    private CDataProcessExecutor() {
    }

    @NotNull
    static ProcessResult execute(
        @NotNull DBRProgressMonitor monitor,
        @NotNull List<String> command,
        @NotNull Path workingDirectory,
        @NotNull String operation,
        @NotNull List<PromptResponse> responses
    ) throws DBException {
        Process process = null;
        ExecutorService outputExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "CDATA process output");
            thread.setDaemon(true);
            return thread;
        });
        Future<String> output = null;
        OutputStreamWriter inputWriter = null;
        try {
            process = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true)
                .start();
            Process activeProcess = process;
            inputWriter = new OutputStreamWriter(
                process.getOutputStream(),
                Charset.forName(GeneralUtils.getDefaultConsoleEncoding())
            );
            if (responses.isEmpty()) {
                inputWriter.close();
            }
            OutputStreamWriter activeInputWriter = inputWriter;
            output = outputExecutor.submit(() -> readOutput(
                activeProcess.getInputStream(),
                responses.isEmpty() ? null : activeInputWriter,
                responses
            ));

            long deadline = System.nanoTime() + PROCESS_TIMEOUT_NANOS;
            while (!process.waitFor(200, TimeUnit.MILLISECONDS)) {
                if (monitor.isCanceled()) {
                    terminate(process);
                    throw new DBException(operation + " was canceled");
                }
                if (System.nanoTime() >= deadline) {
                    terminate(process);
                    throw new DBException(operation + " timed out");
                }
            }
            try {
                return new ProcessResult(process.exitValue(), output.get(5, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                terminate(process);
                throw new DBException("Unable to read " + operation + " result", e);
            }
        } catch (IOException e) {
            throw new DBException("Unable to start " + operation, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                terminate(process);
            }
            throw new DBException(operation + " was interrupted", e);
        } catch (ExecutionException e) {
            if (process != null) {
                terminate(process);
            }
            throw new DBException("Unable to read " + operation + " result", e);
        } finally {
            if (output != null && !output.isDone()) {
                output.cancel(true);
            }
            if (process != null) {
                try {
                    process.getInputStream().close();
                } catch (IOException ignored) {
                }
            }
            if (inputWriter != null) {
                try {
                    inputWriter.close();
                } catch (IOException ignored) {
                }
            }
            outputExecutor.shutdownNow();
        }
    }

    @NotNull
    private static String readOutput(
        @NotNull InputStream input,
        @Nullable OutputStreamWriter inputWriter,
        @NotNull List<PromptResponse> responses
    ) throws IOException {
        Charset charset = Charset.forName(GeneralUtils.getDefaultConsoleEncoding());
        StringBuilder output = new StringBuilder();
        int responseIndex = 0;
        int searchFrom = 0;
        try (var reader = new InputStreamReader(input, charset); inputWriter) {
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) >= 0) {
                if (output.length() < MAX_OUTPUT_LENGTH) {
                    output.append(buffer, 0, Math.min(count, MAX_OUTPUT_LENGTH - output.length()));
                }
                while (inputWriter != null && responseIndex < responses.size()) {
                    PromptResponse response = responses.get(responseIndex);
                    int promptPosition = findPrompt(output, response.prompt(), searchFrom, response.lineStart());
                    if (promptPosition < 0) {
                        break;
                    }
                    inputWriter.write(response.response());
                    inputWriter.write(System.lineSeparator());
                    inputWriter.flush();
                    searchFrom = promptPosition + response.prompt().length();
                    responseIndex++;
                }
            }
        }
        return output.toString();
    }

    private static int findPrompt(
        @NotNull StringBuilder output,
        @NotNull String prompt,
        int searchFrom,
        boolean lineStart
    ) {
        int position = output.indexOf(prompt, searchFrom);
        while (lineStart && position > 0 && output.charAt(position - 1) != '\n' && output.charAt(position - 1) != '\r') {
            position = output.indexOf(prompt, position + 1);
        }
        return position;
    }

    private static void terminate(@NotNull Process process) {
        List<ProcessHandle> descendants = process.toHandle().descendants().toList();
        descendants.forEach(ProcessHandle::destroy);
        process.destroy();
        try {
            process.waitFor(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        descendants.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
        if (process.isAlive()) {
            process.destroyForcibly();
        }
    }

    record ProcessResult(int exitCode, @NotNull String output) {
    }

    record PromptResponse(@NotNull String prompt, @NotNull String response, boolean lineStart) {
        PromptResponse(@NotNull String prompt, @NotNull String response) {
            this(prompt, response, false);
        }
    }
}
