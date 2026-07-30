/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0
 */
package org.jkiss.dbeaver.ext.polardbx.ui.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

    private static final String PLUGIN_ID = "org.jkiss.dbeaver.ext.polardbx.ui";

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }
}