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

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIImageAttachment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

// native SWT tests need a display and the macOS main thread
@EnabledIfEnvironmentVariable(named = "DBEAVER_SWT_TESTS", matches = "true")
class AIImageThumbnailBrowserTest {
    private static final int TIMEOUT_MS = 20_000;
    private static final String LOSSLESS = "UklGRhwAAABXRUJQVlA4TA8AAAAvH8ADAAcQ/Y/+ByKi/wEA";
    private static final String TRANSPARENT = "UklGRhwAAABXRUJQVlA4TA8AAAAvH8ADEAcQ/Y8CBSKi/wEA";

    private static final String WIDE =
        "UklGRooCAABXRUJQVlA4WAoAAAAgAAAA/wsADwAASUNDUMgBAAAAAAHIAAAAAAQwAABtbnRyUkdCIFhZWiAH4AABAAEAAAAAAABhY3NwAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAA9tYAAQAAAADTLQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            + "AAlkZXNjAAAA8AAAACRyWFlaAAABFAAAABRnWFlaAAABKAAAABRiWFlaAAABPAAAABR3dHB0AAABUAAAABRyVFJDAAABZAAAAChnVFJDAAABZAAA"
            + "AChiVFJDAAABZAAAAChjcHJ0AAABjAAAADxtbHVjAAAAAAAAAAEAAAAMZW5VUwAAAAgAAAAcAHMAUgBHAEJYWVogAAAAAAAAb6IAADj1AAADkFhZ"
            + "WiAAAAAAAABimQAAt4UAABjaWFlaIAAAAAAAACSgAAAPhAAAts9YWVogAAAAAAAA9tYAAQAAAADTLXBhcmEAAAAAAAQAAAACZmYAAPKnAAANWQAA"
            + "E9AAAApbAAAAAAAAAABtbHVjAAAAAAAAAAEAAAAMZW5VUwAAACAAAAAcAEcAbwBvAGcAbABlACAASQBuAGMALgAgADIAMAAxADZWUDggnAAAAPAN"
            + "AJ0BKgAMEAA+bTaZSaQjIqEgKACADYlpbuF2sR7cB+AAABPYB77ZOQ99snIe+2TkPfbJyHvtk5D32ych77ZOQ99snIe+2TkPfbJyHvtk5D32ych7"
            + "7ZOQ99snIe+2TkPfbJyHvtk5D32ych77ZOQ99snIe+2TEAD+/3A9///5BcsLrka///kB/yA/5Af/x8U02nAgAAAAAAAAAA==";

    private Display display;
    private Shell shell;
    private Browser browser;

    @BeforeEach
    void createBrowser() {
        display = new Display();
        shell = new Shell(display);
        browser = new Browser(shell, SWT.NONE);
        browser.setSize(1, 1);
    }

    @AfterEach
    void disposeBrowser() {
        if (shell != null && !shell.isDisposed()) {
            shell.dispose();
        }
        if (display != null && !display.isDisposed()) {
            display.dispose();
        }
    }

    @Test
    void queuesAttachmentReferencesWithoutPreparingDataUrls() throws Exception {
        AIImageThumbnailBrowser decoder = new AIImageThumbnailBrowser(browser);
        byte[] original = Base64.getDecoder().decode(LOSSLESS);
        int padding = 1024 * 1024;
        byte[] bytes = new byte[original.length + 8 + padding];
        System.arraycopy(original, 0, bytes, 0, original.length);
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(4, bytes.length - 8);
        ByteBuffer.wrap(bytes, original.length, 8).order(ByteOrder.LITTLE_ENDIAN)
            .put(new byte[]{'J', 'U', 'N', 'K'}).putInt(padding);
        AIImageAttachment image = AIImageAttachment.fromBytes("large.webp", bytes);
        List<CompletableFuture<AIImageThumbnailBrowser.Thumbnail>> results = new ArrayList<>();
        for (int index = 0; index < 50; index++) {
            results.add(decoder.load(image, 128, () -> false));
        }
        Field requestsField = AIImageThumbnailBrowser.class.getDeclaredField("requests");
        requestsField.setAccessible(true);
        Queue<?> requests = (Queue<?>) requestsField.get(decoder);
        Assertions.assertEquals(50, requests.size());
        for (Object request : requests) {
            Field imageField = request.getClass().getDeclaredField("image");
            imageField.setAccessible(true);
            Assertions.assertSame(image, imageField.get(request));
            for (Field field : request.getClass().getDeclaredFields()) {
                Assertions.assertNotEquals(String.class, field.getType());
            }
        }
        Field preparationField = AIImageThumbnailBrowser.class.getDeclaredField("preparationJob");
        preparationField.setAccessible(true);
        Assertions.assertNull(preparationField.get(decoder));
        browser.dispose();
        results.forEach(result -> Assertions.assertTrue(result.isCancelled()));
    }

