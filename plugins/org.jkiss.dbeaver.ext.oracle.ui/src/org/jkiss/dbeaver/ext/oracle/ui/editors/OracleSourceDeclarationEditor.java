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

package org.jkiss.dbeaver.ext.oracle.ui.editors;

import org.eclipse.jface.action.IContributionManager;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Oracle source declaration editor
 */
public class OracleSourceDeclarationEditor extends SQLSourceViewer<OracleSourceObject> implements OracleDDLOptions {
    private Map<String, Object> oracleDDLOptions = new HashMap<>();

    @Override
    protected String getCompileCommandId()
    {
        return OracleConstants.CMD_COMPILE;
    }

    @Override
    protected void setSourceText(DBRProgressMonitor monitor, String sourceText) {
        getInputPropertySource().setPropertyValue(
            monitor,
            OracleConstants.PROP_OBJECT_DEFINITION,
            sourceText);
    }

    @Override
    protected boolean isReadOnly() {
        return false;
    }

    @Override
    protected void contributeEditorCommands(IContributionManager toolBarManager) {
        super.contributeEditorCommands(toolBarManager);

        if (getSourceObject() instanceof OracleTableBase) {
            OracleTableBase sourceObject = (OracleTableBase) getSourceObject();
            OracleEditorUtils.addDDLControl(toolBarManager, sourceObject, this);
        }
    }

    @Override
    public void putDDLOptions(String name, Object value) {
        oracleDDLOptions.put(name, value);
    }

    @Override
    protected Map<String, Object> getSourceOptions() {
        Map<String, Object> options = super.getSourceOptions();
        if (!CommonUtils.isEmpty(oracleDDLOptions)) {
            options.putAll(oracleDDLOptions);
        }
        return options;
    }
}
