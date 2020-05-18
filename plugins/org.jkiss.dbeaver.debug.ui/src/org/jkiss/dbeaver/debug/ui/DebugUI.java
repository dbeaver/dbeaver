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

package org.jkiss.dbeaver.debug.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class DebugUI {

    public static final String BUNDLE_SYMBOLIC_NAME = "org.jkiss.dbeaver.debug.ui"; //$NON-NLS-1$
    public static final String DEBUG_LAUNCH_GROUP_ID = "org.jkiss.dbeaver.debug.launchGroup";
    public static final String DEBUG_OPEN_CONFIGURATION_COMMAND_ID = "org.jkiss.dbeaver.debug.ui.command.debugConfigurationMenu"; //$NON-NLS-1$

    public static DBGEditorAdvisor findEditorAdvisor(DBPDataSourceContainer dataSourceContainer) {
        DBGEditorAdvisor advisor = GeneralUtils.adapt(dataSourceContainer, DBGEditorAdvisor.class);
        if (advisor != null) {
            return advisor;
        }
        return null;
    }

    public static DBSObject extractDatabaseObject(IEditorPart editor) {
        if (editor != null) {
            IEditorInput editorInput = editor.getEditorInput();
            return editorInput.getAdapter(DBSObject.class);
        }
        return null;
    }

    public static IStatus createError(String message) {
        return new Status(IStatus.ERROR, BUNDLE_SYMBOLIC_NAME, message);
    }

}