    @Test
    void continuesAfterDecodeFailureAndSkipsCanceledRequests() throws Exception {
        AIImageThumbnailBrowser decoder = new AIImageThumbnailBrowser(browser);
        CompletableFuture<AIImageThumbnailBrowser.Thumbnail> failed = decoder.load(corruptImage(), 8, () -> false);
        AtomicBoolean cancel = new AtomicBoolean();
        CompletableFuture<AIImageThumbnailBrowser.Thumbnail> canceled = decoder.load(image(LOSSLESS), 8, cancel::get);
        CompletableFuture<AIImageThumbnailBrowser.Thumbnail> loaded = decoder.load(image(TRANSPARENT), 8, () -> false);
        cancel.set(true);
        loadThumbnailScript();
        waitFor(loaded);
        Assertions.assertThrows(CompletionException.class, failed::join);
        Assertions.assertTrue(canceled.isCancelled());
        Assertions.assertEquals(8, loaded.join().imageData().width);
        Assertions.assertEquals(4, loaded.join().imageData().height);
        Assertions.assertEquals(64, loaded.join().imageData().getAlpha(0, 0));
    }

    @Test
    void fullPreviewReportsBrowserDecodeFailure() throws Exception {
        CompletableFuture<Void> failed = new CompletableFuture<>();
        registerPreviewError(failed);
        browser.setText(AIImageAttachmentViewer.createImageHtml(AIImageThumbnail.toWebpSource(corruptImage().getBytes())));
        waitFor(failed);
    }

    @Test
    void fullPreviewLoadsValidWebpWithoutReportingError() {
        CompletableFuture<Void> failed = new CompletableFuture<>();
        CompletableFuture<Void> loaded = new CompletableFuture<>();
        registerPreviewError(failed);
        browser.addProgressListener(ProgressListener.completedAdapter(event -> loaded.complete(null)));
        browser.setText(AIImageAttachmentViewer.createImageHtml(image(LOSSLESS).toDataUrl()));
        waitFor(loaded);
        Assertions.assertFalse(failed.isDone());
        Assertions.assertEquals(32, ((Number) browser.evaluate("return document.images[0].naturalWidth;")).intValue());
    }

    @Test
    void limitsWideWebpThumbnailDimensions() throws Exception {
        AIImageThumbnailBrowser decoder = new AIImageThumbnailBrowser(browser);
        CompletableFuture<AIImageThumbnailBrowser.Thumbnail> loaded = decoder.load(image(WIDE), 128, () -> false);
        loadThumbnailScript();
        waitFor(loaded);
        Assertions.assertEquals(128, loaded.join().imageData().width);
        Assertions.assertEquals(1, loaded.join().imageData().height);
        Assertions.assertTrue(loaded.join().dataUrl().startsWith("data:image/png;base64,"));
    }

    @Test
    void limitsFullWebpPreviewRasterDimensions() throws Exception {
        CompletableFuture<Void> failed = new CompletableFuture<>();
        CompletableFuture<int[]> loaded = new CompletableFuture<>();
        registerPreviewError(failed);
        new BrowserFunction(browser, "imagePreviewLoaded") {
            @Nullable
            @Override
            public Object function(@NotNull Object[] arguments) {
                loaded.complete(new int[]{((Number) arguments[0]).intValue(), ((Number) arguments[1]).intValue()});
                return null;
            }
        };
        String html = AIImageAttachmentViewer.createImageHtml(AIImageThumbnail.toWebpSource(image(WIDE).getBytes()));
        browser.setText(html.replace("onload='resizeImage(this)'", "onload='resizeImage(this); "
            + "if (this.complete && this.naturalWidth > 0 && this.src.startsWith(\"data:image/png;\")) "
            + "imagePreviewLoaded(this.naturalWidth, this.naturalHeight)'"));
        waitFor(loaded);
        Assertions.assertFalse(failed.isDone());
        Assertions.assertArrayEquals(new int[]{AIImageAttachmentViewer.MAX_DISPLAY_SIZE, 11}, loaded.join());
    }

    private void registerPreviewError(@NotNull CompletableFuture<Void> failed) {
        new BrowserFunction(browser, AIImageAttachmentViewer.ERROR_FUNCTION) {
            @Nullable
            @Override
            public Object function(@NotNull Object[] arguments) {
                failed.complete(null);
                return null;
            }
        };
    }

    private void loadThumbnailScript() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/web/scripts/messages.js")) {
            Assertions.assertNotNull(input);
            String script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            browser.setText("<html><head><meta http-equiv='Content-Security-Policy' content=\"default-src 'none'; "
                + "connect-src swt:; img-src data:; script-src 'unsafe-inline'\"></head><body><script>"
                + script + "</script></body></html>");
        }
    }

    private void waitFor(@NotNull CompletableFuture<?> future) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MS);
        display.timerExec(TIMEOUT_MS, () -> {});
        while (!future.isDone() && System.nanoTime() < deadline) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        Assertions.assertTrue(future.isDone(), "Browser did not complete the request");
    }

    @NotNull
    private static AIImageAttachment corruptImage() {
        byte[] bytes = Base64.getDecoder().decode(LOSSLESS);
        bytes[24] |= (byte) 0xe0;
        return AIImageAttachment.fromBytes("corrupt.webp", bytes);
    }

    @NotNull
    private static AIImageAttachment image(@NotNull String source) {
        return AIImageAttachment.fromBytes("image.webp", Base64.getDecoder().decode(source));
    }
}
