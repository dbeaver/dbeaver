/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;

import java.util.Iterator;

public class FolderDisconnectHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection treeSelection = (IStructuredSelection) selection;
            @SuppressWarnings("rawtypes")
            Iterator it = treeSelection.iterator();
            while (it.hasNext()) {
                Object el = it.next();
                if (el instanceof DBNLocalFolder) {
                    DBNLocalFolder localFolder = (DBNLocalFolder) el;
                    localFolder.getNestedDataSources().forEach(ds -> {
                        final DBPDataSourceContainer dataSourceContainer = ds.getObject();
                        if (ds != null && ds.getObject().isConnected()) {
                            DataSourceHandler.disconnectDataSource(dataSourceContainer, null);
                        }
                    });
                }
            }
        }
        return null;
    }
}
