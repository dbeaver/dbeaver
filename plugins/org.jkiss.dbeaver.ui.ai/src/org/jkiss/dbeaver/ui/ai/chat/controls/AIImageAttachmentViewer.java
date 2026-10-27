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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIImageAttachment;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;

final class AIImageAttachmentViewer {
    static final String ERROR_FUNCTION = "imagePreviewError";
    static final int MAX_DISPLAY_SIZE = 2048;

    private AIImageAttachmentViewer() {
    }

    static void open(@NotNull Shell shell, @NotNull AIImageAttachment image) {
        new AbstractPopupPanel(shell, image.name()) {
            @Nullable
            private AbstractJob imageJob;
            private boolean imageErrorShown;

            {
                setModeless(true);
                setShellStyle(SWT.BORDER | SWT.RESIZE);
            }

            @Override
            protected boolean isShowTitle() {
                return false;
            }

            @NotNull
            @Override
            protected Composite createDialogArea(@NotNull Composite parent) {
                Composite area = UIUtils.createPlaceholder(parent, 1);
                area.setLayoutData(new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.FILL, true, true));
                Browser browser = new Browser(area, SWT.NONE);
                browser.setLayoutData(new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.FILL, true, true));
                closeOnFocusLost(browser);
                Shell popup = getShell();
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
                browser.setText(createStatusHtml(AIChatMessagesUI.ai_chat_image_loading));
                imageJob = new AbstractJob(AIChatMessagesUI.ai_chat_image_loading) {
                    @NotNull
                    @Override
                    protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                        if (monitor.isCanceled()) {
                            return Status.CANCEL_STATUS;
                        }
                        try {
                            byte[] bytes = image.getBytes();
                            String source = AIImageThumbnail.isWebp(bytes)
                                ? AIImageThumbnail.toWebpSource(bytes) : AIImageThumbnail.toDataUrl(bytes, MAX_DISPLAY_SIZE);
                            UIUtils.asyncExec(() -> {
                                if (!browser.isDisposed() && !monitor.isCanceled()) {
                                    browser.setText(createImageHtml(source));
                                }
                            });
                        } catch (IOException | SWTException | IllegalArgumentException exception) {
                            UIUtils.asyncExec(() -> {
                                if (!monitor.isCanceled()) {
                                    showImageError(browser, exception);
                                }
                            });
                        }
                        return Status.OK_STATUS;
                    }
                };
                imageJob.setRule(AIImageThumbnail.DECODING_RULE);
                imageJob.setSystem(true);
                browser.addDisposeListener(event -> {
                    if (imageJob != null) {
                        imageJob.cancel();
                    }
                });
                imageJob.schedule();
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

    @NotNull
    static String createImageHtml(@NotNull String source) {
        return """
            <html><head><script>
            let zoom = 1;
            let imageWidth = 0;
            let imageHeight = 0;
            function setZoom(value) {
                zoom = Math.max(0.1, Math.min(8, value));
                const image = document.getElementById('preview');
                image.style.width = Math.max(1, Math.round(imageWidth * zoom)) + 'px';
                image.style.height = Math.max(1, Math.round(imageHeight * zoom)) + 'px';
            }
            function resizeImage(image) {
                const size = %d;
                const scale = Math.min(1, size / Math.max(image.naturalWidth, image.naturalHeight));
                if (scale === 1) {
                    imageWidth = image.naturalWidth;
                    imageHeight = image.naturalHeight;
                    setZoom(Math.min(1, innerWidth / imageWidth, innerHeight / imageHeight));
                    document.body.focus();
                    return;
                }
                try {
                    const canvas = document.createElement('canvas');
                    canvas.width = Math.max(1, Math.round(image.naturalWidth * scale));
                    canvas.height = Math.max(1, Math.round(image.naturalHeight * scale));
                    canvas.getContext('2d').drawImage(image, 0, 0, canvas.width, canvas.height);
                    image.src = canvas.toDataURL('image/png');
                } catch (error) {
                    %s();
                }
            }
            document.addEventListener('wheel', event => {
                if (event.ctrlKey || event.metaKey) {
                    event.preventDefault();
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
                event.preventDefault();
            });
            </script></head><body tabindex='-1' style='margin:0;background:%s;display:flex;align-items:center;justify-content:center;min-width:100vw;min-height:100vh;width:max-content;outline:none'>
            <img id='preview' style='display:block;flex-shrink:0' onload='resizeImage(this)' onerror='%s()' src='%s'>
            </body></html>
            """.formatted(MAX_DISPLAY_SIZE, ERROR_FUNCTION,
            WebCSSInitializer.colorToHex(UIStyles.getDefaultTextBackground()), ERROR_FUNCTION, source);
    }
}
