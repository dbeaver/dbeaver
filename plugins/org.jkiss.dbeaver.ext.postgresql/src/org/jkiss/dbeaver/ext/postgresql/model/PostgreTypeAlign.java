/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * PostgreTypeAlign
 */
public enum PostgreTypeAlign implements DBPNamedObject
{
    c("char", 1),
    s("short", 2),
    i("int", 4),
    d("double", 8);

    private final String desc;
    private final int bytes;

    PostgreTypeAlign(String desc, int bytes) {
        this.desc = desc;
        this.bytes = bytes;
    }

    @Override
    public String getName() {
        return desc;
    }

    public int getBytes() {
        return bytes;
    }
}
