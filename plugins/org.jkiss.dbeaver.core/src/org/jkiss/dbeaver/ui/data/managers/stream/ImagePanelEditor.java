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
import org.eclipse.swt.SWT;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.controls.imageview.ImageViewer;
import org.jkiss.dbeaver.ui.data.IStreamValueEditor;
import org.jkiss.dbeaver.ui.data.IValueController;

import java.io.IOException;
import java.io.InputStream;

/**
* ImagePanelEditor
*/
public class ImagePanelEditor implements IStreamValueEditor<ImageViewer> {

    @Override
    public ImageViewer createControl(IValueController valueController)
    {
        return new ImageViewer(valueController.getEditPlaceholder(), SWT.NONE);
    }

    @Override
    public void primeEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull ImageViewer control, @NotNull DBDContent value) throws DBException
    {
        monitor.subTask("Read image value");
        DBDContentStorage data = value.getContents(monitor);
        if (data != null) {
            try (InputStream contentStream = data.getContentStream()) {
                if (!control.loadImage(contentStream)) {
                    throw new DBException("Can't load image: " + control.getLastError().getMessage());
                }
            } catch (IOException e) {
                throw new DBException("Error reading stream value", e);
            }
        } else {
            control.clearImage();
        }
    }

    @Override
    public void extractEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull ImageViewer control, @NotNull DBDContent value) throws DBException
    {
        // Not implemented
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull final ImageViewer control) throws DBCException {
        control.fillToolBar(manager);
    }

}
