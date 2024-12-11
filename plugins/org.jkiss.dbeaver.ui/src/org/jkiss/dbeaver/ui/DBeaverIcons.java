/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBIconComposite;
import org.jkiss.dbeaver.model.DBPImage;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * DBeaverIcons
 */
public class DBeaverIcons
{
    private static final Log log = Log.getLog(DBeaverIcons.class);

    private static final boolean useLegacyOverlay;

    static {
        boolean hasCachedImageDataProvider;
        try {
            Class.forName("org.eclipse.jface.resource.CompositeImageDescriptor$CachedImageDataProvider");
            hasCachedImageDataProvider = true;
        } catch (ClassNotFoundException e) {
            hasCachedImageDataProvider = false;
        }
        useLegacyOverlay = !hasCachedImageDataProvider;
    }

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

    private static Map<String, IconDescriptor> imageMap = new HashMap<>();
    private static Map<String, IconDescriptor> compositeMap = new HashMap<>();
    private static Image viewMenuImage;

    @NotNull
    public static Image getImage(@NotNull DBPImage image) {
        return getIconDescriptor(image, true).image;
    }

    @NotNull
    public static Image getImage(@NotNull DBPImage image, boolean useCache) {
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
    public static ImageDescriptor getImageDescriptor(@NotNull DBPImage image) {
        return getIconDescriptor(image, true).imageDescriptor; 
    }

    @NotNull
    private static IconDescriptor getIconDescriptor(DBPImage image, boolean useCache) {
        if (image == null) {
            return getIconDescriptor(DBIcon.TYPE_UNKNOWN, useCache);
        } else if (image instanceof DBIconBinary) {
            return new IconDescriptor(
                "[" + image.getLocation() + "]",
                ((DBIconBinary) image).getImage(),
                ((DBIconBinary) image).getImageDescriptor()
            );
        } else if (image instanceof DBIconComposite) {
            IconDescriptor icon = getIconDescriptor(((DBIconComposite) image).getMain(), useCache);
            return getCompositeIcon(icon, (DBIconComposite) image, useCache);
        } else if (image instanceof DBIcon) {
            IconDescriptor icon = getIconByLocation(image.getLocation());
            if (icon == null) {
                log.error("Image '" + image.getLocation() + "' not found");
                return getIconDescriptor(DBIcon.TYPE_UNKNOWN, useCache);
            } else {
                return icon;
            }
        } else {
            log.error("Unexpected image of type " + image.getClass());
            return getIconDescriptor(DBIcon.TYPE_UNKNOWN, useCache);
        }
    }

    private static IconDescriptor getCompositeIcon(IconDescriptor mainIcon, DBIconComposite image, boolean useCache) {
        if (!image.hasOverlays()) {
            return mainIcon;
        }
        String compositeId = mainIcon.id + "^" +
            (image.getTopLeft() == null ? "" : image.getTopLeft().getLocation()) + "^" +
            (image.getTopRight() == null ? "" : image.getTopRight().getLocation()) + "^" +
            (image.getBottomLeft() == null ? "" : image.getBottomLeft().getLocation()) + "^" +
            (image.getBottomRight() == null ? "" : image.getBottomRight().getLocation());
        IconDescriptor icon = useCache ? compositeMap.get(compositeId) : null;
        if (icon == null) {
            Image resultImage;
            if (useLegacyOverlay) {
                OverlayImageDescriptorLegacy ovrImage = new OverlayImageDescriptorLegacy(mainIcon.image.getImageData());
                if (image.getTopLeft() != null)
                    ovrImage.setTopLeft(accumulateDecorations(image, i -> i.getTopLeft()));
                if (image.getTopRight() != null)
                    ovrImage.setTopRight(accumulateDecorations(image, i -> i.getTopRight()));
                if (image.getBottomLeft() != null)
                    ovrImage.setBottomLeft(accumulateDecorations(image, i -> i.getBottomLeft()));
                if (image.getBottomRight() != null)
                    ovrImage.setBottomRight(accumulateDecorations(image, i -> i.getBottomRight()));
                resultImage = ovrImage.createImage();
            } else {
                OverlayImageDescriptor ovrImage = new OverlayImageDescriptor(mainIcon.imageDescriptor);
                if (image.getTopLeft() != null)
                    ovrImage.setTopLeft(accumulateDecorations(image, i -> i.getTopLeft()));
                if (image.getTopRight() != null)
                    ovrImage.setTopRight(accumulateDecorations(image, i -> i.getTopRight()));
                if (image.getBottomLeft() != null)
                    ovrImage.setBottomLeft(accumulateDecorations(image, i -> i.getBottomLeft()));
                if (image.getBottomRight() != null)
                    ovrImage.setBottomRight(accumulateDecorations(image, i -> i.getBottomRight()));
                resultImage = ovrImage.createImage();
            }
            icon = new IconDescriptor(compositeId, resultImage);
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
        if (base instanceof DBIconComposite) {
            List<ImageDescriptor> decorations = new ArrayList<>();
            decorations.add(getImageDescriptor(map.apply(image)));
            do {
                image = (DBIconComposite) base;
                decorations.add(getImageDescriptor(map.apply(image)));
                base = image.getMain();
            } while (base instanceof DBIconComposite);
            return decorations.toArray(ImageDescriptor[]::new);
        } else {
            return new ImageDescriptor[]{getImageDescriptor(map.apply(image))};
        }
    }

    private static IconDescriptor getIconByLocation(String location) {
        IconDescriptor icon = imageMap.get(location);
        if (icon == null) {
            try {
                ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(new URL(location));
                icon = new IconDescriptor(location, imageDescriptor);
                if (icon.image == null) {
                    log.warn("Bad image: " + location);
                    return null;
                } else {
                    imageMap.put(location, icon);
                }
            } catch (Exception e) {
                log.error(e);
                return null;
            }
        }
        return icon;
    }

}
