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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBIconComposite;
import org.jkiss.dbeaver.model.DBPImage;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * DBeaverIcons
 */
public class DBeaverIcons {
    private static final Log log = Log.getLog(DBeaverIcons.class);

    private static class IconDescriptor {
        @NotNull
        String id;
        @NotNull
        Image image;
        @NotNull
        ImageDescriptor imageDescriptor;

        IconDescriptor(@NotNull String id, @NotNull ImageDescriptor imageDescriptor) {
            this.id = id;
            this.image = imageDescriptor.createImage(false);
            this.imageDescriptor = imageDescriptor;
        }

        IconDescriptor(@NotNull String id, @NotNull Image image) {
            this.id = id;
            this.image = image;
            this.imageDescriptor = ImageDescriptor.createFromImage(image);
        }

        IconDescriptor(@NotNull String id, @NotNull Image image, @NotNull ImageDescriptor imageDescriptor) {
            this.id = id;
            this.image = image;
            this.imageDescriptor = imageDescriptor;
        }
    }

    private static final Map<String, IconDescriptor> imageMap = new HashMap<>();
    private static final Map<String, IconDescriptor> compositeMap = new HashMap<>();
    private static final Map<Image, DBPImage> trackedImages = new WeakHashMap<>();
    private static final String E4_MODEL_ELEMENT = "modelElement";
    private static boolean themeListenerInstalled;
    @NotNull
    private static String iconBlue = SVGUtils.SVG_BLUE;

    @NotNull
    public static Image getImage(@Nullable DBPImage image) {
        return getIconDescriptor(image, true).image;
    }

    @NotNull
    public static Image getImage(@Nullable DBPImage image, boolean useCache) {
        return getIconDescriptor(image, useCache).image;
    }

    @Nullable
    public static Image getImageByLocation(@NotNull String iconLocation) {
        IconDescriptor icon = getIconByLocation(iconLocation);
        if (icon == null) {
            return null;
        }
        return icon.image;
    }

    @NotNull
    public static ImageDescriptor getImageDescriptor(@Nullable DBPImage image) {
        return getIconDescriptor(image, true).imageDescriptor; 
    }

    @NotNull
    private static IconDescriptor getIconDescriptor(@Nullable DBPImage image, boolean useCache) {
        switch (image) {
            case null -> {
                return getIconDescriptor(DBIcon.TYPE_UNKNOWN, useCache);
            }
            case DBIconBinary ib -> {
                return new IconDescriptor(
                    "[" + image.getLocation() + "]",
                    ib.getImage(),
                    ib.getImageDescriptor()
                );
            }
            case DBIconComposite ic -> {
                IconDescriptor icon = getIconDescriptor(ic.getMain(), useCache);
                return getCompositeIcon(icon, ic, useCache);
            }
            case DBIcon ic -> {
                IconDescriptor icon = getIconByLocation(image.getLocation());
                if (icon == null) {
                    log.error("Image '" + image.getLocation() + "' not found");
                    return getIconDescriptor(DBIcon.TYPE_UNKNOWN, useCache);
                } else {
                    return icon;
                }
            }
            default -> {
                log.error("Unexpected image of type " + image.getClass());
                return getIconDescriptor(DBIcon.TYPE_UNKNOWN, useCache);
            }
        }
    }

    @NotNull
    private static IconDescriptor getCompositeIcon(@NotNull IconDescriptor mainIcon, @NotNull DBIconComposite image, boolean useCache) {
        if (!image.hasOverlays()) {
            return mainIcon;
        }
        String compositeId = mainIcon.id + "^" + iconBlue + "^" +
            (image.getTopLeft() == null ? "" : image.getTopLeft().getLocation()) + "^" +
            (image.getTopRight() == null ? "" : image.getTopRight().getLocation()) + "^" +
            (image.getBottomLeft() == null ? "" : image.getBottomLeft().getLocation()) + "^" +
            (image.getBottomRight() == null ? "" : image.getBottomRight().getLocation());
        IconDescriptor icon = useCache ? compositeMap.get(compositeId) : null;
        if (icon == null) {
            OverlayImageDescriptor ovrImage = new OverlayImageDescriptor(mainIcon.imageDescriptor);
            if (image.getTopLeft() != null) {
                ovrImage.setTopLeft(accumulateDecorations(image, DBIconComposite::getTopLeft));
            }
            if (image.getTopRight() != null) {
                ovrImage.setTopRight(accumulateDecorations(image, DBIconComposite::getTopRight));
            }
            if (image.getBottomLeft() != null) {
                ovrImage.setBottomLeft(accumulateDecorations(image, DBIconComposite::getBottomLeft));
            }
            if (image.getBottomRight() != null) {
                ovrImage.setBottomRight(accumulateDecorations(image, DBIconComposite::getBottomRight));
            }
            Image resultImage = ovrImage.createImage();
            icon = new IconDescriptor(compositeId, resultImage, trackImages(image, ImageDescriptor.createFromImage(resultImage)));
            trackedImages.put(resultImage, image);
            if (useCache) {
                compositeMap.put(compositeId, icon);
            }
        }
        return icon;
    }

