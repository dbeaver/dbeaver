/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.denodo.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.model.*;

public class DenodoDataType extends GenericDataType {

    @NotNull
    private final DenodoDataSource dataSource;
    @NotNull
    private final Kind typeKind;
    @NotNull
    private final DBPDataKind dataKind;


    public enum Kind {
        BUILTIN(null),
        REGISTER(DBPDataKind.STRUCT),
        ARRAY(DBPDataKind.ARRAY);

        @Nullable
        public final DBPDataKind dataKind;

        Kind(@Nullable DBPDataKind dataKind) {
            this.dataKind = dataKind;
        }

        @NotNull
        public static Kind fromVdpTypeString(@NotNull String value) {
            return switch (value.toLowerCase()) {
                case "register" -> Kind.REGISTER;
                case "array" -> Kind.ARRAY;
                default ->
                    // according to the docs, should be unreachable
                    // https://community.denodo.com/docs/html/browse/8.0/en/vdp/vql/stored_procedures/predefined_stored_procedures/get_user_defined_types
                    throw new IllegalStateException("Unexpected data kind " + value);
            };
        }
    }


    protected DenodoDataType(
        @NotNull DenodoDataSource owner,
        int valueType,
        @NotNull String name,
        @NotNull Kind kind
    ) {
        super(owner, valueType, name, null, false, false, 0, 0, 0);
        this.dataSource = owner;
        this.typeKind = kind;
        assert this.typeKind.dataKind != null;
        this.dataKind = this.typeKind.dataKind;
    }

    public DenodoDataType(
        @NotNull DenodoDataSource owner,
        int valueType,
        @NotNull String name,
        boolean searchable,
        int precision,
        int scale
    ) {
        super(owner, valueType, name, null, false, searchable, precision, 0, scale);
        this.dataSource = owner;
        this.typeKind = Kind.BUILTIN;
        this.dataKind = GenericDataSource.getDataKind(getName(), getTypeID());
    }
    
    @NotNull
    @Override
    public DenodoDataSource getDataSource() {
        return this.dataSource;
    }

    @NotNull
    @Override
    public DBPDataKind getDataKind() {
        return this.dataKind;
    }
}