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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIImageAttachment;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;

import java.util.List;
import java.util.function.IntConsumer;

final class AIImageAttachmentsComposite extends ScrolledComposite {
    private final Composite attachments;
    private final IntConsumer removeImage;

    AIImageAttachmentsComposite(@NotNull Composite parent, @NotNull IntConsumer removeImage) {
        super(parent, SWT.V_SCROLL);
        this.removeImage = removeImage;
        setBackgroundMode(SWT.INHERIT_DEFAULT);
        setExpandHorizontal(true);
        setExpandVertical(true);
        attachments = new Composite(this, SWT.NONE);
        attachments.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(6, 2).create());
        setContent(attachments);
        addListener(SWT.Resize, event -> updateContentSize());
    }

    void setImages(@NotNull List<AIImageAttachment> images) {
        for (Control control : attachments.getChildren()) {
            control.dispose();
        }
        for (int index = 0; index < images.size(); index++) {
            AIImageAttachment image = images.get(index);
            Link link = new Link(attachments, SWT.NONE);
            link.setText("<a>" + LegacyActionTools.escapeMnemonics(image.name()) + "</a>");
            link.setToolTipText(image.name());
            GridData linkData = new GridData(SWT.FILL, SWT.CENTER, true, false);
            linkData.widthHint = 1;
            link.setLayoutData(linkData);
            link.addSelectionListener(SelectionListener.widgetSelectedAdapter(event ->
                AIImageAttachmentViewer.open(getShell(), image)));
            int imageIndex = index;
            Button remove = new Button(attachments, SWT.PUSH);
            remove.setImage(DBeaverIcons.getImage(UIIcon.CLOSE));
            remove.setToolTipText(AIChatMessagesUI.ai_chat_image_remove + ": " + image.name());
            remove.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
            remove.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
                if (isEnabled()) {
                    removeImage.accept(imageIndex);
                }
            }));
        }
        attachments.layout(true, true);
        int rowHeight = images.isEmpty() ? 0 : attachments.getChildren()[1].computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        ((GridData) getLayoutData()).heightHint = Math.min(images.size(), 3) * (rowHeight + 2);
        setOrigin(0, 0);
        updateContentSize();
    }

    private void updateContentSize() {
        setMinSize(attachments.computeSize(Math.max(1, getClientArea().width), SWT.DEFAULT));
    }
}
