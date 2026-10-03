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
package org.jkiss.dbeaver.ext.mimer.ui.config;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerGroup;
import org.jkiss.dbeaver.ext.mimer.model.MimerProgram;
import org.jkiss.dbeaver.ext.mimer.model.MimerUser;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

import java.util.stream.Stream;

/**
 * Shared grantee-picker helper for every "Create ... Privilege"/"Add Privileges" dialog in this
 * plugin - every plain Users+Groups+Programs grantee combo populates itself identically, which
 * used to be copy-pasted verbatim across ~10 call sites.
 *
 * @author Mimer Information Technology
 */
public final class MimerIdentPickerUtils {

    private static final Log log = Log.getLog(MimerIdentPickerUtils.class);

    private MimerIdentPickerUtils() {
    }

    /**
     * Every User/Group/Program name on the datasource, sorted - the grantee-combo contents shared
     * by every plain grant/revoke create dialog. {@link MimerCreateGroupMemberPage} does NOT use
     * this - it additionally excludes the group being edited from the Groups list (a group can't
     * be its own member), so it keeps its own, slightly different version.
     */
    @NotNull
    public static String[] loadIdentNames(@NotNull MimerDataSource dataSource) {
        try {
            VoidProgressMonitor monitor = new VoidProgressMonitor();
            return Stream.of(
                    dataSource.getUsers(monitor).stream().map(MimerUser::getName),
                    dataSource.getGroups(monitor).stream().map(MimerGroup::getName),
                    dataSource.getPrograms(monitor).stream().map(MimerProgram::getName))
                .flatMap(s -> s)
                .sorted()
                .toArray(String[]::new);
        } catch (Exception e) {
            log.debug("Can't load ident list for a grantee picker", e);
            return new String[0];
        }
    }
}
