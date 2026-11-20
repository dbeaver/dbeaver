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

package org.jkiss.dbeaver.registry.updater;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLException;
import org.osgi.framework.Version;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Version descriptor
 */
public class VersionDescriptor {

    private static final Log log = Log.getLog(VersionDescriptor.class);

    public enum PatchSeverity {
        NONE, MEDIUM, HIGH, CRITICAL;

        @NotNull
        private static PatchSeverity parse(@NotNull String text) {
            return switch (text.trim().toLowerCase(Locale.ROOT)) {
                case "medium" -> MEDIUM;
                case "high" -> HIGH;
                case "critical" -> CRITICAL;
                default -> NONE;
            };
        }
    }

    public record VersionPatch(
        int number,
        @NotNull String date,
        @NotNull String releaseNotes
    ) {
    }

    /**
     * Optional update advisory. The fixed-in version contains three numeric components and an optional numeric patch qualifier.
     */
    public record CriticalUpdate(
        @NotNull Date updateDate,
        @NotNull Version fixedIn,
        @NotNull PatchSeverity severity,
        @NotNull String description
    ) {
        public CriticalUpdate {
            updateDate = new Date(updateDate.getTime());
            Objects.requireNonNull(fixedIn);
            Objects.requireNonNull(severity);
            Objects.requireNonNull(description);
        }

        @NotNull
        @Override
        public Date updateDate() {
            return new Date(updateDate.getTime());
        }
    }

    private String programName;
    private Version programVersion;
    private String updateTime;
    private String baseURL;
    private String downloadURL;
    private String releaseNotes;
    private List<VersionPatch> patches = List.of();
    private List<CriticalUpdate> criticalUpdates = List.of();

    public VersionDescriptor(@NotNull DBPPlatform platform, @NotNull String fileAddr) throws IOException {
        try (InputStream inputStream = WebUtils.openConnection(fileAddr, platform.getWorkspace().getWorkspaceId()).getInputStream()) {
            parseVersionInfo(inputStream);
        }
    }

    public VersionDescriptor(@NotNull InputStream inputStream) throws IOException {
        parseVersionInfo(inputStream);
    }

    @Nullable
    public String getProgramName() {
        return programName;
    }

    @NotNull
    public Version getProgramVersion() {
        return programVersion;
    }

    public void setProgramVersion(@NotNull Version programVersion) {
        this.programVersion = programVersion;
    }

    @NotNull
    public String getPlainVersion() {
        return programVersion.getMajor() + "." + programVersion.getMinor() + "." + programVersion.getMicro();
    }

    public int getPatchNumber() {
        return patches.isEmpty() ? 0 : patches.getLast().number();
    }

    @NotNull
    public String getUpdateVersion() {
        int patchNumber = getPatchNumber();
        return getPlainVersion() + (patchNumber > 0 ? "." + patchNumber : "");
    }

    public boolean isPatchUpdate(@NotNull Version installedVersion, int installedPatch) {
        return hasSameBaseVersion(installedVersion) && getPatchNumber() > installedPatch;
    }

    public boolean isMajorUpdate() {
        return programVersion.getMicro() == 0;
    }

    public boolean isNewerThan(@NotNull Version installedVersion, int installedPatch) {
        int patchNumber = getPatchNumber();
        // Build qualifiers are not patch numbers. Preserve OSGi ordering for ordinary releases.
        if (hasSameBaseVersion(installedVersion) && (patchNumber > 0 || installedPatch > 0)) {
            return patchNumber > installedPatch;
        }
        return programVersion.compareTo(installedVersion) > 0;
    }

    @NotNull
    public List<VersionPatch> getApplicablePatches(@NotNull Version installedVersion, int installedPatch) {
        if (hasSameBaseVersion(installedVersion)) {
            return patches.stream().filter(patch -> patch.number() > installedPatch).toList();
        }
        return programVersion.compareTo(installedVersion) > 0 ? patches : List.of();
    }

    /**
     * All valid advisories in descriptor order, regardless of severity or applicability.
     */
    @NotNull
    public List<CriticalUpdate> getCriticalUpdates() {
        return criticalUpdates;
    }

