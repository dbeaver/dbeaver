/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.colors;

import org.eclipse.jface.resource.StringConverter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UITextUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.utils.CommonUtils;

public class SetRowColorAction extends ColorAction {
    private ResultSetViewer resultSetViewer;
    private final DBDAttributeBinding attribute;
    private final Object value;
    public SetRowColorAction(ResultSetViewer resultSetViewer, DBDAttributeBinding attr, Object value) {
        super(resultSetViewer, NLS.bind(ResultSetMessages.actions_name_color_by,
            attr.getName() + " = " + UITextUtils.getShortText(resultSetViewer.getSizingGC(), CommonUtils.toString(value), 100)));
        this.resultSetViewer = resultSetViewer;
        this.attribute = attr;
        this.value = value;
    }

    @Override
    public void run() {
        RGB color;
        final Shell shell = UIUtils.createCenteredShell(resultSetViewer.getControl().getShell());
        try {
            ColorDialog cd = new ColorDialog(shell);
            color = cd.open();
            if (color == null) {
                return;
            }
        } finally {
            shell.dispose();
        }
        try {
            final DBVEntity vEntity = getColorsVirtualEntity();
            vEntity.setColorOverride(attribute, value, null, StringConverter.asString(color));
            updateColors(vEntity);
        } catch (IllegalStateException e) {
            DBWorkbench.getPlatformUI().showError(
                    "Row color",
                "Can't set row color",
                e);
        }
    }
}
