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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.dialogs.SelectDataSourceDialog;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class SelectDataSourceCombo extends CSmartSelector<DBPDataSourceContainer> {

    private static final Log log = Log.getLog(SelectDataSourceCombo.class);
    private final List<DBRRunnableParametrized<DBPDataSourceContainer>> listeners = new ArrayList<>();

    public SelectDataSourceCombo(Composite comboGroup) {
        super(comboGroup, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER, new DatabaseLabelProviders.ConnectionLabelProvider());
    }

    @Override
    protected void dropDown(boolean drop) {
        if (!drop) {
            return;
        }
        showConnectionSelector();
    }

    public void showConnectionSelector() {
        SelectDataSourceDialog dialog = new SelectDataSourceDialog(getShell(), getActiveProject(), getSelectedItem());
        dialog.setModeless(true);
        if (dialog.open() == IDialogConstants.CANCEL_ID) {
            return;
        }
        DBPDataSourceContainer dataSource = dialog.getDataSource();
        this.select(dataSource);
        onDataSourceChange(dataSource);
    }

    protected IProject getActiveProject() {
        return DBWorkbench.getPlatform().getProjectManager().getActiveProject();
    }

    protected void onDataSourceChange(DBPDataSourceContainer dataSource) {
        List<DBRRunnableParametrized<DBPDataSourceContainer>> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList<>(listeners);
        }
        for (DBRRunnableParametrized<DBPDataSourceContainer> listener : listenersCopy) {
            try {
                listener.run(dataSource);
            } catch (InvocationTargetException e) {
                log.error(e.getTargetException());
            } catch (InterruptedException e) {
                // ignore
                break;
            }
        }
    }

    public void addSelectionListener(DBRRunnableParametrized<DBPDataSourceContainer> listener) {
        listeners.add(listener);
    }

    public void addProjectDataSources() {
        addItem(null);
        if (getActiveProject() == null) {
            return;
        }
        DBPDataSourceRegistry registry = DBWorkbench.getPlatform().getProjectManager().getDataSourceRegistry(getActiveProject());
        for (DBPDataSourceContainer ds : registry.getDataSources()) {
            addItem(ds);
        }
    }

}
