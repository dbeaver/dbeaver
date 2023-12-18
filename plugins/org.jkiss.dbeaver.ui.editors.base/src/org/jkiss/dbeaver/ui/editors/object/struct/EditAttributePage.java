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

package org.jkiss.dbeaver.ui.editors.object.struct;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.internal.EditorsMessages;

/**
 * EditAttributePage
 */
public class EditAttributePage extends PropertyObjectEditPage {

    public EditAttributePage(@Nullable DBECommandContext commandContext, @NotNull DBSObject object) {
        super(commandContext, object);
    }

    @Override
    protected String getPropertiesGroupTitle() {
        return EditorsMessages.dialog_struct_label_text_properties;
    }

    @Override
    protected void createAdditionalEditControls(Composite composite) {
        UIUtils.createControlGroup(composite, "Keys", 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);


    }

}
