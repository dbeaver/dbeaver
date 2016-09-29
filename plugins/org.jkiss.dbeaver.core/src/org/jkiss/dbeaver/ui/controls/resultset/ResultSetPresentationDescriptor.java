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

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.registry.AbstractContextDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.MimeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ResultSetPresentationDescriptor
 */
public class ResultSetPresentationDescriptor extends AbstractContextDescriptor {

    private static final Log log = Log.getLog(ResultSetPresentationDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resultset.presentation"; //NON-NLS-1 //$NON-NLS-1$

    private static final String CONTENT_TYPE = "contentType";

    private final String id;
    private final String label;
    private final String description;
    private final ObjectType implClass;
    private final DBPImage icon;
    private final int order;
    private final IResultSetPresentation.PresentationType presentationType;
    private final List<MimeType> contentTypes = new ArrayList<>();

    protected ResultSetPresentationDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.implClass = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        this.order = CommonUtils.toInt(config.getAttribute(RegistryConstants.ATTR_ORDER));
        this.presentationType = IResultSetPresentation.PresentationType.valueOf(config.getAttribute(RegistryConstants.ATTR_TYPE).toUpperCase(Locale.ENGLISH));

        for (IConfigurationElement typeCfg : config.getChildren(CONTENT_TYPE)) {
            String type = typeCfg.getAttribute(RegistryConstants.ATTR_TYPE);
            try {
                MimeType contentType = new MimeType(type);
                contentTypes.add(contentType);
            } catch (Throwable e) {
                log.warn("Invalid content type: " + type, e);
            }
        }
    }

    public String getId() {
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

    public int getOrder() {
        return order;
    }

    public IResultSetPresentation.PresentationType getPresentationType() {
        return presentationType;
    }

    public boolean supportedBy(DBCResultSet resultSet, IResultSetContext context) {
        return appliesTo(resultSet, context) || matchesContentType(context);
    }

    public IResultSetPresentation createInstance() throws DBException {
        return implClass.createInstance(IResultSetPresentation.class);
    }

    public boolean matches(Class<? extends IResultSetPresentation> type) {
        return implClass.matchesType(type);
    }

    private boolean matchesContentType(IResultSetContext context) {
        String documentType = context.getDocumentContentType();
        if (contentTypes.isEmpty() || CommonUtils.isEmpty(documentType)) {
            return false;
        }
        for (MimeType mimeType : contentTypes) {
            try {
                if (mimeType.match(documentType)) {
                    return true;
                }
            } catch (Throwable e) {
                log.warn("Bad document content type: " + documentType, e);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return id;
    }
}
