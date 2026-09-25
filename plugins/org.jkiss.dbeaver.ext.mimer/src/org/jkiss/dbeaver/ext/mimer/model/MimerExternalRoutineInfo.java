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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.Nullable;

/**
 * {@code INFORMATION_SCHEMA.ROUTINES.ROUTINE_BODY}/{@code EXTERNAL_LANGUAGE}/{@code
 * EXTERNAL_NAME}/{@code EXTERNAL_LIBRARY} for one procedure/function, read by {@link
 * MimerUtils#readExternalRoutineInfo} - shared by {@link MimerMetaModel#getProcedureDDL} (to
 * reconstruct the {@code EXTERNAL NAME ... IN library} source, see {@link
 * MimerUtils#buildExternalRoutineSource}) and {@link MimerProcedure}'s own Language/External
 * Name/Library properties, so the same query only ever needs to be issued once per routine.
 *
 * @author Mimer Information Technology
 */
public record MimerExternalRoutineInfo(boolean external, @Nullable String language, @Nullable String externalName, @Nullable String library) {

    public static final MimerExternalRoutineInfo NOT_EXTERNAL = new MimerExternalRoutineInfo(false, null, null, null);
}
