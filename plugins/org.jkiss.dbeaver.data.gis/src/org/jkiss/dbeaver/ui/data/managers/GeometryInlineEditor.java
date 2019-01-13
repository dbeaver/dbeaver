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
package org.jkiss.dbeaver.ui.data.managers;

import com.vividsolutions.jts.geom.Geometry;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.gis.GisAttribute;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.editors.StringInlineEditor;

/**
 * GisAttribute.
 * Edits value as string. Also manager SRID.
*/
public class GeometryInlineEditor extends StringInlineEditor {

    private int valueSRID;

    public GeometryInlineEditor(IValueController controller) {
        super(controller);
    }

    @Override
    public void primeEditorValue(Object value) throws DBException {
        super.primeEditorValue(value);
        if (value instanceof Geometry) {
            this.valueSRID = ((Geometry) value).getSRID();
        }
        if (valueSRID == 0) {
            DBSTypedObject column = valueController.getValueType();
            if (column instanceof GisAttribute) {
                valueSRID = ((GisAttribute) column).getAttributeSRID(new VoidProgressMonitor());
            }
        }
    }

    @Override
    public Object extractEditorValue() throws DBCException {
        Object geometry = super.extractEditorValue();
        if (geometry instanceof Geometry) {
            if (valueSRID != 0) {
                ((Geometry) geometry).setSRID(valueSRID);
            }
        }
        return geometry;
    }
}
