/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.gaussdb.ui.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBPackage;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreScriptObject;
import org.jkiss.dbeaver.ext.postgresql.ui.editors.PostgreSourceViewEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class GaussDBPackageDeclareViewEditor extends PostgreSourceViewEditor {

    @Override
    protected boolean isReadOnly() {
        return false;
    }

    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException {
        PostgreScriptObject object = getSourceObject();
        if (object instanceof GaussDBPackage) {
            GaussDBPackage sourceObject = (GaussDBPackage) object;
            return sourceObject.getObjectDefinitionText();
        }
        return "";
    }
}
