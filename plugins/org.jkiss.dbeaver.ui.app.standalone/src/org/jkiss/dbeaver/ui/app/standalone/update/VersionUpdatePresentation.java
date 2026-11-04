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
package org.jkiss.dbeaver.ui.app.standalone.update;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor.PatchSeverity;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared patch presentation for the update dialog and toolbar contribution.
 */
final class VersionUpdatePresentation {
    private VersionUpdatePresentation() {
    }

    @NotNull
    static DBPImage getSeverityIcon(@NotNull PatchSeverity severity) {
        return switch (severity) {
            case NONE -> UIIcon.DOWNLOAD;
            case MEDIUM -> DBIcon.SMALL_INFO;
            case HIGH -> DBIcon.SMALL_WARNING;
            case CRITICAL -> DBIcon.SMALL_ERROR;
        };
    }

    @NotNull
    static String getSeverityLabel(@NotNull PatchSeverity severity) {
        return switch (severity) {
            case NONE -> "";
            case MEDIUM -> CoreMessages.dialog_version_update_severity_medium;
            case HIGH -> CoreMessages.dialog_version_update_severity_high;
            case CRITICAL -> CoreMessages.dialog_version_update_severity_critical;
        };
    }

    @NotNull
    static String getReleaseNotes(@NotNull VersionDescriptor descriptor, @NotNull Version currentVersion, int currentPatch) {
        List<String> sections = new ArrayList<>();
        var patches = descriptor.getApplicablePatches(currentVersion, currentPatch);
        // An installed base release needs only pending patch notes, not the original release notes again.
        if (patches.isEmpty()) {
            sections.add(formatNotes(CommonUtils.notEmpty(descriptor.getReleaseNotes())));
        } else if (CommonUtils.isNotEmpty(descriptor.getReleaseNotes()) && !descriptor.getPlainVersion().equals(
            currentVersion.getMajor() + "." + currentVersion.getMinor() + "." + currentVersion.getMicro()
        )) {
            sections.add(formatNotes(descriptor.getReleaseNotes()));
        }
        for (var patch : patches) {
            String heading = NLS.bind(CoreMessages.dialog_version_update_patch_notes, patch.number(), patch.date());
            if (patch.severity() != PatchSeverity.NONE) {
                heading += " - " + getSeverityLabel(patch.severity());
            }
            sections.add(heading + "\n" + formatNotes(patch.releaseNotes()));
        }
        return String.join("\n\n", sections);
    }

    @NotNull
    private static String formatNotes(@NotNull String notes) {
        // Normalize each section independently; preserve short lines and spacing between changelog items.
        String formatted = notes.replace("\r", "").stripTrailing().stripIndent().strip();
        return formatted.isEmpty() ? CoreMessages.dialog_version_update_no_notes : formatted;
    }
}
