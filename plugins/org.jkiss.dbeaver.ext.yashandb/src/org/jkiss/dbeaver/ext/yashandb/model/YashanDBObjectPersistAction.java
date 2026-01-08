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

import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;

public class YashanDBObjectPersistAction extends SQLDatabasePersistAction {

	private final YashanDBObjectType objectType;

	public YashanDBObjectPersistAction(YashanDBObjectType objectType, String title, String script) {
		super(title, script);
		this.objectType = objectType;
	}

	public YashanDBObjectPersistAction(YashanDBObjectType objectType, String script) {
		super(script);
		this.objectType = objectType;
	}

	public YashanDBObjectType getObjectType() {
		return objectType;
	}
}