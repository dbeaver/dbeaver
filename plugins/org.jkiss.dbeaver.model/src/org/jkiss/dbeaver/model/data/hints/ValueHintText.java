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
package org.jkiss.dbeaver.model.data.hints;

import org.jkiss.dbeaver.model.DBPImage;

/**
 * ValueHintText
 */
public class ValueHintText implements DBDValueHint {

    private final String text;
    private final String description;
    private final DBPImage icon;

    public ValueHintText(String text, String description, DBPImage icon) {
        this.text = text;
        this.description = description;
        this.icon = icon;
    }

    @Override
    public HintType getHintType() {
        return HintType.STRING;
    }

    @Override
    public String getHintText() {
        return text;
    }

    @Override
    public String getHintDescription() {
        return description;
    }

    @Override
    public DBPImage getHintIcon() {
        return icon;
    }
}