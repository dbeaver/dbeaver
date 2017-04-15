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
package org.jkiss.dbeaver.ui.data.managers.stream;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.impl.StringContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.utils.ContentUtils;

/**
* TextPanelEditor
*/
public class TextPanelEditor extends AbstractTextPanelEditor {

    @Override
    public StyledText createControl(IValueController valueController)
    {
        StyledText text = new StyledText(valueController.getEditPlaceholder(), SWT.MULTI | SWT.V_SCROLL);
        text.setEditable(!valueController.isReadOnly());
        text.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
        initEditorSettings(text);
        return text;
    }

    @Override
    public void primeEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull StyledText control, @NotNull DBDContent value) throws DBException
    {
        monitor.subTask("Read text value");
        if (value.isNull()) {
            control.setText("");
        } else {
            String strValue = ContentUtils.getContentStringValue(monitor, value);
            control.setText(strValue);
        }
    }

    @Override
    public void extractEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull StyledText control, @NotNull DBDContent value) throws DBException
    {
        monitor.subTask("Read text value");
        value.updateContents(
            monitor,
            new StringContentStorage(control.getText()));
    }

}
