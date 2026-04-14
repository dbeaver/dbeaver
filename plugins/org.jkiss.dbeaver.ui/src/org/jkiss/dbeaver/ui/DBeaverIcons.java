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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
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
        String compositeId = mainIcon.id + "^" +
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
        IconDescriptor icon = imageMap.get(location);
        if (icon == null) {
            try {
                ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(URI.create(location).toURL());
                icon = new IconDescriptor(location, imageDescriptor);
                imageMap.put(location, icon);
            } catch (Exception e) {
                log.error(e);
                return null;
            }
        }
        return icon;
    }

}
