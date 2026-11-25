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

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIImageAttachment;
import org.jkiss.dbeaver.model.ai.AIImageDimensions;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;
import org.jkiss.dbeaver.ui.browser.LocalResourceHttpServer;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

final class AIImageAttachmentViewer {
    static final String ERROR_FUNCTION = "imagePreviewError";
    // a small file can expand to gigabytes when the browser decodes it
    private static final long MAX_DISPLAY_PIXELS = 100_000_000;

    private AIImageAttachmentViewer() {
    }

    static void open(@NotNull Shell shell, @NotNull AIImageAttachment image) {
        new AbstractPopupPanel(shell, image.name()) {
            private boolean imageErrorShown;

            {
                setModeless(true);
                // Windows paints a non-client strip above resizable, titleless shells.
                setShellStyle(RuntimeUtils.isWindows() ? SWT.NO_TRIM : SWT.BORDER | SWT.RESIZE);
            }

            @Override
            protected boolean isShowTitle() {
                return false;
            }

            @NotNull
            @Override
            protected Composite createDialogArea(@NotNull Composite parent) {
                // NO_TRIM removes the native shell border on Windows as well as its title strip.
                Composite area = new Composite(parent, RuntimeUtils.isWindows() ? SWT.BORDER : SWT.NONE);
                area.setLayout(new FillLayout());
                area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
                Browser browser = new Browser(area, SWT.NONE);
                Shell popup = getShell();
                popup.setBackground(UIStyles.getDefaultTextBackground());
                popup.setBackgroundMode(SWT.INHERIT_DEFAULT);
                popup.addShellListener(ShellListener.shellDeactivatedAdapter(event -> UIUtils.asyncExec(() -> {
                    if (!popup.isDisposed() && popup.getDisplay().getActiveShell() != popup) {
                        close();
                    }
                })));
                Listener outsideClick = event -> {
                    if (event.widget instanceof Control control && control.getShell() != popup && !popup.isDisposed()) {
                        close();
                    }
                };
                popup.getDisplay().addFilter(SWT.MouseDown, outsideClick);
                popup.addDisposeListener(event -> popup.getDisplay().removeFilter(SWT.MouseDown, outsideClick));
                browser.addProgressListener(ProgressListener.completedAdapter(event -> {
                    if (!browser.isDisposed() && browser.getShell() == browser.getDisplay().getActiveShell()) {
                        browser.setFocus();
                    }
                }));
                new BrowserFunction(browser, ERROR_FUNCTION) {
                    @Nullable
                    @Override
                    public Object function(@NotNull Object[] arguments) {
                        UIUtils.asyncExec(() -> showImageError(browser, new IOException("Cannot decode image.")));
                        return null;
                    }
                };
                try {
                    LocalResourceHttpServer.Handle resources = LocalResourceHttpServer.acquire();
                    browser.addDisposeListener(event -> resources.close());
                    String imageHtml = createImageHtml(registerImage(resources, "image", image,
                        exception -> UIUtils.asyncExec(() -> showImageError(browser, exception))));
                    resources.addTextResource("preview.html", () -> imageHtml);
                    browser.setUrl(resources.getUrl("preview.html"));
                } catch (IOException exception) {
                    UIUtils.asyncExec(() -> showImageError(browser, exception));
                }
                return area;
            }

            @NotNull
            @Override
            protected Control createButtonBar(@NotNull Composite parent) {
                return parent;
            }

            private void showImageError(@NotNull Browser browser, @NotNull Exception exception) {
                if (browser.isDisposed() || imageErrorShown) {
                    return;
                }
                imageErrorShown = true;
                browser.setText(createStatusHtml(CommonUtils.notEmpty(exception.getMessage())));
                DBWorkbench.getPlatformUI().showError(AIChatMessagesUI.ai_chat_image_error,
                    NLS.bind(AIChatMessagesUI.ai_chat_image_view_error, image.name()), exception);
            }

            @NotNull
            @Override
            protected Point getInitialSize() {
                return new Point(600, 450);
            }

            @NotNull
            @Override
            protected Point getInitialLocation(@NotNull Point initialSize) {
                Point cursor = shell.getDisplay().getCursorLocation();
                Rectangle bounds = shell.getMonitor().getClientArea();
                for (var monitor : shell.getDisplay().getMonitors()) {
                    if (monitor.getClientArea().contains(cursor)) {
                        bounds = monitor.getClientArea();
                        break;
                    }
                }
                int x = Math.max(bounds.x, Math.min(cursor.x - initialSize.x / 2, bounds.x + bounds.width - initialSize.x));
                int y = cursor.y - initialSize.y - 8;
                if (y < bounds.y) {
                    y = cursor.y + 8;
                }
                y = Math.max(bounds.y, Math.min(y, bounds.y + bounds.height - initialSize.y));
                return new Point(x, y);
            }
        }.open();
    }

