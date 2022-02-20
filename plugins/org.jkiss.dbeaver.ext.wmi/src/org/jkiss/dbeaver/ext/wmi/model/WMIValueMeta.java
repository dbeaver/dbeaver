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
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.model.data.DBDValueMeta;
import org.jkiss.wmi.service.WMIQualifier;

import java.util.Collection;

/**
 * Value meta information
 */
public class WMIValueMeta implements DBDValueMeta {

    private Collection<WMIQualifier> qualifiers;

    public WMIValueMeta(Collection<WMIQualifier> qualifiers)
    {
        this.qualifiers = qualifiers;
    }

    public Collection<WMIQualifier> getQualifiers()
    {
        return qualifiers;
    }
}
