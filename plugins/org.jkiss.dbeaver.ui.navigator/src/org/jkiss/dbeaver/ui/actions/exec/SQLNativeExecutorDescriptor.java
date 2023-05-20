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
package org.jkiss.dbeaver.ui.actions.exec;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class SQLNativeExecutorDescriptor extends AbstractContextDescriptor {

    private static final Log log = Log.getLog(SQLNativeExecutorDescriptor.class);

    private final ObjectType implClass;
    private final String dataSourceId;
    private SQLScriptExecutor<? extends DBSObject> instance = null;

    public SQLNativeExecutorDescriptor(IConfigurationElement config) {
        super(config);
        this.implClass = new ObjectType(config.getAttribute("class"));
        this.dataSourceId = config.getAttribute("datasource");
    }

    /**
     * Returns native executor implementation
     */
    @Nullable
    public SQLScriptExecutor<? extends DBSObject> getNativeExecutor() throws DBException {
        if (instance == null) {
            instance = implClass.createInstance(SQLScriptExecutor.class);
        }
        return instance;
    }

    @NotNull
    public String getDataSourceId() {
        return dataSourceId;
    }
}
