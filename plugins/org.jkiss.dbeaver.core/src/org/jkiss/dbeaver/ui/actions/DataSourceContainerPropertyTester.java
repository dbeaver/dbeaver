/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.runtime.IPluginService;
import org.jkiss.dbeaver.runtime.qm.DefaultExecutionHandler;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

/**
 * DatabaseEditorPropertyTester
 */
public class DataSourceContainerPropertyTester extends PropertyTester
{
    static protected final Log log = Log.getLog(DataSourceContainerPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.datasourceContainer";
    public static final String PROP_DRIVER_ID = "driverId";
    public static final String PROP_DRIVER_CLASS = "driverClass";
    public static final String PROP_CONNECTED = "connected";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof DBPDataSourceContainer)) {
            return false;
        }
        DBPDataSourceContainer container = (DBPDataSourceContainer)receiver;
        switch (property) {
            case PROP_DRIVER_ID:
                return container.getDriver().getId().equals(expectedValue);
            case PROP_DRIVER_CLASS:
                return container.getDriver().getDriverClassName().equals(expectedValue);
            case PROP_CONNECTED:
                return container.isConnected();
        }
        return false;
    }

}