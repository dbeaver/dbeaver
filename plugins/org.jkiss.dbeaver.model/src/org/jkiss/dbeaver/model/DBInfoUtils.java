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
package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * DB value formatting utilities
 */
public final class DBInfoUtils {

    public static String makeObjectDescription(@NotNull DBRProgressMonitor monitor, DBPNamedObject object, boolean html) {
        StringBuilder info = new StringBuilder();

        DBPNamedObject targetObject = object;
        if (object instanceof DBSObjectReference) {
            try {
                targetObject = ((DBSObjectReference) object).resolveObject(monitor);
            } catch (DBException e) {
                StringWriter buf = new StringWriter();
                e.printStackTrace(new PrintWriter(buf, true));
                info.append(buf.toString());
            }
        }
        PropertyCollector collector = new PropertyCollector(targetObject, false);
        collector.collectProperties();

        for (DBPPropertyDescriptor descriptor : collector.getPropertyDescriptors2()) {
            Object propValue = collector.getPropertyValue(null, descriptor.getId());
            if (propValue == null) {
                continue;
            }
            String propString;
            if (propValue instanceof DBPNamedObject) {
                propString = ((DBPNamedObject) propValue).getName();
            } else {
                propString = DBValueFormatting.getDefaultValueDisplayString(propValue, DBDDisplayFormat.UI);
            }
            if (CommonUtils.isEmpty(propString)) {
                continue;
            }
            if (html) {
                info.append("<b>").append(descriptor.getDisplayName()).append(":  </b>");
                info.append(propString);
                info.append("<br>");
            } else {
                info.append(descriptor.getDisplayName()).append(": ").append(propString).append("\n");
            }
        }
        return info.toString();
    }

}
