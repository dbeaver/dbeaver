/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.dialogs;

import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.ui.editors.sql.util.ObjectInformationView;

public class SuggestionInformationControl extends AbstractInformationControl implements IInformationControlExtension2 {

    private ObjectInformationView objectInformationView;

    public SuggestionInformationControl(Shell parentShell, boolean isResizable) {
        super(parentShell, isResizable);
        create();
    }

    @Override
    protected void createContent(Composite parent) {
        this.objectInformationView = new ObjectInformationView();
        this.objectInformationView.createContent(parent);
    }

    @Override
    public boolean hasContents() {
        return this.objectInformationView.hasContents();
    }

    @Override
    public void setInput(Object input) {
        this.objectInformationView.setInput(input);
    }

}
