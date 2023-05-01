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
package org.jkiss.dbeaver.ui.editors.sql.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorExecutor;

public class SQLNativeExecutorDescriptor extends AbstractContextDescriptor {

    private static final Log log = Log.getLog(SQLNativeExecutorDescriptor.class);

    public static String EXTENSION_ID = "org.jkiss.dbeaver.sql.executors";
    private final ObjectType implClass;
    private final ObjectType supportedDataSource;

    public SQLNativeExecutorDescriptor(IConfigurationElement config) {
        super(config);
        this.implClass = new ObjectType(config.getAttribute("class"));
        this.supportedDataSource = new ObjectType(config.getAttribute("datasource"));
    }

    public static String getExtensionId() {
        return EXTENSION_ID;
    }

    /**
     * Returns native executor implementation
     */
    @Nullable
    public SQLEditorExecutor<? extends DBSObject> getNativeExecutor()  {
        try {
            return implClass.createInstance(SQLEditorExecutor.class);
        } catch (DBException e) {
            log.error("Error creating SQLEditorExecutor instance", e);
        }
        return null;
    }

    /**
     * Is it possible to use the executor with following datasource
     */
    public boolean isSupported(@NotNull DBPDataSource datasource) {
        return supportedDataSource.getObjectClass().isAssignableFrom(datasource.getClass());
    }

}
