/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.ui.editors;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.IContributionManager;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTable;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;
import org.jkiss.utils.CommonUtils;

public class YashanDBObjectDDLEditor extends SQLSourceViewer<YashanDBTable> implements YashanDBDDLOptions {

	private Map<String, Object> ddlOptions = new HashMap<>();

	@Override
	protected void contributeEditorCommands(IContributionManager contributionManager) {
		super.contributeEditorCommands(contributionManager);
		if (getSourceObject() instanceof YashanDBTable) {
			YashanDBEditorUtils.addDDLControl(contributionManager, getSourceObject(), this);
		}
	}

	@Override
	public void putDDLOptions(String name, Object value) {
		ddlOptions.put(name, value);
	}

	@Override
	protected Map<String, Object> getSourceOptions() {
		Map<String, Object> options = super.getSourceOptions();
		if (!CommonUtils.isEmpty(ddlOptions)) {
			options.putAll(ddlOptions);
		}
		return options;
	}
}