    @NotNull
    public List<CriticalUpdate> getApplicableCriticalUpdates(@NotNull Version installedVersion, int installedPatch) {
        // OSGi qualifiers identify builds, while fixed-in's optional fourth numeric component identifies a patch.
        Version installedBase = new Version(installedVersion.getMajor(), installedVersion.getMinor(), installedVersion.getMicro());
        return criticalUpdates.stream().filter(update -> {
            Version fixedIn = update.fixedIn();
            Version fixedBase = new Version(fixedIn.getMajor(), fixedIn.getMinor(), fixedIn.getMicro());
            int comparison = installedBase.compareTo(fixedBase);
            int fixedPatch = fixedIn.getQualifier().isEmpty() ? 0 : Integer.parseInt(fixedIn.getQualifier());
            return comparison < 0 || comparison == 0 && installedPatch < fixedPatch;
        }).toList();
    }

    public boolean hasCriticalUpdates(@NotNull Version installedVersion, int installedPatch) {
        return getApplicableCriticalUpdates(installedVersion, installedPatch).stream()
            .anyMatch(update -> update.severity() == PatchSeverity.CRITICAL);
    }

    @NotNull
    public PatchSeverity getUpdateSeverity(@NotNull Version installedVersion, int installedPatch) {
        return getApplicableCriticalUpdates(installedVersion, installedPatch).stream()
            .map(CriticalUpdate::severity)
            .max(PatchSeverity::compareTo)
            .orElse(PatchSeverity.NONE);
    }

    private boolean hasSameBaseVersion(@NotNull Version version) {
        return programVersion.getMajor() == version.getMajor()
            && programVersion.getMinor() == version.getMinor()
            && programVersion.getMicro() == version.getMicro();
    }

