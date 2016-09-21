/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.data.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.ui.data.IStreamValueManager;
import org.jkiss.utils.CommonUtils;

/**
 * StreamValueManagerDescriptor
 */
public class StreamValueManagerDescriptor extends AbstractDescriptor
{
    public static final String TAG_STREAM_MANAGER = "streamManager"; //$NON-NLS-1$
    private static final String ATTR_PRIMARY_MIME = "primaryMime";
    private static final String ATTR_SUPPORTED_MIME = "supportedMime";

    private String id;
    private ObjectType implType;
    private final String label;
    private final String description;
    private final DBPImage icon;
    private final String primaryMime;
    private final String[] supportedMime;

    private IStreamValueManager instance;

    public StreamValueManagerDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.implType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));

        this.primaryMime = config.getAttribute(ATTR_PRIMARY_MIME);
        this.supportedMime = CommonUtils.notEmpty(config.getAttribute(ATTR_SUPPORTED_MIME)).split(",");
    }

    public String getId()
    {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public DBPImage getIcon() {
        return icon;
    }

    public String[] getSupportedMime() {
        return supportedMime;
    }

    public String getPrimaryMime() {
        return primaryMime;
    }

    @NotNull
    public IStreamValueManager getInstance()
    {
        if (instance == null) {
            try {
                this.instance = implType.createInstance(IStreamValueManager.class);
            }
            catch (Exception e) {
                throw new IllegalStateException("Can't instantiate content value manager '" + this.id + "'", e); //$NON-NLS-1$
            }
        }
        return instance;
    }

    @Override
    public String toString() {
        return id + " (" + label + ")";
    }
}