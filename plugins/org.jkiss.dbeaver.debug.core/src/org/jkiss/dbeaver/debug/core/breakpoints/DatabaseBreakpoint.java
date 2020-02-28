/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

package org.jkiss.dbeaver.debug.core.breakpoints;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.Breakpoint;
import org.jkiss.dbeaver.debug.DBGBreakpointDescriptor;
import org.jkiss.dbeaver.debug.DBGConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class DatabaseBreakpoint extends Breakpoint implements IDatabaseBreakpoint {

    @Override
    public String getModelIdentifier() {
        return DBGConstants.MODEL_IDENTIFIER_DATABASE;
    }

    @Override
    public String getDatasourceId() throws CoreException {
        return ensureMarker().getAttribute(DBGConstants.BREAKPOINT_ATTRIBUTE_DATASOURCE_ID, null);
    }

    @Override
    public String getNodePath() throws CoreException {
        return ensureMarker().getAttribute(DBGConstants.BREAKPOINT_ATTRIBUTE_NODE_PATH, null);
    }

    @Override
    public String getObjectName() throws CoreException {
        return ensureMarker().getAttribute(DBGConstants.BREAKPOINT_ATTRIBUTE_OBJECT_NAME, null);
    }

    protected void register(boolean register) throws CoreException {
        if (register) {
            DebugPlugin plugin = DebugPlugin.getDefault();
            if (plugin != null) {
                plugin.getBreakpointManager().addBreakpoint(this);
            }
        } else {
            setRegistered(false);
        }
    }

    protected void addDatabaseBreakpointAttributes(Map<String, Object> attributes, DBSObject databaseObject, DBNNode node, DBGBreakpointDescriptor breakpointDescriptor) {
        attributes.put(DBGConstants.BREAKPOINT_ATTRIBUTE_DATASOURCE_ID, databaseObject.getDataSource().getContainer().getId());
        attributes.put(DBGConstants.BREAKPOINT_ATTRIBUTE_NODE_PATH, node.getNodeItemPath());
        attributes.put(DBGConstants.BREAKPOINT_ATTRIBUTE_OBJECT_NAME, DBUtils.getObjectFullName(databaseObject, DBPEvaluationContext.UI));
        attributes.putAll(breakpointDescriptor.toMap());
    }
}
