/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorInput;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Read-only HTML content editor part
 */
public class HtmlEditorPart extends EditorPart implements IRefreshablePart {

    private static final Log log = Log.getLog(HtmlEditorPart.class);

    private final IValueController valueController;
    private Browser browser;

    public HtmlEditorPart(@NotNull IValueController valueController) {
        this.valueController = valueController;
    }

    @Override
    public void init(@NotNull IEditorSite site, @NotNull IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
    }

    @Override
    public void createPartControl(@NotNull Composite parent) {
        browser = HtmlPanelEditor.createBrowser(parent);
        loadContent();
    }

    private void loadContent() {
        if (browser == null || browser.isDisposed() || !(getEditorInput() instanceof ContentEditorInput input)) {
            return;
        }
        IStorage storage = input.getAdapter(IStorage.class);
        if (storage == null) {
            browser.setText(""); //$NON-NLS-1$
            return;
        }
        DBPPreferenceStore store = valueController.getExecutionContext() != null
            ? valueController.getExecutionContext().getDataSource().getContainer().getPreferenceStore()
            : DBWorkbench.getPlatform().getPreferenceStore();
        long configuredLimit = Math.max(store.getInt(ResultSetPreferences.RS_EDIT_MAX_TEXT_SIZE), 0) * 1000L;
        int maxContentLength = (int) Math.min(configuredLimit, Integer.MAX_VALUE);
        try (Reader reader = new InputStreamReader(storage.getContents(), input.getEncoding())) {
            browser.setText(HtmlPanelEditor.prepareContent(HtmlPanelEditor.readContent(reader, maxContentLength)));
        } catch (CoreException | IOException e) {
            log.error("Error reading HTML content", e); //$NON-NLS-1$
            browser.setText(""); //$NON-NLS-1$
        }
    }

    @Override
    public void setFocus() {
        if (browser != null && !browser.isDisposed()) {
            browser.setFocus();
        }
    }

    @NotNull
    @Override
    public String getTitle() {
        return "HTML"; //$NON-NLS-1$
    }

    @NotNull
    @Override
    public Image getTitleImage() {
        return DBeaverIcons.getImage(DBIcon.TYPE_TEXT);
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void doSave(@NotNull IProgressMonitor monitor) {
    }

    @Override
    public void doSaveAs() {
    }

    @NotNull
    @Override
    public RefreshResult refreshPart(@Nullable Object source, boolean force) {
        UIUtils.asyncExec(this::loadContent);
        return RefreshResult.REFRESHED;
    }
}
