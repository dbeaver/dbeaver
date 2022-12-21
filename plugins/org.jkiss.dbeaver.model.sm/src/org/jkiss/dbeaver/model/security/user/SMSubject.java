/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.security.user;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPNamedObject;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SMSubject implements DBPNamedObject {

    protected final String subjectId;
    private final Map<String, String> metaParameters = new LinkedHashMap<>();

    public SMSubject(
        @NotNull String subjectId,
        @Nullable Map<String, String> metaParameters
    ) {
        this.subjectId = subjectId;
        if (metaParameters != null) {
            this.metaParameters.putAll(metaParameters);
        }
    }

    public String getSubjectId() {
        return subjectId;
    }

    @NotNull
    public Map<String, String> getMetaParameters() {
        return metaParameters;
    }

    public void setMetaParameter(String name, String value) {
        metaParameters.put(name, value);
    }

    @NotNull
    public void setMetaParameters(Map<String, String>  parameters) {
        metaParameters.clear();
        metaParameters.putAll(parameters);
    }

}

