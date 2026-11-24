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
package org.jkiss.dbeaver.ui;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.model.application.descriptor.basic.MPartDescriptor;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MItem;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.osgi.service.event.EventHandler;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Bridges theme registry changes to images already assigned to SWT and Eclipse model widgets.
 */
final class IconThemeManager {
    private static final String E4_MODEL_ELEMENT = "modelElement";
    private static final List<String> ICON_COLOR_IDS = List.of(
        BaseThemeSettings.ICON_DEFAULT,
        BaseThemeSettings.ICON_SECONDARY,
        BaseThemeSettings.ICON_ACCENT,
        BaseThemeSettings.ICON_TIP,
        BaseThemeSettings.ICON_ATTENTION,
        BaseThemeSettings.ICON_POSITIVE,
        BaseThemeSettings.ICON_IMPORTANT,
        BaseThemeSettings.ICON_DANGER,
        BaseThemeSettings.ICON_BACKGROUND);
    private static final Map<Image, DBPImage> trackedImages = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Image, Map<String, Boolean>> originalImages = new WeakHashMap<>();
    private static final AtomicBoolean initializationPending = new AtomicBoolean();
    private static volatile Map<String, String> palette = Map.of();
    private static volatile boolean initialized;
    private static boolean refreshPending;

    private IconThemeManager() {
    }

    static void initialize() {
        if (initialized || !PlatformUI.isWorkbenchRunning()) {
            return;
        }
        Display display = PlatformUI.getWorkbench().getDisplay();
        if (display.isDisposed()) {
            return;
        }
        if (Display.getCurrent() != display) {
            if (initializationPending.compareAndSet(false, true)) {
                display.asyncExec(() -> {
                    initializationPending.set(false);
                    initialize();
                });
            }
            return;
        }
        initialized = true;
        updatePalette();
        IEventBroker broker = PlatformUI.getWorkbench().getService(IEventBroker.class);
        EventHandler themeListener = event -> scheduleRefresh(display);
        if (broker != null) {
            broker.subscribe(IThemeEngine.Events.THEME_CHANGED, themeListener);
        }
        IPropertyChangeListener colorListener = event -> {
            if (event.getProperty().startsWith("org.jkiss.dbeaver.ui.icons.")) {
                scheduleRefresh(display);
            }
        };
        var themeManager = PlatformUI.getWorkbench().getThemeManager();
        themeManager.addPropertyChangeListener(colorListener);
        // Eclipse renderers can assign new images after the theme notification (or when a menu opens).
        Listener showListener = event -> {
            if (event.widget instanceof Menu menu) {
                refreshMenu(menu);
                // Menu contributions can be populated by Show listeners after this display filter.
                display.asyncExec(() -> refreshMenu(menu));
            } else if (event.widget instanceof Control control) {
                refreshControl(control);
            }
        };
        display.addFilter(SWT.Show, showListener);
        // Do not enumerate virtual items: getItems() can materialize an entire result set.
        Listener paintListener = event -> {
            if (event.widget.isDisposed()) {
                return;
            }
            if (event.widget instanceof Tree tree && (tree.getStyle() & SWT.VIRTUAL) != 0) {
                refreshVisibleTreeItems(tree);
            } else if (event.widget instanceof Table table && (table.getStyle() & SWT.VIRTUAL) != 0) {
                int count = table.getClientArea().height / Math.max(1, table.getItemHeight()) + 1;
                int end = Math.min(table.getItemCount(), table.getTopIndex() + count);
                for (int i = table.getTopIndex(); i < end; i++) {
                    refreshTableItem(table.getItem(i), Math.max(1, table.getColumnCount()));
                }
            } else if (event.widget instanceof ToolBar toolbar) {
                refreshToolbarIcons(toolbar);
            } else if (event.widget instanceof CTabFolder folder) {
                refreshTabIcons(folder);
            }
        };
        display.addFilter(SWT.Paint, paintListener);
        display.disposeExec(() -> {
            if (broker != null) {
                broker.unsubscribe(themeListener);
            }
            themeManager.removePropertyChangeListener(colorListener);
            display.removeFilter(SWT.Show, showListener);
            display.removeFilter(SWT.Paint, paintListener);
            DBeaverIcons.disposeCachedImages();
            trackedImages.clear();
            originalImages.clear();
            initialized = false;
        });
        scheduleRefresh(display);
    }

    @NotNull
    static Map<String, String> getPalette() {
        return palette;
    }

    static void trackImage(@NotNull Image image, @NotNull DBPImage icon) {
        trackedImages.put(image, icon);
    }

    private static void updatePalette() {
        palette = readPalette(UIUtils.getColorRegistry()::getRGB);
    }

