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

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Data formatter profile
 */
public interface DBDDataFormatterProfile {

    DBPPreferenceStore getPreferenceStore();

    String getProfileName();
    
    void setProfileName(String name);

    Locale getLocale();

    void setLocale(Locale locale);

    Map<Object, Object> getFormatterProperties(String typeId);

    void setFormatterProperties(String typeId, Map<Object, Object> properties);

    boolean isOverridesParent();

    void reset();

    void saveProfile() throws IOException;

    DBDDataFormatter createFormatter(String typeId) throws IllegalAccessException, InstantiationException, IllegalArgumentException;

}
