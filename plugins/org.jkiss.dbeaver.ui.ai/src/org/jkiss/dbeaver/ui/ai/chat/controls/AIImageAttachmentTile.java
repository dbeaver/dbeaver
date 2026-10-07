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
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleActionAdapter;
import org.eclipse.swt.accessibility.AccessibleActionEvent;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIImageAttachment;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;

final class AIImageAttachmentTile extends Composite {
    static final int PREVIEW_SIZE = 64;
    private static final int PREVIEW_PADDING = 4;
    private static final int CLOSE_SIZE = 18;
    private static final int TOP_PADDING = 8;

    private final Canvas preview;
    @Nullable
    private Image thumbnail;

    AIImageAttachmentTile(@NotNull Composite parent, @NotNull AIImageAttachment image, @NotNull Runnable removeImage) {
        super(parent, SWT.NONE);
        preview = new Canvas(this, SWT.DOUBLE_BUFFERED);
        preview.setBounds(0, TOP_PADDING, PREVIEW_SIZE, PREVIEW_SIZE);
        preview.addPaintListener(event -> paintPreview(event.gc));
        configureButton(preview, image.name(), AIChatMessagesUI.ai_chat_image_open,
            () -> AIImageAttachmentViewer.open(getShell(), image));
        Canvas remove = new Canvas(this, SWT.DOUBLE_BUFFERED);
        remove.setBounds(PREVIEW_SIZE - CLOSE_SIZE / 2, 0, CLOSE_SIZE, CLOSE_SIZE);
        configureButton(remove, AIChatMessagesUI.ai_chat_image_remove + ": " + image.name(),
            AIChatMessagesUI.ai_chat_image_remove, removeImage);
        remove.addPaintListener(event -> {
            GC gc = event.gc;
            gc.setAntialias(SWT.ON);
            gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
            gc.fillOval(1, 1, CLOSE_SIZE - 2, CLOSE_SIZE - 2);
            gc.setForeground(remove.isFocusControl() ? getDisplay().getSystemColor(SWT.COLOR_LINK_FOREGROUND) : getBackground());
            gc.drawOval(1, 1, CLOSE_SIZE - 2, CLOSE_SIZE - 2);
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
            gc.setLineWidth(2);
            gc.drawLine(6, 6, CLOSE_SIZE - 6, CLOSE_SIZE - 6);
            gc.drawLine(6, CLOSE_SIZE - 6, CLOSE_SIZE - 6, 6);
        });
        setLayoutData(new RowData(PREVIEW_SIZE + CLOSE_SIZE / 2, PREVIEW_SIZE + TOP_PADDING));
        setTabList(new Control[]{preview, remove});
        addDisposeListener(event -> UIUtils.dispose(thumbnail));
    }

    private void configureButton(@NotNull Canvas button, @NotNull String name, @NotNull String actionName, @NotNull Runnable action) {
        button.setToolTipText(name);
        button.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        Runnable activate = () -> {
            if (button.isEnabled()) {
                action.run();
            }
        };
        button.addListener(SWT.MouseUp, event -> {
            if (event.button == 1) {
                button.setFocus();
                activate.run();
            }
        });
        button.addListener(SWT.KeyDown, event -> {
            if (event.stateMask == 0 && (event.keyCode == SWT.CR || event.keyCode == SWT.SPACE)) {
                activate.run();
            }
        });
        button.addTraverseListener(event -> {
            if (event.detail == SWT.TRAVERSE_TAB_NEXT || event.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                event.doit = true;
            }
        });
        button.addListener(SWT.FocusIn, event -> button.redraw());
        button.addListener(SWT.FocusOut, event -> button.redraw());
        button.getAccessible().addAccessibleListener(new AccessibleAdapter() {
            @Override
            public void getName(@NotNull AccessibleEvent event) {
                event.result = name;
            }
        });
        button.getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
            @Override
            public void getRole(@NotNull AccessibleControlEvent event) {
                event.detail = ACC.ROLE_PUSHBUTTON;
            }
        });
        button.getAccessible().addAccessibleActionListener(new AccessibleActionAdapter() {
            @Override
            public void getActionCount(@NotNull AccessibleActionEvent event) {
                event.count = 1;
            }

            @Override
            public void getName(@NotNull AccessibleActionEvent event) {
                event.result = actionName;
            }

            @Override
            public void doAction(@NotNull AccessibleActionEvent event) {
                if (event.index == 0) {
                    activate.run();
                    event.result = ACC.OK;
                }
            }
        });
    }

    void setThumbnail(@NotNull ImageData data) {
        UIUtils.dispose(thumbnail);
        thumbnail = new Image(getDisplay(), data);
        preview.redraw();
    }

    private void paintPreview(@NotNull GC gc) {
        gc.setAntialias(SWT.ON);
        gc.setInterpolation(SWT.HIGH);
        Image image = thumbnail != null ? thumbnail : DBeaverIcons.getImage(UIIcon.PICTURE);
        if (image != null) {
            Rectangle bounds = image.getBounds();
            double scale = Math.min((double) (PREVIEW_SIZE - 2 * PREVIEW_PADDING) / bounds.width,
                (double) (PREVIEW_SIZE - 2 * PREVIEW_PADDING) / bounds.height);
            if (thumbnail == null) {
                scale = Math.min(1, scale);
            }
            int width = Math.max(1, (int) Math.round(bounds.width * scale));
            int height = Math.max(1, (int) Math.round(bounds.height * scale));
            gc.drawImage(image, 0, 0, bounds.width, bounds.height,
                (PREVIEW_SIZE - width) / 2, (PREVIEW_SIZE - height) / 2, width, height);
        }
        gc.setForeground(preview.isFocusControl() ? getDisplay().getSystemColor(SWT.COLOR_LINK_FOREGROUND) : getForeground());
        gc.setAlpha(preview.isFocusControl() ? 255 : 60);
        gc.drawRoundRectangle(0, 0, PREVIEW_SIZE - 1, PREVIEW_SIZE - 1, 12, 12);
    }
}
