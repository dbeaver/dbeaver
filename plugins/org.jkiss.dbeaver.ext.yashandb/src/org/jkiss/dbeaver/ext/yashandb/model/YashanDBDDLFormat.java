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
package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

public enum YashanDBDDLFormat {

	FULL("Full DDL", true, true, true), NO_STORAGE("No storage information", false, true, true),
	COMPACT("Compact form", false, false, false);

	private final String title;
	private final boolean showStorage;
	private final boolean showSegments;
	private final boolean showTablespace;

	public static final String PREF_KEY_DDL_FORMAT = "yashandb.ddl.format";

	private static final Log log = org.jkiss.dbeaver.Log.getLog(YashanDBDDLFormat.class);

	private YashanDBDDLFormat(String title, boolean showStorage, boolean showSegments, boolean showTablespace) {
		this.showTablespace = showTablespace;
		this.showSegments = showSegments;
		this.showStorage = showStorage;
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public boolean isShowStorage() {
		return showStorage;
	}

	public boolean isShowSegments() {
		return showSegments;
	}

	public boolean isShowTablespace() {
		return showTablespace;
	}

	public static YashanDBDDLFormat getCurrentFormat(YashanDBDataSource dataSource) {
		String ddlFormatString = dataSource.getContainer().getPreferenceStore()
				.getString(YashanDBConstants.PREF_KEY_DDL_FORMAT);
		if (!CommonUtils.isEmpty(ddlFormatString)) {
			try {
				return YashanDBDDLFormat.valueOf(ddlFormatString);
			} catch (IllegalArgumentException e) {
				log.error(e);
			}
		}
		return YashanDBDDLFormat.FULL;
	}

}