    @NotNull
    private static String createStatusHtml(@NotNull String message) {
        return "<html><body style='margin:0;background:%s;color:%s'>%s</body></html>".formatted(
            WebCSSInitializer.colorToHex(UIStyles.getDefaultTextBackground()),
            WebCSSInitializer.colorToHex(UIStyles.getDefaultTextForeground()),
            CommonUtils.escapeHtml(message));
    }

    // the browser decodes the original bytes, so animations and full resolution are preserved
    @NotNull
    static String registerImage(
        @NotNull LocalResourceHttpServer.Handle resources,
        @NotNull String name,
        @NotNull AIImageAttachment image,
        @NotNull Consumer<IOException> errorHandler
    ) {
        String path = name + '.' + image.mediaType().substring(image.mediaType().indexOf('/') + 1);
        resources.addResource(path, () -> {
            byte[] bytes = image.getBytes();
            try {
                checkDisplaySize(bytes);
            } catch (IOException exception) {
                errorHandler.accept(exception);
                throw exception;
            }
            return new ByteArrayInputStream(bytes);
        });
        return resources.getUrl(path);
    }

    private static void checkDisplaySize(@NotNull byte[] bytes) throws IOException {
        AIImageDimensions dimensions = AIImageDimensions.read(bytes);
        long pixels = (long) dimensions.width() * dimensions.height();
        if (bytes.length >= 10 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            // the browser allocates the whole canvas, which can be larger than the first frame
            ByteBuffer header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            pixels = Math.max(pixels, (long) Short.toUnsignedInt(header.getShort(6)) * Short.toUnsignedInt(header.getShort(8)));
        }
        if (pixels > MAX_DISPLAY_PIXELS) {
            throw new IOException("Image is too large to display: " + pixels + " pixels.");
        }
    }

    @NotNull
    private static String createImageHtml(@NotNull String source) {
        return """
            <html><head><script>
            let zoom = 1;
            let minZoom = 0.1;
            let imageWidth = 0;
            let imageHeight = 0;
            let zoomed = false;
            function setZoom(value) {
                zoom = Math.max(minZoom, Math.min(8, value));
                const image = document.getElementById('preview');
                image.style.width = Math.max(1, Math.round(imageWidth * zoom)) + 'px';
                image.style.height = Math.max(1, Math.round(imageHeight * zoom)) + 'px';
            }
            function fitImage() {
                const image = document.getElementById('preview');
                // the page can load before the popup gets its size
                if ((zoomed && imageWidth) || !image?.naturalWidth || !innerWidth || !innerHeight) {
                    return;
                }
                imageWidth = image.naturalWidth;
                imageHeight = image.naturalHeight;
                const fitZoom = Math.min(1, innerWidth / imageWidth, innerHeight / imageHeight);
                minZoom = Math.min(0.1, fitZoom);
                setZoom(fitZoom);
            }
            addEventListener('resize', fitImage);
            document.addEventListener('wheel', event => {
                if (event.ctrlKey || event.metaKey) {
                    event.preventDefault();
                    zoomed = true;
                    setZoom(zoom * (event.deltaY < 0 ? 1.2 : 1 / 1.2));
                }
            }, {passive: false});
            document.addEventListener('keydown', event => {
                if (!event.ctrlKey && !event.metaKey) {
                    return;
                }
                if (event.key === '+' || event.key === '=' || event.code === 'NumpadAdd') {
                    setZoom(zoom * 1.2);
                } else if (event.key === '-' || event.key === '_' || event.code === 'NumpadSubtract') {
                    setZoom(zoom / 1.2);
                } else if (event.key === '0' || event.code === 'Numpad0') {
                    setZoom(1);
                } else {
                    return;
                }
                zoomed = true;
                event.preventDefault();
            });
            </script></head><body tabindex='-1' style='margin:0;background:%s;display:flex;align-items:center;justify-content:center;min-width:100vw;min-height:100vh;width:max-content;outline:none'>
            <img id='preview' style='display:block;flex-shrink:0' onload='fitImage(); document.body.focus()' onerror='%s()' src='%s'>
            </body></html>
            """.formatted(WebCSSInitializer.colorToHex(UIStyles.getDefaultTextBackground()), ERROR_FUNCTION, source);
    }
}
