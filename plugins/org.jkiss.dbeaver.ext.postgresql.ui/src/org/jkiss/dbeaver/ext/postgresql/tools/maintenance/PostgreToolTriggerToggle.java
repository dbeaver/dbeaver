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
package org.jkiss.dbeaver.ext.postgresql.tools.maintenance;

import java.util.Collection;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreObject;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTrigger;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.utils.CommonUtils;

public abstract class PostgreToolTriggerToggle implements IExternalTool {

    private boolean isEnable;

    protected PostgreToolTriggerToggle(boolean enable) {
	this.isEnable = enable;
    }

    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects)
	    throws DBException {
	List<PostgreTrigger> trigger = CommonUtils.filterCollection(objects, PostgreTrigger.class);
	if (!trigger.isEmpty()) {
	    SQLDialog dialog = new SQLDialog(activePart.getSite(), trigger);
	    dialog.open();
	}
    }

    class SQLDialog extends TableToolDialog {

	public SQLDialog(IWorkbenchPartSite partSite, List<PostgreTrigger> selectedTrigger) {
	    super(partSite, (isEnable ? "Enable" : "Disable") + " trigger", selectedTrigger);
	}

	@Override
	protected void generateObjectCommand(List<String> lines, PostgreObject object) {
	    lines.add("ALTER TABLE " + ((PostgreTrigger) object).getTable() + " " + (isEnable ? "ENABLE" : "DISABLE")
		    + " TRIGGER " + DBUtils.getQuotedIdentifier((PostgreTrigger) object));
	}

	@Override
	protected void createControls(Composite parent) {
	    createObjectsSelector(parent);
	}
    }

}
