/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.ui.tools.maintenance;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.ext.db2.ui.internal.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.model.DBPEvaluationContext;

import java.util.Collection;
import java.util.List;

/**
 * DB2 Table Reorg Check dialog
 */
public class DB2ReorgCheckTableDialog extends DB2BaseTableToolDialog {

    private static final int NB_RESULT_COLS = 12;

    public DB2ReorgCheckTableDialog(IWorkbenchPartSite partSite, final Collection<DB2Table> selectedTables)
    {
        super(partSite, DB2Messages.dialog_table_tools_reorgcheck_title, selectedTables);
    }

    @Override
    protected int getNumberExtraResultingColumns()
    {
        return NB_RESULT_COLS;
    }

    @Override
    protected void createControls(Composite parent)
    {
        // Object Selector
        createObjectsSelector(parent);
    }

    @Override
    protected void generateObjectCommand(List<String> lines, DB2Table db2Table)
    {
        StringBuilder sb = new StringBuilder(128);
        sb.append("CALL SYSPROC.REORGCHK_TB_STATS('T','");
        sb.append(db2Table.getFullyQualifiedName(DBPEvaluationContext.DDL));
        sb.append("')");

        lines.add(sb.toString());
    }
}
