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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.graphics.ImageData;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AIImageAttachment;
import org.jkiss.dbeaver.model.ai.AIImageDimensions;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

final class AIImageThumbnailBrowser {
    private static final Log log = Log.getLog(AIImageThumbnailBrowser.class);
    private static final String PNG_PREFIX = "data:image/png;base64,";
    private static final int LOAD_TIMEOUT_MS = 30_000;

    private final Browser browser;
    private final Queue<Request> requests = new ArrayDeque<>();
    @Nullable
    private Request active;
    @Nullable
    private Job preparationJob;
    @Nullable
    private Runnable timeout;
    private int nextId;
    private boolean ready;
    private boolean functionRegistered;

    AIImageThumbnailBrowser(@NotNull Browser browser) {
        this.browser = browser;
        browser.addProgressListener(ProgressListener.completedAdapter(event -> {
            if (!functionRegistered) {
                functionRegistered = true;
                registerFunction();
            }
            ready = true;
            loadNext();
        }));
        browser.addDisposeListener(event -> {
            cancelTimeout();
            cancelPreparation();
            requests.forEach(request -> request.result().cancel(false));
            requests.clear();
            if (active != null) {
                active.result().cancel(false);
            }
            active = null;
        });
    }

    private void registerFunction() {
        new BrowserFunction(browser, "imageThumbnailLoaded") {
            @Nullable
            @Override
            public Object function(@NotNull Object[] arguments) {
                if (active != null && arguments.length == 2 && arguments[0] instanceof Number id
                    && id.intValue() == active.id() && arguments[1] instanceof String source) {
                    complete(source);
                }
                return null;
            }
        };
    }

    @NotNull
    CompletableFuture<Thumbnail> load(@NotNull AIImageAttachment image, int size, @NotNull BooleanSupplier canceled) {
        CompletableFuture<Thumbnail> result = new CompletableFuture<>();
        if (browser.isDisposed() || canceled.getAsBoolean()) {
            result.cancel(false);
            return result;
        }
        requests.removeIf(request -> {
            if (request.result().isCancelled() || request.canceled().getAsBoolean()) {
                request.result().cancel(false);
                return true;
            }
            return false;
        });
        requests.add(new Request(nextId++, image, size, canceled, result));
        loadNext();
        return result;
    }

    private void loadNext() {
        if (browser.isDisposed() || !ready || active != null) {
            return;
        }
        while (!requests.isEmpty()) {
            Request request = requests.remove();
            if (request.result().isCancelled() || request.canceled().getAsBoolean()) {
                request.result().cancel(false);
                continue;
            }
            active = request;
            prepare(request);
            return;
        }
    }

    private void prepare(@NotNull Request request) {
        preparationJob = new Job(AIChatMessagesUI.ai_chat_image_loading) {
            @NotNull
            @Override
            protected IStatus run(@NotNull IProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                try {
                    String source = AIImageThumbnail.toWebpSource(request.image().getBytes());
                    String arguments = JSONUtils.GSON.toJson(Map.of(
                        "id", request.id(), "src", source, "size", request.size(), "timeout", LOAD_TIMEOUT_MS));
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    UIUtils.asyncExec(() -> {
                        if (!browser.isDisposed() && active == request && !monitor.isCanceled()) {
                            execute(request, arguments);
                        }
                    });
                } catch (IOException | SWTException | IllegalArgumentException exception) {
                    UIUtils.asyncExec(() -> {
                        if (!browser.isDisposed() && active == request) {
                            complete("", exception);
                        }
                    });
                }
                return Status.OK_STATUS;
            }
        };
        preparationJob.setRule(AIImageThumbnail.DECODING_RULE);
        preparationJob.setSystem(true);
        preparationJob.schedule();
    }

    private void execute(@NotNull Request request, @NotNull String arguments) {
        if (request.result().isCancelled() || request.canceled().getAsBoolean()) {
            complete("");
            return;
        }
        timeout = () -> {
            if (!browser.isDisposed() && active == request) {
                complete("");
            }
        };
        browser.getDisplay().timerExec(LOAD_TIMEOUT_MS, timeout);
        try {
            if (!browser.execute("loadImageThumbnail(" + arguments + ");")) {
                complete("");
            }
        } catch (SWTException exception) {
            complete("", exception);
        }
    }

    private void complete(@NotNull String source) {
        complete(source, null);
    }

    private void complete(@NotNull String source, @Nullable Exception failure) {
        cancelTimeout();
        cancelPreparation();
        Request request = active;
        active = null;
        try {
            if (request == null) {
                return;
            }
            if (request.result().isCancelled() || request.canceled().getAsBoolean()) {
                request.result().cancel(false);
                return;
            }
            if (failure != null) {
                log.debug("Cannot prepare browser image thumbnail", failure);
                request.result().completeExceptionally(failure);
                return;
            }
            if (!source.startsWith(PNG_PREFIX) || source.length() > (long) request.size() * request.size() * 8 + 1024) {
                throw new IOException("Cannot decode browser image thumbnail.");
            }
            byte[] bytes = Base64.getDecoder().decode(source.substring(PNG_PREFIX.length()));
            AIImageDimensions dimensions = AIImageDimensions.read(bytes);
            if (dimensions.width() > request.size() || dimensions.height() > request.size()) {
                throw new IOException("Browser image thumbnail exceeds the requested size.");
            }
            request.result().complete(new Thumbnail(source, new ImageData(new ByteArrayInputStream(bytes))));
        } catch (IOException | SWTException | IllegalArgumentException exception) {
            log.debug("Cannot create browser image thumbnail", exception);
            if (request != null) {
                request.result().completeExceptionally(exception);
            }
        } finally {
            if (!browser.isDisposed()) {
                browser.getDisplay().asyncExec(this::loadNext);
            }
        }
    }

    private void cancelTimeout() {
        if (timeout != null) {
            browser.getDisplay().timerExec(-1, timeout);
            timeout = null;
        }
    }

    private void cancelPreparation() {
        if (preparationJob != null) {
            preparationJob.cancel();
            preparationJob = null;
        }
    }

    private record Request(
        int id,
        @NotNull AIImageAttachment image,
        int size,
        @NotNull BooleanSupplier canceled,
        @NotNull CompletableFuture<Thumbnail> result
    ) {
    }

    record Thumbnail(@NotNull String dataUrl, @NotNull ImageData imageData) {
    }
}