    @NotNull
    static Map<String, String> readPalette(@NotNull Function<String, RGB> colorLookup) {
        Map<String, String> colors = new LinkedHashMap<>();
        for (String colorId : ICON_COLOR_IDS) {
            putColor(colors, colorLookup, colorId + ".source", colorId);
        }
        // Older type icons used variants of Accent, also defined in the registry.
        putColor(colors, colorLookup, BaseThemeSettings.ICON_ACCENT + ".source.type", BaseThemeSettings.ICON_ACCENT);
        putColor(colors, colorLookup, BaseThemeSettings.ICON_ACCENT + ".source.unknown", BaseThemeSettings.ICON_ACCENT);
        return Collections.unmodifiableMap(colors);
    }

    private static void putColor(
        @NotNull Map<String, String> colors,
        @NotNull Function<String, RGB> colorLookup,
        @NotNull String sourceId,
        @NotNull String targetId
    ) {
        RGB source = colorLookup.apply(sourceId);
        RGB target = colorLookup.apply(targetId);
        if (source != null && target != null) {
            colors.put(SVGUtils.toHex(source), SVGUtils.toHex(target));
        }
    }

    private static void scheduleRefresh(@NotNull Display display) {
        if (display.isDisposed()) {
            return;
        }
        if (Display.getCurrent() != display) {
            display.asyncExec(() -> scheduleRefresh(display));
            return;
        }
        if (!refreshPending) {
            refreshPending = true;
            display.asyncExec(() -> {
                refreshPending = false;
                if (!display.isDisposed() && PlatformUI.isWorkbenchRunning()) {
                    updatePalette();
                    for (Shell shell : display.getShells()) {
                        if (!shell.isDisposed()) {
                            refreshMenu(shell.getMenuBar());
                            refreshControl(shell);
                        }
                    }
                }
            });
        }
    }

    @Nullable
    private static Image refreshedImage(@Nullable Image image) {
        if (image == null || image.isDisposed()) {
            return image;
        }
        DBPImage icon = trackedImages.get(image);
        return icon == null ? image : DBeaverIcons.getImage(icon);
    }

    @Nullable
    private static Image refreshedModelImage(@NotNull Widget widget, @Nullable Image image) {
        Image refreshed = refreshedImage(image);
        if (image == null || image.isDisposed()) {
            return refreshed;
        }
        Object model = widget.getData(E4_MODEL_ELEMENT);
        if (model instanceof MPlaceholder placeholder) {
            model = placeholder.getRef();
        }
        String location = null;
        if (model instanceof MPart part) {
            location = part.getIconURI();
            if (location == null) {
                EModelService service = PlatformUI.getWorkbench().getService(EModelService.class);
                MPartDescriptor descriptor = service == null ? null : service.getPartDescriptor(part.getElementId());
                location = descriptor == null ? null : descriptor.getIconURI();
            }
            Object override = part.getTransientData().get(IPresentationEngine.OVERRIDE_ICON_IMAGE_KEY);
            if (override != null) {
                if (override != image) {
                    return refreshed;
                }
                if (refreshed == image && (!SVGUtils.isThemeableSvg(location) || !matchesOriginalIcon(image, location))) {
                    return image;
                }
                if (refreshed == image) {
                    refreshed = DBeaverIcons.getImageByLocation(location);
                }
                if (refreshed != null && refreshed != image) {
                    part.getTransientData().put(IPresentationEngine.OVERRIDE_ICON_IMAGE_KEY, refreshed);
                }
                return refreshed == null ? image : refreshed;
            }
        } else if (model instanceof MItem item) {
            location = item.getIconURI();
        }
        if (refreshed != image || !SVGUtils.isThemeableSvg(location)) {
            return refreshed;
        }
        Image themed = DBeaverIcons.getImageByLocation(location);
        return themed == null ? image : themed;
    }

