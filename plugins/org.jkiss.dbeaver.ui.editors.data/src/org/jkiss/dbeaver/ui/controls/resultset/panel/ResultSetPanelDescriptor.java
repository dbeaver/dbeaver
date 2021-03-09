/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.controls.resultset.panel;

import org.apache.commons.jexl3.JexlExpression;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetContext;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPanel;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ResultSetPresentationDescriptor
 */
public class ResultSetPanelDescriptor extends AbstractContextDescriptor {
    private static final Log log = Log.getLog(ResultSetPanelDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resultset.panel"; //NON-NLS-1 //$NON-NLS-1$

    public static final String TAG_SUPPORTS = "supports"; //$NON-NLS-1$

    private final String id;
    private final String label;
    private final String description;
    private final ObjectType implClass;
    private final DBPImage icon;
    private final boolean showByDefault;
    private final List<IResultSetPresentation.PresentationType> supportedPresentationTypes = new ArrayList<>();
    private final List<String> supportedPresentations = new ArrayList<>();
    private final List<String> supportedDataSources = new ArrayList<>();
    private final List<JexlExpression> supportedExpressions = new ArrayList<>();

    public ResultSetPanelDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.implClass = new ObjectType(config.getAttribute("class"));
        this.icon = iconToImage(config.getAttribute("icon"));
        this.showByDefault = CommonUtils.toBoolean(config.getAttribute("default"));

        for (IConfigurationElement supports : config.getChildren(TAG_SUPPORTS)) {
            String type = supports.getAttribute("type");
            if (!CommonUtils.isEmpty(type)) {
                supportedPresentationTypes.add(IResultSetPresentation.PresentationType.valueOf(type.toUpperCase(Locale.ENGLISH)));
            }
            String id = supports.getAttribute("id");
            if (!CommonUtils.isEmpty(id)) {
                supportedPresentations.add(id);
            }

            String expr = supports.getAttribute("if");
            if (!CommonUtils.isEmpty(expr)) {
                try {
                    supportedExpressions.add(parseExpression(expr));
                } catch (DBException e) {
                    log.debug(e);
                }
            }
        }

        for (IConfigurationElement dsElement : config.getChildren("datasource")) {
            String dsId = dsElement.getAttribute("id");
            if (dsId != null) {
                supportedDataSources.add(dsId);
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

    public boolean isShowByDefault() {
        return showByDefault;
    }

    public boolean supportedBy(IResultSetContext context, DBPDataSource dataSource, String presentationId, IResultSetPresentation.PresentationType presentationType) {
        if (!supportedDataSources.isEmpty()) {
            if (dataSource == null) {
                return false;
            }
            if (!supportsAnyProvider(dataSource)) {
                return false;
            }
        }
        if (!supportedExpressions.isEmpty()) {
            for (JexlExpression expr : supportedExpressions) {
                if (!Boolean.TRUE.equals(expr.evaluate(AbstractDescriptor.makeContext(dataSource, context)))) {
                    return false;
                }
            }
        }
        if (supportedPresentations.isEmpty() && supportedPresentationTypes.isEmpty()) {
            return true;
        }
        return
            (presentationId != null && supportedPresentations.contains(presentationId)) ||
            (presentationType != null && supportedPresentationTypes.contains(presentationType));
    }

    private boolean supportsAnyProvider(DBPDataSource dataSource) {
        for (DBPDataSourceProviderDescriptor provider = dataSource.getContainer().getDriver().getProviderDescriptor();
            provider != null;
            provider = provider.getParentProvider())
        {
            if (supportedDataSources.contains(provider.getId())) {
                return true;
            }
        }
        return false;
    }

    public IResultSetPanel createInstance() throws DBException {
        return implClass.createInstance(IResultSetPanel.class);
    }

    @Override
    public String toString() {
        return id;
    }
}
