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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPImageProvider;

public class AltibaseDataType extends GenericDataType implements DBPImageProvider {

    public AltibaseDataType(GenericStructContainer owner, AltibaseDataTypeDomain dataTypeDomin) {
        super(owner, dataTypeDomin.getValueType(), dataTypeDomin.getTypeName(), null, false, true, 0, 0, 0);
    }

    public AltibaseDataType(GenericStructContainer owner, AltibaseDataTypeDomain fieldType,
            String name, String remarks, boolean unsigned, boolean searchable,
            int precision, int minScale, int maxScale) {
        super(owner, fieldType.getValueType(), name, remarks, unsigned, searchable, precision, 
                minScale, maxScale);
    }

    @Override
    public DBPDataKind getDataKind() {
        return switch (getName().toUpperCase()) {
            case AltibaseConstants.TYPE_NAME_JSON -> DBPDataKind.CONTENT;
            case AltibaseConstants.TYPE_NAME_NUMBER -> DBPDataKind.NUMERIC;
            default -> super.getDataKind();
        };
    }

    public DBPImage getObjectImage() {
        return switch (getName().toUpperCase()) {
            case AltibaseConstants.TYPE_NAME_JSON -> DBIcon.TYPE_JSON;
            default -> null;
        };
    }
}
