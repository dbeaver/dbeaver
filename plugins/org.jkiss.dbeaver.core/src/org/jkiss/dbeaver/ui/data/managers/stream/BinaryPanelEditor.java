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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.BytesContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.data.IStreamValueEditor;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.editors.binary.BinaryContent;
import org.jkiss.dbeaver.ui.editors.binary.HexEditControl;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
* ControlPanelEditor
*/
public class BinaryPanelEditor implements IStreamValueEditor<HexEditControl> {

    private static final Log log = Log.getLog(BinaryPanelEditor.class);

    @Override
    public HexEditControl createControl(IValueController valueController)
    {
        return new HexEditControl(valueController.getEditPlaceholder(), SWT.BORDER);
    }

    @Override
    public void primeEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull HexEditControl control, @NotNull DBDContent value) throws DBException
    {
        monitor.beginTask("Prime content value", 1);
        try {
            DBDContentStorage data = value.getContents(monitor);
            monitor.subTask("Read binary value");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            if (data != null) {
                try (InputStream contentStream = data.getContentStream()){
                    ContentUtils.copyStreams(contentStream, -1, buffer, monitor);
                }
            }
            control.setContent(buffer.toByteArray());
        } catch (IOException e) {
            throw new DBException("Error reading stream value", e);
        } finally {
            monitor.done();
        }
    }

    @Override
    public void extractEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull HexEditControl control, @NotNull DBDContent value) throws DBException
    {
        BinaryContent binaryContent = control.getContent();
        ByteBuffer buffer = ByteBuffer.allocate((int) binaryContent.length());
        try {
            binaryContent.get(buffer, 0);
        } catch (IOException e) {
            log.error(e);
        }
        value.updateContents(
            monitor,
            new BytesContentStorage(buffer.array(), GeneralUtils.getDefaultFileEncoding()));
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull final HexEditControl control) throws DBCException {
        manager.add(new Action("Switch Insert/Overwrite mode", DBeaverIcons.getImageDescriptor(UIIcon.CURSOR)) {
            @Override
            public void run() {
                control.redrawCaret(true);
            }
        });
    }

}