    @NotNull
    private static ImageDescriptor[] accumulateDecorations(
        @NotNull DBIconComposite image,
        @NotNull Function<DBIconComposite, DBPImage> map
    ) {
        DBPImage base = image.getMain();
        if (base instanceof DBIconComposite ic) {
            List<ImageDescriptor> decorations = new ArrayList<>();
            decorations.add(getImageDescriptor(map.apply(image)));
            do {
                image = ic;
                decorations.add(getImageDescriptor(map.apply(image)));
                base = image.getMain();
            } while (base instanceof DBIconComposite);
            return decorations.toArray(ImageDescriptor[]::new);
        } else {
            return new ImageDescriptor[]{getImageDescriptor(map.apply(image))};
        }
    }

    @Nullable
    private static IconDescriptor getIconByLocation(@NotNull String location) {
        updateTheme();
        boolean registeredSvg = SVGUtils.isThemeableSvg(location);
        boolean recolor = registeredSvg && !SVGUtils.SVG_BLUE.equals(iconBlue);
        String key = recolor ? location + "#" + iconBlue : location;
        IconDescriptor icon = imageMap.get(key);
        if (icon == null) {
            try {
                ImageDescriptor imageDescriptor = recolor ? SVGUtils.createRecoloredSvgDescriptor(location, iconBlue) :
                    ImageDescriptor.createFromURL(URI.create(location).toURL());
                if (registeredSvg) {
                    imageDescriptor = trackImages(new DBIcon(location), imageDescriptor);
                }
                icon = new IconDescriptor(key, imageDescriptor);
                imageMap.put(key, icon);
            } catch (Exception e) {
                log.error(e);
                return null;
            }
        }
        return icon;
    }

    @NotNull
    private static ImageDescriptor trackImages(@NotNull DBPImage icon, @NotNull ImageDescriptor source) {
        return new ImageDescriptor() {
            @Nullable
            @Override
            public ImageData getImageData(int zoom) {
                return source.getImageData(zoom);
            }

            @Nullable
            @Override
            public Image createImage(boolean returnMissingImageOnError, @Nullable Device device) {
                Image image = source.createImage(returnMissingImageOnError, device);
                if (image != null) {
                    trackedImages.put(image, icon);
                }
                return image;
            }
        };
    }

    private static void updateTheme() {
        if (!PlatformUI.isWorkbenchRunning()) {
            return;
        }
        if (!themeListenerInstalled) {
            IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
            if (eventBroker != null && eventBroker.subscribe(IThemeEngine.Events.THEME_CHANGED,
                event -> UIUtils.asyncExec(DBeaverIcons::refreshThemeIcons))) {
                PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(event -> {
                    if (SVGUtils.SVG_BLUE_LIGHT.equals(event.getProperty()) || SVGUtils.SVG_BLUE_DARK.equals(event.getProperty())) {
                        UIUtils.asyncExec(DBeaverIcons::refreshThemeIcons);
                    }
                });
                themeListenerInstalled = true;
            }
        }
        iconBlue = SVGUtils.getThemeColor();
    }

