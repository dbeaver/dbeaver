/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.internal;

import org.eclipse.core.runtime.*;
import org.eclipse.gef.internal.InternalImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.ui.notations.ERDNotationDescriptor;
import org.jkiss.dbeaver.erd.ui.notations.ERDNotationService;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator class controls the plug-in life cycle
 */
public class ERDUIActivator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.jkiss.dbeaver.erd.ui";
    public static final String ERD_STYLE_NOTATION_EXT = "org.jkiss.dbeaver.erd.ui.notation.style";
    private static final Log log = Log.getLog(ERDUIActivator.class);
    private static ERDUIActivator plugin;
    private static BundleContext context;
    private DBPPreferenceStore preferences;
    private ERDNotationService<ERDNotationDescriptor> erdNotationService;
    private ServiceTracker<ERDNotationService<ERDNotationDescriptor>, ERDNotationService<ERDNotationDescriptor>> serviceTracker;

    public ERDUIActivator() {
        // no specific
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        ERDUIActivator.context = context;
        ERDUIActivator.plugin = this;
        preferences = new BundlePreferenceStore(getBundle());
        registerERDService(context);
        registerERDNotations();
        // Switch off D3D because of Sun XOR painting bug
        // See http://www.jgraph.com/forum/viewtopic.php?t=4066
        System.setProperty("sun.java2d.d3d", Boolean.FALSE.toString()); //$NON-NLS-1$
        // Overload GEF images
        overloadGEFImage();
    }

    private void registerERDService(BundleContext context) {
        String erdServiceClassName = ERDNotationService.class.getName();
        serviceTracker = new ServiceTracker<>(context, erdServiceClassName, null);
        serviceTracker.open();
        erdNotationService = serviceTracker.getService();
        if (erdNotationService == null) {
            log.error("OSGI Service ERDNotationService is unreachable or null");
        }
    }

    private void registerERDNotations() {
        IExtensionRegistry registry = RegistryFactory.getRegistry();
        IExtensionPoint ep = registry.getExtensionPoint(PLUGIN_ID, ERD_STYLE_NOTATION_EXT);
        if (ep == null) {
            ep = registry.getExtensionPoint(ERD_STYLE_NOTATION_EXT);
            if (ep == null) {
                log.error("Extension point:[" + ERD_STYLE_NOTATION_EXT + "] not found");
                return;
            }
        }
        IConfigurationElement[] configurationElements = ep.getConfigurationElements();
        for (IConfigurationElement cf : configurationElements) {
            try {
                ERDNotationDescriptor notationDescriptor = new ERDNotationDescriptor(cf);
                if (erdNotationService != null) {
                    erdNotationService.addNotation(notationDescriptor);
                }
            } catch (CoreException e) {
                log.error(e.getStatus());
            }
        }
    }

    private void overloadGEFImage() {
        try {
            // Use reflection because of Eclipse API incompatibility with oder versions
            InternalImages.class.getMethod(
                "set",
                String.class, Image.class)
                .invoke(null, InternalImages.IMG_PALETTE, DBeaverIcons.getImage(UIIcon.PALETTE));
        } catch (Throwable e) {
            log.debug(e);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static ERDUIActivator getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in relative
     * path
     *
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public DBPPreferenceStore getPreferences() {
        return preferences;
    }

    public ERDNotationService<ERDNotationDescriptor> getERDNotationService() {
        return erdNotationService;
    }
}
