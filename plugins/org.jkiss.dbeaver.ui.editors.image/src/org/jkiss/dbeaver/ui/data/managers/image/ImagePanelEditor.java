/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.data.managers.image;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
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
                if (!(new UITask<Boolean>() {
                    @Override
                    protected Boolean runTask() {
                        return control.loadImage(contentStream);
                    }
                }).execute())
                {
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

    @Override
    public void contributeSettings(@NotNull IContributionManager manager, @NotNull ImageViewer control) throws DBCException {

    }

}
