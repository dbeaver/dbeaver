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
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessages;
import org.jkiss.dbeaver.ui.ai.internal.AIUIFeatures;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.IOUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;

final class WebViewDndController {

    private static final Log log = Log.getLog(WebViewDndController.class);

    private final Composite owner;
    private final Browser browser;
    private final Composite dndOverlay;
    private final StackLayout stackLayout;
    private final AIChatControl chat;
    private final Supplier<DBPDataSource> dataSourceSupplier;

    WebViewDndController(
        @NotNull Composite owner,
        @NotNull Browser browser,
        @NotNull AIChatControl chat,
        @NotNull Supplier<DBPDataSource> dataSourceSupplier
    ) {
        this.owner = owner;
        this.browser = browser;
        this.dndOverlay = new Composite(owner, SWT.NONE);
        this.stackLayout = new StackLayout();
        owner.setLayout(stackLayout);
        this.chat = chat;
        this.dataSourceSupplier = dataSourceSupplier;
        setupDragAndDrop();
    }

    @Nullable
    private DBPDataSource getDataSource() {
        return dataSourceSupplier.get();
    }

    private void setupDragAndDrop() {
        stackLayout.topControl = browser;
        dndOverlay.addPaintListener(WebViewDndController::drawDNDWindow);

        DropTarget dropTarget = new DropTarget(dndOverlay, DND.DROP_COPY | DND.DROP_DEFAULT);
        dropTarget.setTransfer(FileTransfer.getInstance());

        dropTarget.addDropListener(new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void dragOver(DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void dragLeave(DropTargetEvent event) {
                hideDndOverlay();
            }

            @Override
            public void dragOperationChanged(DropTargetEvent event) {
                handleDragEvent(event);
            }

            private void handleDragEvent(@NotNull DropTargetEvent event) {
                if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
                    event.detail = DND.DROP_COPY;
                } else {
                    event.detail = DND.DROP_NONE;
                }
            }

            @Override
            public void drop(DropTargetEvent event) {
                log.debug("DND: drop on overlay");
                hideDndOverlay();

                if (event.data instanceof String[] files) {
                    List<Path> paths = new ArrayList<>();
                    for (String file : files) {
                        Path filepath = Path.of(file);
                        paths.add(filepath);
                        DBPDataSource dataSource = getDataSource();
                        if (dataSource != null) {
                            LinkedHashMap<String, Object> additionalParameters = new LinkedHashMap<>();
                            String fileExtension = IOUtils.getFileExtension(file);
                            additionalParameters.put(AIUIFeatures.PARAM_FILE_TYPE, fileExtension);
                            AIUIFeatures.AI_CHAT_DROP_EVENT.use(AIUIFeatures.buildFeatureParameters(
                                dataSource.getContainer(),
                                additionalParameters
                            ));
                        }
                    }
                    if (!paths.isEmpty()) {
                        chat.attachFiles(paths);
                    }
                } else {
                    log.warn("DND: unexpected data type: " + (event.data != null ? event.data.getClass() : "null"));
                }
            }
        });

        new BrowserFunction(browser, "onDragEnter") {
            @Nullable
            @Override
            public Object function(@NotNull Object[] arguments1) {
                super.function(arguments1);
                if (arguments1.length > 0 &&
                    arguments1[0] instanceof Object[] mtList &&
                    ArrayUtils.contains(mtList, MimeTypes.TEXT_PLAIN)) {
                    return null;
                }
                log.debug("DND: onDragEnter from JS");
                showDndOverlay();
                return null;
            }
        };

        owner.addTraverseListener(e -> {
            if (e.detail == SWT.TRAVERSE_ESCAPE) {
                hideDndOverlay();
            }
        });
    }

    private static void drawDNDWindow(@NotNull PaintEvent e) {
        GC gc = e.gc;
        gc.setForeground(BaseThemeSettings.instance.colorAccent);
        gc.setLineStyle(SWT.LINE_DASH);
        gc.drawRoundRectangle(e.x + 5, e.y + 5, e.width - 11, e.height - 11, 10, 10);

        Font font = UIUtils.scaleFontSize(gc.getFont(), 2);
        gc.setFont(font);

        var text = AIChatMessages.ai_chat_drag_n_drop_message;
        var extent = gc.textExtent(text);

        Image icon = DBeaverIcons.getImage(UIIcon.IMPORT);
        Rectangle iconBounds = icon.getBounds();
        int iconSize = 48;

        int spacing = 20;
        int totalHeight = extent.y + spacing + iconSize;

        int textY = (e.height - totalHeight) / 2;
        int textX = (e.width - extent.x) / 2;
        gc.drawText(text, textX, textY);

        int iconX = (e.width - iconSize) / 2;
        int iconY = textY + extent.y + spacing;
        gc.drawImage(icon, 0, 0, iconBounds.width, iconBounds.height,
            iconX, iconY, iconSize, iconSize);

        font.dispose();
    }

    private void showDndOverlay() {
        UIUtils.asyncExec(() -> {
            if (!owner.isDisposed()) {
                stackLayout.topControl = dndOverlay;
                owner.layout();
                dndOverlay.forceFocus();
            }
        });
    }

    private void hideDndOverlay() {
        UIUtils.asyncExec(() -> {
            if (!owner.isDisposed()) {
                stackLayout.topControl = browser;
                owner.layout();
            }
        });
    }
}
