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
package org.jkiss.dbeaver.ui.editors.sql.util;

import org.eclipse.jface.text.*;
import org.eclipse.ui.IEditorPart;

public abstract class AbstractSQLEditorTextHover implements ITextHover, ITextHoverExtension {

    public AbstractSQLEditorTextHover() {
    }

    /**
     * Associates a SQL editor with this hover. Subclass can cache it for later use.
     */
    public abstract void setEditor(IEditorPart editor);

    @Override
    public IInformationControlCreator getHoverControlCreator() {
        return parent -> {
            DefaultInformationControl control = new DefaultInformationControl(parent, true);
            control.setSizeConstraints(60, 10);
            return control;
        };
    }

    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        return SQLWordFinder.findWord(textViewer.getDocument(), offset);
    }

}