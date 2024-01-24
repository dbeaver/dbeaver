/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.runtime.DBWorkbench;

/**
 * ConfigurationFileSelector
 */
public class ConfigurationFileSelector extends TextWithOpenFile {

    public ConfigurationFileSelector(Composite parent, String title, String[] filterExt) {
        super(parent, title, filterExt);
    }

    public ConfigurationFileSelector(Composite parent, String title, String[] filterExt, boolean binaryFile) {
        super(parent, title, filterExt, binaryFile);
    }

    public ConfigurationFileSelector(Composite parent, String title, String[] filterExt, boolean binaryFile, boolean secured) {
        super(parent, title, filterExt, binaryFile, secured);
    }

    @Override
    protected boolean isShowFileContentEditor() {
        return DBWorkbench.isDistributed();
    }

}