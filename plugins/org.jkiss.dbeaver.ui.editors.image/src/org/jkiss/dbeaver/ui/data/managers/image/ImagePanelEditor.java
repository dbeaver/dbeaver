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
package org.jkiss.dbeaver.ui.data.managers.image;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.controls.imageview.AbstractImageViewer;
import org.jkiss.dbeaver.ui.controls.imageview.BrowserImageViewer;
import org.jkiss.dbeaver.ui.controls.imageview.SWTImageViewer;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.data.IStreamValueEditorPersistent;
import org.jkiss.dbeaver.ui.data.IValueController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
* ImagePanelEditor
*/
public class ImagePanelEditor implements IStreamValueEditorPersistent<AbstractImageViewer> {

    @Override
    public AbstractImageViewer createControl(IValueController valueController) {
        DBPPreferenceStore preferenceStore = valueController.getExecutionContext()
            .getDataSource()
            .getContainer()
            .getPreferenceStore();
        if (preferenceStore.getBoolean(ResultSetPreferences.RESULT_IMAGE_USE_BROWSER_BASED_RENDERER)) {
            return new BrowserImageViewer(valueController.getEditPlaceholder(), SWT.NONE);
        } else {
            return new SWTImageViewer(valueController.getEditPlaceholder(), SWT.NONE);
        }
    }

    @Override
    public void primeEditorValue(
        @NotNull DBRProgressMonitor monitor,
        @Nullable AbstractImageViewer control,
        @NotNull DBDContent value
    ) throws DBException {
        monitor.subTask("Read image value");
        DBDContentStorage data = value.getContents(monitor);
        if (data != null) {
            try (InputStream contentStream = data.getContentStream()) {
                if (!(new UITask<Boolean>() {
                    @Override
                    protected Boolean runTask() {
                        if (control != null && !control.isDisposed()) {
                            return control.loadImage(contentStream);
                        } else {
                            return true; // already read
                        }
                    }
                }).runTask());

            } catch (IOException e) {
                throw new DBException("Error reading stream value", e);
            }
        } else {
            control.clearImage();
        }
    }

    @Override
    public void extractEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull AbstractImageViewer control, @NotNull DBDContent value) throws DBException {
        // Not implemented
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull final AbstractImageViewer control) throws DBCException {
        control.fillToolBar(manager);
    }

    @Override
    public void contributeSettings(@NotNull IContributionManager manager, @NotNull AbstractImageViewer control) throws DBCException {

    }

    @Override
    public void disposeEditor() {

    }

    @Nullable
    @Override
    public Path getExternalFilePath(@NotNull AbstractImageViewer control) {
        return control.getExternalFilePath();
    }
}
