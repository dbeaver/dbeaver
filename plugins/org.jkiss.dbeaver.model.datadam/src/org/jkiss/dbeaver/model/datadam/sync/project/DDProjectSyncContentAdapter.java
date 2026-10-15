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
package org.jkiss.dbeaver.model.datadam.sync.project;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.sync.*;

import java.util.*;

/**
 * Maps project synchronization units to the flat file collection used by the DataDam project API.
 */
public final class DDProjectSyncContentAdapter {

    private static final char FILE_NAME_SEPARATOR = '/';

    private final List<DBPSyncUnit> units;

    private DDProjectSyncContentAdapter(@NotNull List<DBPSyncUnit> units) {
        this.units = units;
    }

    @NotNull
    public static DDProjectSyncContentAdapter forEnabledUnits() throws DBException {
        List<DBPSyncUnit> units = DBPSyncRegistry.getInstance().getUnits().stream()
            .filter(unit -> unit.getScope() == DBPSyncScope.PROJECT && DBPSyncSettings.isEnabled(unit))
            .toList();
        return new DDProjectSyncContentAdapter(validateUnits(units));
    }

    @NotNull
    public static DDProjectSyncContentAdapter forUnitIds(@NotNull Collection<String> unitIds) throws DBException {
        List<DBPSyncUnit> units = new ArrayList<>(unitIds.size());
        Set<String> uniqueIds = new LinkedHashSet<>();
        for (String unitId : unitIds) {
            if (!uniqueIds.add(unitId)) {
                throw new DBException("Duplicate project synchronization unit: " + unitId);
            }
            DBPSyncUnit unit = DBPSyncRegistry.getInstance().findById(unitId);
            if (unit == null || unit.getScope() != DBPSyncScope.PROJECT) {
                throw new DBException("Unknown project synchronization unit: " + unitId);
            }
            units.add(unit);
        }
        return new DDProjectSyncContentAdapter(validateUnits(units));
    }

    @NotNull
    public static DDProjectSyncContentAdapter forFiles(@NotNull Collection<String> fileNames) throws DBException {
        Set<String> unitIds = new LinkedHashSet<>();
        for (String fileName : fileNames) {
            unitIds.add(decodeFileName(fileName).unitId());
        }
        return unitIds.isEmpty() ? forEnabledUnits() : forUnitIds(unitIds);
    }

    @NotNull
    public Set<String> getUnitIds() {
        Set<String> unitIds = new LinkedHashSet<>();
        for (DBPSyncUnit unit : units) {
            unitIds.add(unit.getId());
        }
        return Set.copyOf(unitIds);
    }

    @NotNull
    public Map<String, byte[]> read(@NotNull DBPProject project) throws DBException {
        DBPSyncTarget target = new DBPSyncTarget(project.getWorkspace(), project);
        Map<String, byte[]> files = new LinkedHashMap<>();
        for (DBPSyncUnit unit : units) {
            for (Map.Entry<String, byte[]> resource : unit.read(target).entrySet()) {
                String fileName = encodeFileName(unit.getId(), resource.getKey());
                if (files.put(fileName, resource.getValue()) != null) {
                    throw new DBException("Duplicate project synchronization file: " + fileName);
                }
            }
        }
        return files;
    }

    public void write(@NotNull DBPProject project, @NotNull Map<String, byte[]> files) throws DBException {
        Map<String, Map<String, byte[]>> resourcesByUnit = new LinkedHashMap<>();
        for (DBPSyncUnit unit : units) {
            resourcesByUnit.put(unit.getId(), new LinkedHashMap<>());
        }
        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            FileReference reference = decodeFileName(file.getKey());
            Map<String, byte[]> resources = resourcesByUnit.get(reference.unitId());
            if (resources == null) {
                throw new DBException("Unknown project synchronization unit: " + reference.unitId());
            }
            if (resources.put(reference.resourceName(), file.getValue()) != null) {
                throw new DBException("Duplicate project synchronization file: " + file.getKey());
            }
        }

        DBPSyncTarget target = new DBPSyncTarget(project.getWorkspace(), project);
        for (DBPSyncUnit unit : units) {
            unit.write(target, resourcesByUnit.get(unit.getId()));
        }
    }

    @NotNull
    private static List<DBPSyncUnit> validateUnits(@NotNull List<DBPSyncUnit> units) throws DBException {
        Set<String> unitIds = new LinkedHashSet<>();
        for (DBPSyncUnit unit : units) {
            String unitId = unit.getId();
            if (unitId.isEmpty() || unitId.indexOf(FILE_NAME_SEPARATOR) >= 0) {
                throw new DBException("Invalid project synchronization unit id: " + unitId);
            }
            if (!unitIds.add(unitId)) {
                throw new DBException("Duplicate project synchronization unit: " + unitId);
            }
        }
        return List.copyOf(units);
    }

    @NotNull
    private static String encodeFileName(@NotNull String unitId, @NotNull String resourceName) throws DBException {
        if (resourceName.indexOf(FILE_NAME_SEPARATOR) >= 0) {
            throw new DBException("Invalid project synchronization resource name: " + resourceName);
        }
        return unitId + FILE_NAME_SEPARATOR + resourceName;
    }

    @NotNull
    private static FileReference decodeFileName(@NotNull String fileName) throws DBException {
        int separator = fileName.indexOf(FILE_NAME_SEPARATOR);
        if (separator <= 0 || fileName.indexOf(FILE_NAME_SEPARATOR, separator + 1) >= 0) {
            throw new DBException("Invalid project synchronization file name: " + fileName);
        }
        return new FileReference(fileName.substring(0, separator), fileName.substring(separator + 1));
    }

    private record FileReference(
        @NotNull String unitId,
        @NotNull String resourceName
    ) {
    }
}
