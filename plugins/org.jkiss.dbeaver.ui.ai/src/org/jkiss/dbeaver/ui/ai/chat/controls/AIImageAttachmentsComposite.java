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

import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ToolBar;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIImageAttachment;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UITextUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;

import java.util.List;
import java.util.function.IntConsumer;

final class AIImageAttachmentsComposite extends ScrolledComposite {
    private static final int MAX_LINK_WIDTH = 180;
    private static final int MAX_VISIBLE_ROWS = 2;

    private final Composite attachments;
    private final IntConsumer removeImage;

    AIImageAttachmentsComposite(@NotNull Composite parent, @NotNull IntConsumer removeImage) {
        super(parent, SWT.V_SCROLL);
        this.removeImage = removeImage;
        setBackgroundMode(SWT.INHERIT_DEFAULT);
        setExpandHorizontal(true);
        setExpandVertical(true);
        setShowFocusedControl(true);
        attachments = new Composite(this, SWT.NONE);
        RowLayout layout = new RowLayout(SWT.HORIZONTAL);
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.spacing = 6;
        layout.center = true;
        attachments.setLayout(layout);
        setContent(attachments);
        addListener(SWT.Resize, event -> updateContentSize());
    }

    void setImages(@NotNull List<AIImageAttachment> images) {
        for (Control control : attachments.getChildren()) {
            control.dispose();
        }
        for (int index = 0; index < images.size(); index++) {
            AIImageAttachment image = images.get(index);
            Composite attachment = new Composite(attachments, SWT.NONE);
            attachment.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(2, 0).create());
            Link link = new Link(attachment, SWT.NONE);
            link.setToolTipText(image.name());
            link.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
            link.addSelectionListener(SelectionListener.widgetSelectedAdapter(event ->
                AIImageAttachmentViewer.open(getShell(), image)));
            int imageIndex = index;
            ToolBar toolbar = new ToolBar(attachment, SWT.FLAT);
            toolbar.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
            UIUtils.createToolItem(toolbar, AIChatMessagesUI.ai_chat_image_remove + ": " + image.name(), UIIcon.CLOSE,
                SelectionListener.widgetSelectedAdapter(event -> {
                    if (isEnabled()) {
                        removeImage.accept(imageIndex);
                    }
                }));
        }
        setOrigin(0, 0);
        updateContentSize();
    }

    private void updateContentSize() {
        int width = Math.max(1, getClientArea().width);
        int rowHeight = 0;
        RowLayout layout = (RowLayout) attachments.getLayout();
        for (Control control : attachments.getChildren()) {
            Composite attachment = (Composite) control;
            Link link = (Link) attachment.getChildren()[0];
            ToolBar toolbar = (ToolBar) attachment.getChildren()[1];
            int linkWidth = Math.max(1, Math.min(MAX_LINK_WIDTH, width - toolbar.computeSize(SWT.DEFAULT, SWT.DEFAULT).x - 2));
            updateLinkText(link, linkWidth);
            attachment.layout(true, true);
            rowHeight = Math.max(rowHeight, attachment.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
        }
        Point size = attachments.computeSize(width, SWT.DEFAULT, true);
        setMinSize(size);
        int height = Math.max(0, Math.min(size.y, MAX_VISIBLE_ROWS * (rowHeight + layout.spacing) - layout.spacing));
        if (getLayoutData() instanceof GridData layoutData && layoutData.heightHint != height) {
            layoutData.heightHint = height;
            requestLayout();
        }
        attachments.layout(true, true);
    }

    private void updateLinkText(@NotNull Link link, int width) {
        String name = link.getToolTipText();
        int textWidth = width;
        // account for native link sizing so abbreviated names stay on one line
        while (true) {
            String text = UITextUtils.getShortText(link, name, textWidth);
            link.setText("<a>" + LegacyActionTools.escapeMnemonics(text) + "</a>");
            int preferredWidth = link.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
            if (preferredWidth <= width || textWidth == 1) {
                break;
            }
            textWidth = Math.max(1, textWidth - preferredWidth + width);
        }
    }
}