    @Nullable
    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(@Nullable String updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * The target patch date, falling back to the base release date when no patch date is available.
     * The base date remains separate because it is also used for license eligibility checks.
     */
    @Nullable
    public String getPatchUpdateTime() {
        if (!patches.isEmpty()) {
            String patchDate = patches.getLast().date();
            if (!patchDate.isBlank()) {
                return patchDate;
            }
        }
        return updateTime;
    }

    @NotNull
    public Date getVersionReleaseTimestamp() {
        if (updateTime == null) {
            return new Date();
        }
        try {
            return new SimpleDateFormat("dd.MM.yyyy").parse(updateTime);
        } catch (ParseException e) {
            return new Date();
        }
    }

    @Nullable
    public String getBaseURL() {
        return baseURL;
    }

    @Nullable
    public String getDownloadURL() {
        return downloadURL;
    }

    @Nullable
    public String getReleaseNotes() {
        return releaseNotes;
    }

    private void parseVersionInfo(@NotNull InputStream inputStream) throws IOException {
        SAXReader parser = new SAXReader(inputStream);
        Map<Integer, VersionPatch> parsedPatches = new TreeMap<>();
        List<CriticalUpdate> parsedCriticalUpdates = new ArrayList<>();
        SAXListener listener = new SAXListener() {
            private final StringBuilder textBuffer = new StringBuilder();
            private final StringBuilder criticalUpdateText = new StringBuilder();
            private int depth;
            private boolean inPatches;
            private Map<String, String> patch;
            private boolean inCriticalUpdates;
            private Map<String, String> criticalUpdate;

            @Override
            public void saxStartElement(@NotNull SAXReader reader, @Nullable String namespaceURI, @NotNull String localName, @NotNull Attributes attributes) {
                depth++;
                textBuffer.setLength(0);
                if (depth == 2 && "patches".equals(localName)) {
                    inPatches = true;
                } else if (depth == 3 && inPatches && "patch".equals(localName)) {
                    patch = new HashMap<>();
                } else if (depth == 2 && "critical-update".equals(localName)) {
                    inCriticalUpdates = true;
                } else if (depth == 3 && inCriticalUpdates && "update".equals(localName)) {
                    criticalUpdate = new HashMap<>();
                    for (String name : List.of("update-date", "fixed-in", "severity")) {
                        criticalUpdate.put(name, attributes.getValue(name));
                    }
                    criticalUpdateText.setLength(0);
                }
            }

            @Override
            public void saxText(@NotNull SAXReader reader, @NotNull String data) {
                textBuffer.append(data);
                if (criticalUpdate != null) {
                    criticalUpdateText.append(data);
                }
            }

            @Override
            public void saxEndElement(@NotNull SAXReader reader, @Nullable String namespaceURI, @NotNull String localName) throws XMLException {
                String text = textBuffer.toString();
                if (patch != null && depth == 4) {
                    if (patch.put(localName, text) != null && "patch-version".equals(localName)) {
                        throw new XMLException("Duplicate patch number field");
                    }
                } else if (patch != null && depth == 3) {
                    VersionPatch descriptor = parsePatch(patch);
                    if (parsedPatches.putIfAbsent(descriptor.number(), descriptor) != null) {
                        throw new XMLException("Duplicate patch number: " + descriptor.number());
                    }
                    patch = null;
                } else if (criticalUpdate != null && depth == 3) {
                    try {
                        parsedCriticalUpdates.add(parseCriticalUpdate(criticalUpdate, criticalUpdateText.toString()));
                    } catch (IllegalArgumentException | ParseException e) {
                        log.warn("Ignoring invalid critical update", e);
                    }
                    criticalUpdate = null;
                } else if (depth == 2) {
                    switch (localName) {
                        case "name" -> programName = text;
                        case "number" -> programVersion = new Version(text.trim());
                        case "date" -> updateTime = text;
                        case "base-url" -> baseURL = text;
                        case "download-url" -> downloadURL = text;
                        case "release-notes" -> releaseNotes = text;
                        case "patches" -> inPatches = false;
                        case "critical-update" -> inCriticalUpdates = false;
                    }
                }
                depth--;
                textBuffer.setLength(0);
            }
        };
        try {
            parser.parse(listener);
            if (programVersion == null) {
                throw new XMLException("Missing version number");
            }
            patches = List.copyOf(parsedPatches.values());
            criticalUpdates = List.copyOf(parsedCriticalUpdates);
        } catch (XMLException | IllegalArgumentException e) {
            throw new IOException("XML parse error", e);
        }
    }

    @NotNull
    private static CriticalUpdate parseCriticalUpdate(
        @NotNull Map<String, String> attributes,
        @NotNull String description
    ) throws ParseException {
        String date = attributes.get("update-date");
        String fixedIn = attributes.get("fixed-in");
        String severity = attributes.get("severity");
        if (date == null || !date.matches("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}")
            || fixedIn == null || !fixedIn.matches("[0-9]+\\.[0-9]+\\.[0-9]+(?:\\.[0-9]+)?")
            || severity == null
        ) {
            throw new IllegalArgumentException("Missing or invalid critical update attributes");
        }
        if (description.isBlank()) {
            throw new IllegalArgumentException("Missing critical update description");
        }
        String[] components = fixedIn.split("\\.");
        Version fixedVersion = new Version(
            Integer.parseInt(components[0]),
            Integer.parseInt(components[1]),
            Integer.parseInt(components[2]),
            components.length == 4 ? Integer.toString(Integer.parseInt(components[3])) : ""
        );
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT);
        dateFormat.setLenient(false);
        return new CriticalUpdate(
            dateFormat.parse(date),
            fixedVersion,
            PatchSeverity.parse(severity),
            description
        );
    }

    @NotNull
    private static VersionPatch parsePatch(@NotNull Map<String, String> fields) throws XMLException {
        String value = fields.getOrDefault("patch-version", "").trim();
        int number = Integer.parseInt(value);
        if (number <= 0 || !value.matches("[0-9]+")) {
            throw new XMLException("Invalid patch number: " + value);
        }
        return new VersionPatch(
            number,
            fields.getOrDefault("patch-date", ""),
            fields.getOrDefault("patch-release-notes", "")
        );
    }
}
