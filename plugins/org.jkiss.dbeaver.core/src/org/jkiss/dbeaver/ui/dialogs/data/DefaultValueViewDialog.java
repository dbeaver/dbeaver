/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.editors.ReferenceValueEditor;

/**
 * Default value view dialog.
 * Uses panel editor inside of value viewer.
 */
public class DefaultValueViewDialog extends ValueViewDialog {

    private static final Log log = Log.getLog(DefaultValueViewDialog.class);

    private IValueEditor panelEditor;

    public DefaultValueViewDialog(IValueController valueController) {
        super(valueController);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        Label label = new Label(dialogGroup, SWT.NONE);
        label.setText(CoreMessages.dialog_data_label_value);

        Composite editorPlaceholder = UIUtils.createPlaceholder(dialogGroup, 1);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = label.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + 4;
        editorPlaceholder.setLayoutData(gd);
        editorPlaceholder.setLayout(new FillLayout());

        try {
            panelEditor = createPanelEditor(editorPlaceholder);
            if (panelEditor == null) {
                return dialogGroup;
            }
            panelEditor.primeEditorValue(getValueController().getValue());
        } catch (DBException e) {
            log.error(e);
            return dialogGroup;
        }
        ReferenceValueEditor referenceValueEditor = new ReferenceValueEditor(getValueController(), this);
        if (referenceValueEditor.isReferenceValue()) {
            referenceValueEditor.createEditorSelector(dialogGroup);
        }

        return dialogGroup;
    }

    @Override
    public Object extractEditorValue()
        throws DBException
    {
        return panelEditor.extractEditorValue();
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        panelEditor.primeEditorValue(value);
    }

    @Override
    public boolean isDirty() {
        return panelEditor.isDirty();
    }

    @Override
    public void setDirty(boolean dirty) {
        panelEditor.setDirty(dirty);
    }

    @Override
    public Control getControl()
    {
        return panelEditor.getControl();
    }

}