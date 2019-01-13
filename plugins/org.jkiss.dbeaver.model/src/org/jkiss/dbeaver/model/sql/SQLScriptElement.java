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

package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;

/**
 * SQL command (query or control)
 */
public interface SQLScriptElement {

    @NotNull
    String getOriginalText();

    @NotNull
    String getText();

    int getOffset();

    int getLength();

    /**
     * User defined data object. May be used to identify statements.
     * @return data or null
     */
    Object getData();

    void setData(Object data);

    void reset();
}
