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
package org.jkiss.dbeaver.ui.controls.findandreplace;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.internal.texteditor.TextEditorPlugin;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.osgi.framework.Bundle;

import java.net.URL;

/**
 * Provides Icons for the editor overlay used for performing
 * find/replace-operations.
 * <p>
 * Derived from <a href="https://github.com/eclipse-platform/eclipse.platform.ui/blob/master/bundles/org.eclipse.ui.workbench.texteditor/src/org/eclipse/ui/internal/findandreplace/overlay/FindReplaceOverlayImages.java">eclipse.platform.ui</a>
 */
class FindReplaceOverlayImages {
    private static final String PREFIX_ELCL = TextEditorPlugin.PLUGIN_ID + ".elcl."; //$NON-NLS-1$
    static final String KEY_CLOSE = PREFIX_ELCL + "close"; //$NON-NLS-1$
    static final String KEY_FIND_NEXT = PREFIX_ELCL + "select_next"; //$NON-NLS-1$
    static final String KEY_FIND_PREV = PREFIX_ELCL + "select_prev"; //$NON-NLS-1$
    static final String KEY_FIND_REGEX = PREFIX_ELCL + "regex"; //$NON-NLS-1$
    static final String KEY_REPLACE = PREFIX_ELCL + "replace"; //$NON-NLS-1$
    static final String KEY_REPLACE_ALL = PREFIX_ELCL + "replace_all"; //$NON-NLS-1$
    static final String KEY_WHOLE_WORD = PREFIX_ELCL + "whole_word"; //$NON-NLS-1$
    static final String KEY_CASE_SENSITIVE = PREFIX_ELCL + "case_sensitive"; //$NON-NLS-1$
    static final String KEY_SEARCH_ALL = PREFIX_ELCL + "search_all"; //$NON-NLS-1$
    static final String KEY_SEARCH_IN_AREA = PREFIX_ELCL + "search_in_selection"; //$NON-NLS-1$
    static final String KEY_OPEN_REPLACE_AREA = PREFIX_ELCL + "open_replace"; //$NON-NLS-1$
    static final String KEY_CLOSE_REPLACE_AREA = PREFIX_ELCL + "close_replace"; //$NON-NLS-1$
    static final String KEY_OPEN_HISTORY = "open_history"; //$NON-NLS-1$

    /**
     * The image registry containing {@link Image images}.
     */
    @Nullable
    private static ImageRegistry fgImageRegistry;

    @NotNull
    private static final String ICONS_PATH = "$nl$/icons/full/"; //$NON-NLS-1$

    @NotNull
    private static final String ELCL = ICONS_PATH + "elcl16/"; //$NON-NLS-1$

    /**
     * Declare all images
     */
    private static void declareImages(@NotNull ImageRegistry reg) {
        declareRegistryImage(reg, KEY_CLOSE, ELCL + "close.svg"); //$NON-NLS-1$
        declareRegistryImage(reg, KEY_FIND_NEXT, ELCL + "select_next.svg"); //$NON-NLS-1$
        declareRegistryImage(reg, KEY_FIND_PREV, ELCL + "select_prev.svg"); //$NON-NLS-1$
        declareRegistryImage(reg, KEY_FIND_REGEX, ELCL + "regex.svg"); //$NON-NLS-1$
        declareRegistryImage(reg, KEY_REPLACE_ALL, ELCL + "replace_all.svg"); //$NON-NLS-1$
        declareRegistryImage(reg, KEY_REPLACE, ELCL + "replace.svg"); //$NON-NLS-1$
        declareRegistryImage(reg, KEY_WHOLE_WORD, ELCL + "whole_word.svg"); //$NON-NLS-1$
        declareRegistryImage(reg, KEY_CASE_SENSITIVE, ELCL + "case_sensitive.svg"); //$NON-NLS-1$
        declareRegistryImage(reg, KEY_SEARCH_ALL, ELCL + "search_all.svg"); //$NON-NLS-1$
        declareRegistryImage(reg, KEY_SEARCH_IN_AREA, ELCL + "search_in_area.svg"); //$NON-NLS-1$
        declareRegistryImage(reg, KEY_OPEN_REPLACE_AREA, ELCL + "open_replace.svg"); //$NON-NLS-1$
        declareRegistryImage(reg, KEY_CLOSE_REPLACE_AREA, ELCL + "close_replace.svg"); //$NON-NLS-1$
        declareRegistryImage(reg, KEY_OPEN_HISTORY, ELCL + "open_history.svg"); //$NON-NLS-1$
    }

    /**
     * Declare an Image in the registry table.
     *
     * @param key  the key to use when registering the image
     * @param path the path where the image can be found. This path is relative to
     *             where this plugin class is found (i.e. typically the packages
     *             directory)
     */
    private static void declareRegistryImage(@NotNull ImageRegistry fgImageRegistry, @NotNull String key, @NotNull String path) {
        if (fgImageRegistry.get(key) == null) {
            ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
            Bundle bundle = Platform.getBundle(TextEditorPlugin.PLUGIN_ID);
            if (bundle != null) {
                URL url = FileLocator.find(bundle, IPath.fromOSString(path), null);
                desc = ImageDescriptor.createFromURL(url);
            }
            fgImageRegistry.put(key, desc);
        }
    }

    /**
     * Returns the ImageRegistry.
     *
     * @return image registry
     */
    @NotNull
    public static ImageRegistry getImageRegistry() {
        if (fgImageRegistry == null) {
            fgImageRegistry = initializeImageRegistry();
        }
        return fgImageRegistry;
    }

    /**
     * Initialize the image registry by declaring all of the required graphics. This
     * involves creating JFace image descriptors describing how to create/find the
     * image should it be needed. The image is not actually allocated until
     * requested.
     * <p>
     * Prefix conventions Wizard Banners WIZBAN_ Preference Banners PREF_BAN_
     * Property Page Banners PROPBAN_ Color toolbar CTOOL_ Enable toolbar ETOOL_
     * Disable toolbar DTOOL_ Local enabled toolbar ELCL_ Local Disable toolbar
     * DLCL_ Object large OBJL_ Object small OBJS_ View VIEW_ Product images PROD_
     * Misc images MISC_
     * <p>
     * Where are the images? The images (typically SVGs) are found in the same
     * location as this plugin class. This may mean the same package directory as
     * the package holding this class. The images are declared using this.getClass()
     * to ensure they are looked up via this plugin class.
     *
     * @return the image registry
     * @see org.eclipse.jface.resource.ImageRegistry
     */
    @NotNull
    private static ImageRegistry initializeImageRegistry() {
        ImageRegistry fgImageRegistry = TextEditorPlugin.getDefault().getImageRegistry();
        declareImages(fgImageRegistry);
        return fgImageRegistry;
    }

    /**
     * Returns the image managed under the given key in this registry.
     *
     * @param key the image's key
     * @return the image managed under the given key
     */
    @Nullable
    public static Image get(@NotNull String key) {
        return getImageRegistry().get(key);
    }

    /**
     * Returns the image descriptor for the given key in this registry.
     *
     * @param key the image's key
     * @return the image descriptor for the given key
     */
    @Nullable
    public static ImageDescriptor getDescriptor(@NotNull String key) {
        return getImageRegistry().getDescriptor(key);
    }
}
