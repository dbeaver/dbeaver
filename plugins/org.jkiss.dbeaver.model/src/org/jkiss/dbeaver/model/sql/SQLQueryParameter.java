/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * SQL statement parameter info
 */
public class SQLQueryParameter {
    private int ordinalPosition;
    private String name;
    private String value;
    private boolean variableSet;
    private final int tokenOffset;
    private final int tokenLength;
    private SQLQueryParameter previous;

    public SQLQueryParameter(int ordinalPosition, String name)
    {
        this(ordinalPosition, name, 0, 0);
    }

    public SQLQueryParameter(int ordinalPosition, String name, int tokenOffset, int tokenLength)
    {
        if (tokenOffset < 0) {
            throw new IndexOutOfBoundsException("Bad parameter offset: " + tokenOffset);
        }
        if (tokenLength < 0) {
            throw new IndexOutOfBoundsException("Bad parameter length: " + tokenLength);
        }
        this.ordinalPosition = ordinalPosition;
        this.name = name.trim();
        this.tokenOffset = tokenOffset;
        this.tokenLength = tokenLength;
    }

    public boolean isNamed() {
        return !"?".equals(name);
    }

    public int getTokenOffset() {
        return tokenOffset;
    }

    public int getTokenLength() {
        return tokenLength;
    }

    public SQLQueryParameter getPrevious() {
        return previous;
    }

    public void setPrevious(SQLQueryParameter previous) {
        this.previous = previous;
    }

    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    public String getName()
    {
        return name;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public boolean isVariableSet() {
        return variableSet;
    }

    public void setVariableSet(boolean variableSet) {
        this.variableSet = variableSet;
    }

    public String getTitle() {
        if (GeneralUtils.isVariablePattern(name)) {
            return GeneralUtils.stripVariablePattern(name);
        } else if (name.startsWith(":")) {
            return name.substring(1);
        } else {
            return name;
        }
    }

    @Override
    public String toString() {
        return getTitle() + "=" + value;
    }
}
