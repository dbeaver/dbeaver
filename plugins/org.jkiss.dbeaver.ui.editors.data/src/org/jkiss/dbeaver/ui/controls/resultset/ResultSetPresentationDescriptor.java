/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
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
    private boolean supportsRecordMode;
    private boolean supportsPanels;
    private boolean supportsNavigation;
    private boolean supportsEdit;

    protected ResultSetPresentationDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.implClass = new ObjectType(config.getAttribute("class"));
        this.icon = iconToImage(config.getAttribute("icon"));
        this.order = CommonUtils.toInt(config.getAttribute("order"));
        this.presentationType = IResultSetPresentation.PresentationType.valueOf(config.getAttribute("type").toUpperCase(Locale.ENGLISH));
        this.supportsRecordMode = CommonUtils.toBoolean(config.getAttribute("supportsRecordMode"));
        this.supportsPanels = CommonUtils.getBoolean(config.getAttribute("supportsPanels"), true);
        this.supportsNavigation = CommonUtils.getBoolean(config.getAttribute("supportsNavigation"), true);
        this.supportsEdit = CommonUtils.getBoolean(config.getAttribute("supportsEdit"), true);

        for (IConfigurationElement typeCfg : config.getChildren(CONTENT_TYPE)) {
            String type = typeCfg.getAttribute("type");
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

    public boolean supportsRecordMode() {
        return supportsRecordMode;
    }

    public boolean supportsPanels() {
        return supportsPanels;
    }

    public boolean supportsNavigation() {
        return supportsNavigation;
    }

    public boolean supportsEdit() {
        return supportsEdit;
    }

    @Override
    public String toString() {
        return id;
    }

}
