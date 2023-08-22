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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.runtime.DBWorkbench;

/**
 * ConfigurationFileSelector
 */
public class ConfigurationFileSelector extends TextWithOpenFile {

    private boolean isSensitiveData;
    private String sensitiveText;
    
    public ConfigurationFileSelector(Composite parent, String title, String[] filterExt) {
        super(parent, title, filterExt);
        this.isSensitiveData = false;
    }

    public ConfigurationFileSelector(Composite parent, String title, String[] filterExt, boolean binaryFile) {
        super(parent, title, filterExt, binaryFile);
        this.isSensitiveData = false;
    }

    public boolean isSensitiveData() {
        return this.isSensitiveData;
    }
    
    public void setSensitiveData(boolean value) {
        this.isSensitiveData = value;
    }
    
    public String getSensitiveData() {
        return this.sensitiveText;
    }
    
    @Override
    protected boolean isShowFileContentEditor() {
        return DBWorkbench.isDistributed();
    }

    @Override
    public String getText() {
        if (this.isSensitiveData) {
            return "******";
        } else {
            return super.getText();
        }
    }
    
    @Override
    public void setText(String str) {
        if (this.isSensitiveData) {
            this.sensitiveText = str;
            super.setText("******");
        } else {
            super.setText(str);
        }
    }
}