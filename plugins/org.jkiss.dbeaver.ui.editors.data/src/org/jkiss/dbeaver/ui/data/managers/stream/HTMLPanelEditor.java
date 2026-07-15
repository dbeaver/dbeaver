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

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.data.IStreamValueEditor;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.editors.EditorMessages;

import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Read-only HTML preview
 */
public class HTMLPanelEditor implements IStreamValueEditor<Composite> {

    private static final Log log = Log.getLog(HTMLPanelEditor.class);

    private final IValueController valueController;
    private final AtomicLong updateSequence = new AtomicLong();
    private Browser browser;

    public HTMLPanelEditor(@NotNull IValueController valueController) {
        this.valueController = valueController;
    }

    @Override
    public Composite createControl(IValueController valueController) {
        Composite composite = new Composite(valueController.getEditPlaceholder(), SWT.NONE);
        composite.setLayout(new FillLayout());
        browser = createBrowser(composite);
        return composite;
    }

    @Nullable
    static Browser createBrowser(@NotNull Composite parent) {
        try {
            Browser browser = new Browser(parent, SWT.NONE);
            browser.setJavascriptEnabled(false);
            browser.addOpenWindowListener(event -> event.required = true);
            browser.addLocationListener(new LocationAdapter() {
                @Override
                public void changing(@NotNull LocationEvent event) {
                    if (event.top && !"about:blank".equals(event.location)) { //$NON-NLS-1$
                        event.doit = false;
                    }
                }
            });
            return browser;
        } catch (SWTError | SWTException e) {
            log.warn("Can't create HTML preview browser", e); //$NON-NLS-1$
            UIUtils.createInfoLabel(parent, NLS.bind(EditorMessages.html_preview_unavailable, e.getMessage()));
            return null;
        }
    }

    @Override
    public void extractEditorValue(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Composite control,
        @NotNull DBDContent value
    ) {
        // HTML preview is read-only and must never update the underlying content.
    }

    @Override
    public void primeEditorValue(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Composite control,
        @NotNull DBDContent value
    ) throws DBException {
        long updateId = updateSequence.incrementAndGet();
        updateBrowser(updateId, ""); //$NON-NLS-1$
        if (value.isNull()) {
            return;
        }

        DBDContentStorage storage = value.getContents(monitor);
        if (storage == null) {
            return;
        }

        DBPPreferenceStore store = valueController.getExecutionContext() != null
            ? valueController.getExecutionContext().getDataSource().getContainer().getPreferenceStore()
            : DBWorkbench.getPlatform().getPreferenceStore();
        long configuredLimit = Math.max(store.getInt(ResultSetPreferences.RS_EDIT_MAX_TEXT_SIZE), 0) * 1000L;
        int maxContentLength = (int) Math.min(configuredLimit, Integer.MAX_VALUE);
        try (Reader reader = storage.getContentReader()) {
            updateBrowser(updateId, readContent(reader, maxContentLength));
        } catch (IOException e) {
            throw new DBException(EditorMessages.html_preview_read_error, e);
        }
    }

    @NotNull
    static String readContent(@NotNull Reader reader, int maxContentLength) throws IOException {
        StringBuilder content = new StringBuilder(Math.min(maxContentLength, 8192));
        char[] buffer = new char[Math.min(maxContentLength, 8192)];
        while (content.length() < maxContentLength) {
            int count = reader.read(buffer, 0, Math.min(buffer.length, maxContentLength - content.length()));
            if (count <= 0) {
                break;
            }
            content.append(buffer, 0, count);
        }
        return content.toString();
    }

    private void updateBrowser(long updateId, @NotNull String html) {
        UIUtils.syncExec(() -> {
            if (updateId == updateSequence.get() && browser != null && !browser.isDisposed()) {
                browser.setText(html);
            }
        });
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull Composite control) {
    }

    @Override
    public void contributeSettings(@NotNull IContributionManager manager, @NotNull Composite control) {
    }

    @Override
    public void disposeEditor() {
        updateSequence.incrementAndGet();
        browser = null;
    }
}
