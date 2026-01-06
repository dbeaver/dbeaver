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

package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Data formatter profile
 */
public interface DBDDataFormatterProfile {

    @NotNull
    DBPPreferenceStore getPreferenceStore();

    @NotNull
    String getProfileName();
    
    void setProfileName(@NotNull String name);

    Locale getLocale();

    void setLocale(@NotNull Locale locale);

    @NotNull
    Map<String, Object> getFormatterProperties(@NotNull DBPPreferenceStore store, @NotNull String typeId);

    void setFormatterProperties(
        @NotNull DBPPreferenceStore store,
        @NotNull String typeId,
        @NotNull Map<String, Object> properties);

    boolean isOverridesParent();

    void reset(@NotNull DBPPreferenceStore store);

    void saveProfile(@NotNull DBPPreferenceStore store) throws IOException;

    @NotNull
    DBDDataFormatter createFormatter(@NotNull String typeId, DBSTypedObject type) throws ReflectiveOperationException;

}
