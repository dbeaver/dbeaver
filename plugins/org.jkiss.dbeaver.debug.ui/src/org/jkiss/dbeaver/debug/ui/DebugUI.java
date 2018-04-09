/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class DebugUI {

    public static final String BUNDLE_SYMBOLIC_NAME = "org.jkiss.dbeaver.debug.ui"; //$NON-NLS-1$

    private static final Log log = Log.getLog(DebugUI.class);

    public static DebugEditorAdvisor findEditorAdvisor(DBPDataSourceContainer dataSourceContainer) {
        DebugEditorAdvisor advisor = Adapters.adapt(dataSourceContainer, DebugEditorAdvisor.class);
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

    public static void log(IStatus status) {
        Log.log(log, status);
    }

}
