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
package org.jkiss.dbeaver.ext.mimer.ui.editors;

import org.jkiss.dbeaver.ext.mimer.model.MimerModule;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

/**
 * Editable Source tab for {@link MimerModule}. A self-contained copy of {@code
 * GenericSourceViewEditor}'s (tiny) logic, kept local to this plugin on purpose: {@code
 * ext.generic.ui}'s {@code MANIFEST.MF} doesn't export the package that class lives in (only
 * {@code .views}), so referencing it directly from {@code mimer.ui}'s {@code plugin.xml} fails
 * silently - {@code Require-Bundle} doesn't grant access to a bundle's non-exported packages,
 * and there's no exception anywhere to reveal it. Copying the logic locally avoids both that
 * trap and needing to touch a plugin outside this codebase.
 *
 * @author Mimer Information Technology
 */
public class MimerModuleSourceViewEditor extends SQLSourceViewer<MimerModule> {

    @Override
    protected boolean isReadOnly() {
        return !(getSourceObject() instanceof DBSObjectWithScript);
    }

    @Override
    protected void setSourceText(DBRProgressMonitor monitor, String sourceText) {
        getInputPropertySource().setPropertyValue(monitor, "objectDefinitionText", sourceText);
    }
}
