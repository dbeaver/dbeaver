/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol.ui.tools;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableBase;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

import java.util.HashSet;
import java.util.List;

public class ExasolImportTableToolCommandHandler extends AbstractHandler {

    private static final Log log = Log.getLog(ExasolImportTableToolCommandHandler.class);

    @Override
    public Object execute(ExecutionEvent event) {
        List<DBSObject> selectedObjects = NavigatorUtils.getSelectedObjects(HandlerUtil.getCurrentSelection(event));
        List<ExasolTable> tables = CommonUtils.filterCollection(selectedObjects, ExasolTable.class);
        List<ExasolSchema> schemas = CommonUtils.filterCollection(selectedObjects, ExasolSchema.class);

        //add tables for all Schemas but ignore views in schema
        for (ExasolSchema schema : schemas) {
            try {
                tables.addAll(schema.getTables(new VoidProgressMonitor()));
            } catch (DBException e) {
                log.error(e);
            }
        }

        // create TableBase Objects list
        @SuppressWarnings({"unchecked", "rawtypes"})
        HashSet<ExasolTableBase> tableBaseObjects = new HashSet();

        //add tables
        for (ExasolTable table : tables) {
            tableBaseObjects.add((ExasolTableBase) table);
        }


        if (!tableBaseObjects.isEmpty()) {
            ExasolImportTableToolDialog dialog = new ExasolImportTableToolDialog(
                HandlerUtil.getActivePart(event).getSite(),
                tableBaseObjects
            );
            return dialog.open();
        }
        return null;
    }
}
