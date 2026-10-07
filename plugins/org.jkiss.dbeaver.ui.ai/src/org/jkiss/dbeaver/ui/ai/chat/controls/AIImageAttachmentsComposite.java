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
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AIImageAttachment;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

final class AIImageAttachmentsComposite extends ScrolledComposite {
    private static final Log log = Log.getLog(AIImageAttachmentsComposite.class);
    private static final int MAX_VISIBLE_ROWS = 2;

    private final Composite attachments;
    private final IntConsumer removeImage;
    private final Map<AIImageAttachment, ImageData> thumbnails = new HashMap<>();
    @Nullable
    private AbstractJob thumbnailJob;

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
        attachments.setLayout(layout);
        setContent(attachments);
        addListener(SWT.Resize, event -> updateContentSize());
        addDisposeListener(event -> {
            if (thumbnailJob != null) {
                thumbnailJob.cancel();
            }
            thumbnails.clear();
        });
    }

    void setImages(@NotNull List<AIImageAttachment> images) {
        if (thumbnailJob != null) {
            thumbnailJob.cancel();
            thumbnailJob = null;
        }
        for (Control control : attachments.getChildren()) {
            control.dispose();
        }
        thumbnails.keySet().retainAll(images);
        List<ThumbnailRequest> pending = new ArrayList<>();
        for (int index = 0; index < images.size(); index++) {
            AIImageAttachment image = images.get(index);
            int imageIndex = index;
            AIImageAttachmentTile tile = new AIImageAttachmentTile(attachments, image, () -> removeImage.accept(imageIndex));
            ImageData cached = thumbnails.get(image);
            if (cached != null) {
                tile.setThumbnail(cached);
            } else {
                pending.add(new ThumbnailRequest(image, tile));
            }
        }
        setOrigin(0, 0);
        updateContentSize();
        if (!pending.isEmpty()) {
            loadThumbnails(pending);
        }
    }

    private void loadThumbnails(@NotNull List<ThumbnailRequest> requests) {
        thumbnailJob = new AbstractJob(AIChatMessagesUI.ai_chat_image_loading) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                for (ThumbnailRequest request : requests) {
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    try {
                        ImageData thumbnail = AIImageThumbnail.read(request.image().getBytes(), AIImageAttachmentTile.PREVIEW_SIZE * 2);
                        if (monitor.isCanceled()) {
                            return Status.CANCEL_STATUS;
                        }
                        UIUtils.asyncExec(() -> {
                            if (!isDisposed() && !request.tile().isDisposed()) {
                                thumbnails.put(request.image(), thumbnail);
                                request.tile().setThumbnail(thumbnail);
                            }
                        });
                    } catch (IOException | SWTException | IllegalArgumentException exception) {
                        log.debug("Cannot create image attachment thumbnail", exception);
                    }
                }
                return Status.OK_STATUS;
            }
        };
        thumbnailJob.setSystem(true);
        thumbnailJob.schedule();
    }

    private void updateContentSize() {
        RowLayout layout = (RowLayout) attachments.getLayout();
        Point size = attachments.computeSize(Math.max(1, getClientArea().width), SWT.DEFAULT, true);
        setMinSize(size);
        int rowHeight = attachments.getChildren().length == 0 ? 0 : ((RowData)
            attachments.getChildren()[0].getLayoutData()).height;
        int height = Math.max(0, Math.min(size.y, MAX_VISIBLE_ROWS * (rowHeight + layout.spacing) - layout.spacing));
        if (getLayoutData() instanceof GridData layoutData && layoutData.heightHint != height) {
            layoutData.heightHint = height;
            requestLayout();
        }
        attachments.layout(true, true);
    }

    private record ThumbnailRequest(@NotNull AIImageAttachment image, @NotNull AIImageAttachmentTile tile) {
    }
}
