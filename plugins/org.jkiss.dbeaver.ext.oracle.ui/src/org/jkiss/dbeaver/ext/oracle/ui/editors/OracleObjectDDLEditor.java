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
import org.jkiss.dbeaver.ext.oracle.model.OracleTable;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * OracleObjectDDLEditor
 */
public class OracleObjectDDLEditor extends SQLSourceViewer<OracleTable> implements OracleDDLOptions{
    private Map<String, Object> oracleDDLOptions = new HashMap<>();

    public OracleObjectDDLEditor()
    {
    }

    @Override
    protected void contributeEditorCommands(IContributionManager contributionManager)
    {
        super.contributeEditorCommands(contributionManager);
        OracleTable sourceObject = getSourceObject();
        OracleEditorUtils.addDDLControl(contributionManager, sourceObject, this);
    }

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