    private static void refreshControl(@NotNull Control control) {
        if (control.isDisposed()) {
            return;
        }
        if (control instanceof Shell shell) {
            Image[] images = shell.getImages();
            boolean changed = false;
            for (int i = 0; i < images.length; i++) {
                Image image = refreshedImage(images[i]);
                if (image != images[i]) {
                    images[i] = image;
                    changed = true;
                }
            }
            if (changed) {
                shell.setImages(images);
            }
        } else if (control instanceof Label label) {
            Image image = refreshedImage(label.getImage());
            if (image != label.getImage()) {
                label.setImage(image);
            }
        } else if (control instanceof Button button) {
            Image image = refreshedImage(button.getImage());
            if (image != button.getImage()) {
                button.setImage(image);
            }
        } else if (control instanceof CLabel label) {
            Image image = refreshedImage(label.getImage());
            if (image != label.getImage()) {
                label.setImage(image);
            }
        } else if (control instanceof ToolBar toolbar) {
            refreshToolbarIcons(toolbar);
        } else if (control instanceof TabFolder folder) {
            for (TabItem item : folder.getItems()) {
                Image image = refreshedImage(item.getImage());
                if (image != item.getImage()) {
                    item.setImage(image);
                }
            }
        } else if (control instanceof CTabFolder folder) {
            refreshTabIcons(folder);
        } else if (control instanceof Tree tree) {
            for (TreeColumn column : tree.getColumns()) {
                Image image = refreshedImage(column.getImage());
                if (image != column.getImage()) {
                    column.setImage(image);
                }
            }
            if ((tree.getStyle() & SWT.VIRTUAL) == 0) {
                for (TreeItem item : tree.getItems()) {
                    refreshTreeItem(item, Math.max(1, tree.getColumnCount()), true);
                }
            } else {
                tree.redraw();
            }
        } else if (control instanceof Table table) {
            for (TableColumn column : table.getColumns()) {
                Image image = refreshedImage(column.getImage());
                if (image != column.getImage()) {
                    column.setImage(image);
                }
            }
            if ((table.getStyle() & SWT.VIRTUAL) == 0) {
                for (TableItem item : table.getItems()) {
                    refreshTableItem(item, Math.max(1, table.getColumnCount()));
                }
            } else {
                table.redraw();
            }
        }
        refreshMenu(control.getMenu());
        if (control instanceof Composite composite) {
            for (Control child : composite.getChildren()) {
                refreshControl(child);
            }
        }
    }

    private static void refreshToolbarIcons(@NotNull ToolBar toolbar) {
        for (ToolItem item : toolbar.getItems()) {
            Image image = refreshedModelImage(item, item.getImage());
            if (image != item.getImage()) {
                item.setImage(image);
            }
            image = refreshedImage(item.getHotImage());
            if (image != item.getHotImage()) {
                item.setHotImage(image);
            }
            image = refreshedImage(item.getDisabledImage());
            if (image != item.getDisabledImage()) {
                item.setDisabledImage(image);
            }
        }
    }

    private static void refreshTabIcons(@NotNull CTabFolder folder) {
        for (CTabItem item : folder.getItems()) {
            Image image = refreshedModelImage(item, item.getImage());
            if (image != item.getImage()) {
                item.setImage(image);
            }
        }
    }

    private static void refreshTableItem(@NotNull TableItem item, int columns) {
        for (int index = 0; index < columns; index++) {
            Image image = refreshedImage(item.getImage(index));
            if (image != item.getImage(index)) {
                item.setImage(index, image);
            }
        }
    }

    private static boolean matchesOriginalIcon(@NotNull Image image, @NotNull String location) {
        return originalImages.computeIfAbsent(image, key -> new LinkedHashMap<>())
            .computeIfAbsent(location, key -> SVGUtils.matchesOriginalIcon(image, key));
    }

    private static void refreshVisibleTreeItems(@NotNull Tree tree) {
        int rows = tree.getClientArea().height / Math.max(1, tree.getItemHeight()) + 1;
        TreeItem item = tree.getTopItem();
        for (int row = 0; item != null && row < rows; row++) {
            refreshTreeItem(item, Math.max(1, tree.getColumnCount()), false);
            if (item.getExpanded() && item.getItemCount() > 0) {
                item = item.getItem(0);
                continue;
            }
            // Walk only the visible branch; never ask a virtual tree for all of its children.
            while (item != null) {
                TreeItem parent = item.getParentItem();
                int next = (parent == null ? tree.indexOf(item) : parent.indexOf(item)) + 1;
                int count = parent == null ? tree.getItemCount() : parent.getItemCount();
                if (next < count) {
                    item = parent == null ? tree.getItem(next) : parent.getItem(next);
                    break;
                }
                item = parent;
            }
        }
    }

    private static void refreshTreeItem(@NotNull TreeItem item, int columns, boolean recursive) {
        for (int index = 0; index < columns; index++) {
            Image image = refreshedImage(item.getImage(index));
            if (image != item.getImage(index)) {
                item.setImage(index, image);
            }
        }
        if (recursive) {
            for (TreeItem child : item.getItems()) {
                refreshTreeItem(child, columns, true);
            }
        }
    }

    private static void refreshMenu(@Nullable Menu menu) {
        if (menu == null || menu.isDisposed()) {
            return;
        }
        for (MenuItem item : menu.getItems()) {
            Image image = refreshedModelImage(item, item.getImage());
            if (image != item.getImage()) {
                item.setImage(image);
            }
            refreshMenu(item.getMenu());
        }
    }
}
