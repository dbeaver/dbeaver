/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;

public class FolderPropertyTester extends PropertyTester {

	static protected final Log log = Log.getLog(FolderPropertyTester.class);

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof DBNLocalFolder)) {
        	log.info(String.format("%s cannot be used with %s type", this.getClass().getName(), receiver.getClass().getName()));
            return false;
        }
        DBNLocalFolder localFolder = (DBNLocalFolder) receiver;
        return localFolder.hasConnected();
    }
}
