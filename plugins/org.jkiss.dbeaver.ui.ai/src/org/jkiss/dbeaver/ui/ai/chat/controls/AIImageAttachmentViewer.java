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
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIImageAttachment;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;

final class AIImageAttachmentViewer {
    static final String ERROR_FUNCTION = "imagePreviewError";
    static final int MAX_DISPLAY_SIZE = 2048;

    private AIImageAttachmentViewer() {
    }

    static void open(@NotNull Shell shell, @NotNull AIImageAttachment image) {
        new Dialog(shell) {
            @Nullable
            private AbstractJob imageJob;
            private boolean imageErrorShown;

            {
                setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
            }

            @Override
            protected void configureShell(@NotNull Shell newShell) {
                super.configureShell(newShell);
                newShell.setText(image.name());
            }

            @NotNull
            @Override
            protected Control createDialogArea(@NotNull Composite parent) {
                Composite area = (Composite) super.createDialogArea(parent);
                Browser browser = new Browser(area, SWT.NONE);
                browser.setLayoutData(new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.FILL, true, true));
                new BrowserFunction(browser, ERROR_FUNCTION) {
                    @Nullable
                    @Override
                    public Object function(@NotNull Object[] arguments) {
                        UIUtils.asyncExec(() -> showImageError(browser, new IOException("Cannot decode image.")));
                        return null;
                    }
                };
                browser.setText("<html><body style='background:#222;color:#ddd'>"
                    + CommonUtils.escapeHtml(AIChatMessagesUI.ai_chat_image_loading) + "</body></html>");
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

            private void showImageError(@NotNull Browser browser, @NotNull Exception exception) {
                if (browser.isDisposed() || imageErrorShown) {
                    return;
                }
                imageErrorShown = true;
                browser.setText("<html><body style='background:#222;color:#ddd'>"
                    + CommonUtils.escapeHtml(CommonUtils.notEmpty(exception.getMessage())) + "</body></html>");
                DBWorkbench.getPlatformUI().showError(AIChatMessagesUI.ai_chat_image_error,
                    NLS.bind(AIChatMessagesUI.ai_chat_image_view_error, image.name()), exception);
            }

            @Override
            protected void createButtonsForButtonBar(@NotNull Composite parent) {
                createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
            }

            @NotNull
            @Override
            protected Point getInitialSize() {
                return new Point(900, 650);
            }
        }.open();
    }

    @NotNull
    static String createImageHtml(@NotNull String source) {
        return """
            <html><head><script>
            function resizeImage(image) {
                const size = %d;
                const scale = Math.min(1, size / Math.max(image.naturalWidth, image.naturalHeight));
                if (scale === 1) {
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
            </script></head><body style='margin:0;background:#222;display:flex;align-items:center;justify-content:center;height:100vh'>
            <img style='max-width:100%%;max-height:100%%' onload='resizeImage(this)' onerror='%s()' src='%s'>
            </body></html>
            """.formatted(MAX_DISPLAY_SIZE, ERROR_FUNCTION, ERROR_FUNCTION, source);
    }
}
