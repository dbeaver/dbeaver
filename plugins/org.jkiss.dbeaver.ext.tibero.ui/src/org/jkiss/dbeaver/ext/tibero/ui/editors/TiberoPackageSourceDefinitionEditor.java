/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.tibero.ui.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.tibero.TiberoConstants;
import org.jkiss.dbeaver.ext.tibero.model.source.TiberoSourceObject;
import org.jkiss.dbeaver.ext.tibero.model.source.TiberoSourceType;
import org.jkiss.dbeaver.ui.IRefreshablePart.RefreshResult;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

import java.util.Map;

public class TiberoPackageSourceDefinitionEditor extends SQLSourceViewer<TiberoSourceObject> {

    private boolean savedDirtySource;

    @Override
    public void doSave(IProgressMonitor progressMonitor) {
        savedDirtySource = isDirty();
        super.doSave(new NullProgressMonitor());
    }

    @Override
    public void runPostSaveCommands(Map<String, Object> context) {
        if (savedDirtySource) {
            savedDirtySource = false;
            TiberoSourceEditorUtils.runPostSave(this, context, TiberoSourceType.PACKAGE_BODY);
        }
    }

    @Override
    public RefreshResult refreshPart(Object source, boolean force) {
        if (!force) {
            return RefreshResult.IGNORED;
        }
        return super.refreshPart(source, true);
    }

    @Override
    protected String getCompileCommandId() {
        return TiberoConstants.CMD_COMPILE;
    }

    @Override
    protected String getSourceText(org.jkiss.dbeaver.model.runtime.DBRProgressMonitor monitor) throws DBException {
        return getSourceObject() == null ? "" : ((org.jkiss.dbeaver.model.DBPScriptObjectExt) getSourceObject()).getExtendedDefinitionText(monitor);
    }

    @Override
    protected void setSourceText(org.jkiss.dbeaver.model.runtime.DBRProgressMonitor monitor, String sourceText) {
        getInputPropertySource().setPropertyValue(
            monitor,
            TiberoConstants.PROP_OBJECT_BODY_DEFINITION,
            sourceText
        );
    }

    @Override
    protected boolean isReadOnly() {
        return false;
    }
}
