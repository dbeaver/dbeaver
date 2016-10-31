/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.data.managers.stream;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.StringContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.data.IStreamValueEditor;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.editors.ContentPanelEditor;
import org.jkiss.dbeaver.utils.ContentUtils;

/**
* TextPanelEditor
*/
public class TextPanelEditor implements IStreamValueEditor<StyledText> {

    @Override
    public StyledText createControl(IValueController valueController)
    {
        StyledText text = new StyledText(valueController.getEditPlaceholder(), SWT.MULTI | SWT.V_SCROLL);
        text.setEditable(!valueController.isReadOnly());
        text.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
        ContentPanelEditor.setEditorSettings(text);
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

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull final StyledText control) throws DBCException {
    }

}