    private static void refreshThemeIcons() {
        if (!PlatformUI.isWorkbenchRunning()) {
            return;
        }
        iconBlue = SVGUtils.getThemeColor();
        // CSS theme updates do not replace images assigned with SWT setImage or JFace image descriptors.
        for (Shell shell : Display.getCurrent().getShells()) {
            if (!shell.isDisposed()) {
                Image image = refreshedImage(shell.getImage());
                if (image != shell.getImage()) {
                    shell.setImage(image);
                }
                refreshMenu(shell.getMenuBar());
                refreshControl(shell);
            }
        }
    }

    @Nullable
    private static Image refreshedImage(@Nullable Image image) {
        if (image == null || image.isDisposed()) {
            return image;
        }
        DBPImage icon = trackedImages.get(image);
        if (icon == null) {
            return image;
        }
        return getIconDescriptor(icon, true).image;
    }

    @Nullable
    private static Image refreshedModelImage(@NotNull Widget widget, @Nullable Image image) {
        Image refreshed = refreshedImage(image);
        if (image == null) {
            return refreshed;
        }

        // Eclipse's command and part renderers load their icon URIs without using DBeaverIcons.
        Object model = widget.getData(E4_MODEL_ELEMENT);
        if (model instanceof MPlaceholder placeholder) {
            model = placeholder.getRef();
        }
        String location = null;
        if (model instanceof MPart part) {
            location = part.getIconURI();
            if (location == null) {
                EModelService modelService = PlatformUI.getWorkbench().getService(EModelService.class);
                MPartDescriptor descriptor = modelService == null ? null : modelService.getPartDescriptor(part.getElementId());
                location = descriptor == null ? null : descriptor.getIconURI();
            }
            Object override = part.getTransientData().get(IPresentationEngine.OVERRIDE_ICON_IMAGE_KEY);
            if (override != null) {
                if (override != image) {
                    return refreshed;
                }
                if (refreshed == image && (!SVGUtils.isThemeableSvg(location) || !SVGUtils.matchesOriginalIcon(image, location))) {
                    return image;
                }
                if (refreshed == image) {
                    IconDescriptor icon = getIconByLocation(location);
                    refreshed = icon == null ? image : icon.image;
                }
                if (refreshed != image) {
                    part.getTransientData().put(IPresentationEngine.OVERRIDE_ICON_IMAGE_KEY, refreshed);
                }
                return refreshed;
            }
        } else if (model instanceof MItem item) {
            location = item.getIconURI();
        }
        if (refreshed != image) {
            return refreshed;
        }
        if (!SVGUtils.isThemeableSvg(location)) {
            return image;
        }
        IconDescriptor icon = getIconByLocation(location);
        return icon == null ? image : icon.image;
    }

    private static void refreshControl(@NotNull Control control) {
        if (control instanceof Label label) {
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
        } else if (control instanceof TabFolder folder) {
            for (TabItem item : folder.getItems()) {
                Image image = refreshedImage(item.getImage());
                if (image != item.getImage()) {
                    item.setImage(image);
                }
            }
        } else if (control instanceof CTabFolder folder) {
            for (CTabItem item : folder.getItems()) {
                Image image = refreshedModelImage(item, item.getImage());
                if (image != item.getImage()) {
                    item.setImage(image);
                }
            }
        } else if (control instanceof Tree tree) {
            for (TreeColumn column : tree.getColumns()) {
                Image image = refreshedImage(column.getImage());
                if (image != column.getImage()) {
                    column.setImage(image);
                }
            }
            if ((tree.getStyle() & SWT.VIRTUAL) == 0) {
                for (TreeItem item : tree.getItems()) {
                    refreshTreeItem(item, Math.max(1, tree.getColumnCount()));
                }
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
                    for (int index = 0; index < Math.max(1, table.getColumnCount()); index++) {
                        Image image = refreshedImage(item.getImage(index));
                        if (image != item.getImage(index)) {
                            item.setImage(index, image);
                        }
                    }
                }
            }
        }

        refreshMenu(control.getMenu());
        if (control instanceof Composite composite) {
            for (Control child : composite.getChildren()) {
                refreshControl(child);
            }
        }
    }

    private static void refreshTreeItem(@NotNull TreeItem item, int columns) {
        for (int index = 0; index < columns; index++) {
            Image image = refreshedImage(item.getImage(index));
            if (image != item.getImage(index)) {
                item.setImage(index, image);
            }
        }
        for (TreeItem child : item.getItems()) {
            refreshTreeItem(child, columns);
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
