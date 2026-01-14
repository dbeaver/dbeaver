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
package org.jkiss.dbeaver.model.secret;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Objects;

public class DBSSecretValue {
    @Nullable
    private String subjectId;
    @NotNull
    private String id;
    @NotNull
    private String displayName;
    @Nullable
    private String value;


    public DBSSecretValue(@NotNull String id, @NotNull String displayName, @Nullable String value) {
        this.id = id;
        this.displayName = displayName;
        this.value = value;
    }

    public DBSSecretValue(@NotNull String subjectId, @NotNull String id, @NotNull String displayName, @Nullable String value) {
        this.subjectId = subjectId;
        this.id = id;
        this.displayName = displayName;
        this.value = value;
    }

    // for serialization
    public DBSSecretValue() {
    }

    @Nullable
    public String getSubjectId() {
        return subjectId;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getUniqueId() {
        if (getSubjectId() == null) {
            return getId();
        }
        return getId() + "_" + getSubjectId();
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    public String getValue() {
        return value;
    }

    public void setSubjectId(@NotNull String subjectId) {
        this.subjectId = subjectId;
    }

    public void setId(@NotNull String id) {
        this.id = id;
    }

    public void setDisplayName(@NotNull String displayName) {
        this.displayName = displayName;
    }

    public void setValue(@Nullable String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DBSSecretValue that = (DBSSecretValue) o;
        return Objects.equals(subjectId, that.subjectId)
            && Objects.equals(id, that.id)
            && Objects.equals(displayName, that.displayName)
            && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subjectId, id, displayName, value);
    }
}
