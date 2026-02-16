/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.utils;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Collection;
import java.util.StringTokenizer;

/**
 * VersionUtils - utilities for parsing, comparing, and managing version strings.
 */
public class VersionUtils {

    /**
     * Checks whether the given version string represents a beta or alpha release.
     *
     * @param versionInfo version string to check
     * @return true if the version contains "beta" or "alpha" (case-insensitive)
     */
    public static boolean isBetaVersion(@NotNull String versionInfo) {
        String lower = versionInfo.toLowerCase();
        return lower.contains("beta") || lower.contains("alpha");
    }

    /**
     * Finds the latest stable version from a collection. If no stable versions exist,
     * falls back to the latest beta/alpha version.
     *
     * @param allVersions collection of version strings
     * @return the latest version, or null if the collection is empty
     */
    @Nullable
    public static String findLatestVersion(@NotNull Collection<String> allVersions) {
        String latest = null;
        for (String version : allVersions) {
            if (version == null || version.isEmpty()) {
                continue;
            }
            if (isBetaVersion(version)) {
                continue;
            }
            if (latest == null || compareVersions(version, latest) > 0) {
                latest = version;
            }
        }
        if (latest == null) {
            // Now use beta versions too
            for (String version : allVersions) {
                if (version == null || version.isEmpty()) {
                    continue;
                }
                if (latest == null || compareVersions(version, latest) > 0) {
                    latest = version;
                }
            }
        }
        return latest;
    }

    /**
     * Checks whether version v1 is strictly less than version v2.
     *
     * @param v1 first version string
     * @param v2 second version string
     * @return true if v1 &lt; v2
     */
    public static boolean isVersionLessThan(@NotNull String v1, @NotNull String v2) {
        int compareResult = compareVersions(v1, v2);
        return compareResult < 0;
    }

    /**
     * Checks whether version v1 is greater than or equal to version v2.
     *
     * @param v1 first version string
     * @param v2 second version string
     * @return true if v1 &gt;= v2
     */
    public static boolean isVersionGreaterThanOrEqual(@NotNull String v1, @NotNull String v2) {
        return compareVersions(v1, v2) >= 0;
    }

    /**
     * Compares two version strings component by component.
     * Supports separators: '.', '-', '_'.
     * Numeric components are compared numerically; non-numeric lexicographically.
     *
     * @param v1 first version string
     * @param v2 second version string
     * @return negative if v1 &lt; v2, positive if v1 &gt; v2, zero if equal
     */
    public static int compareVersions(@NotNull String v1, @NotNull String v2) {
        StringTokenizer st1 = new StringTokenizer(v1, ".-_");
        StringTokenizer st2 = new StringTokenizer(v2, ".-_");
        while (st1.hasMoreTokens() && st2.hasMoreTokens()) {
            String t1 = st1.nextToken();
            String t2 = st2.nextToken();
            try {
                int cmp = Integer.parseInt(t1) - Integer.parseInt(t2);
                if (cmp != 0) {
                    return cmp;
                }
            } catch (NumberFormatException e) {
                // Non-numeric versions - use lexicographical compare
                int cmp = t1.compareTo(t2);
                if (cmp != 0) {
                    return cmp;
                }
            }
        }
        if (st1.hasMoreTokens()) {
            return 1;
        } else if (st2.hasMoreTokens()) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * Extracts the major version number from a version string.
     * For example, "24.3.1" returns 24, "3.0-beta" returns 3.
     *
     * @param version the version string
     * @return the major version number, or 0 if it cannot be parsed
     */
    public static int getMajorVersion(@NotNull String version) {
        return getVersionComponent(version, 0);
    }

    /**
     * Extracts the minor version number from a version string.
     * For example, "24.3.1" returns 3, "3.0-beta" returns 0.
     *
     * @param version the version string
     * @return the minor version number, or 0 if not present or cannot be parsed
     */
    public static int getMinorVersion(@NotNull String version) {
        return getVersionComponent(version, 1);
    }

    /**
     * Extracts the patch version number from a version string.
     * For example, "24.3.1" returns 1, "3.0" returns 0.
     *
     * @param version the version string
     * @return the patch version number, or 0 if not present or cannot be parsed
     */
    public static int getPatchVersion(@NotNull String version) {
        return getVersionComponent(version, 2);
    }

    /**
     * Extracts a specific numeric component from a version string.
     *
     * @param version the version string
     * @param index   the zero-based component index (0=major, 1=minor, 2=patch, etc.)
     * @return the component value, or 0 if the index is out of range or non-numeric
     */
    private static int getVersionComponent(@NotNull String version, int index) {
        StringTokenizer st = new StringTokenizer(version, ".-_");
        int current = 0;
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (current == index) {
                try {
                    return Integer.parseInt(token);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
            current++;
        }
        return 0;
    }

    /**
     * Checks whether a version string is within a given range (inclusive).
     *
     * @param version    the version to check
     * @param minVersion the minimum version (inclusive), or null for no lower bound
     * @param maxVersion the maximum version (inclusive), or null for no upper bound
     * @return true if the version is within the specified range
     */
    public static boolean isVersionInRange(
        @NotNull String version,
        @Nullable String minVersion,
        @Nullable String maxVersion
    ) {
        if (minVersion != null && compareVersions(version, minVersion) < 0) {
            return false;
        }
        if (maxVersion != null && compareVersions(version, maxVersion) > 0) {
            return false;
        }
        return true;
    }
}
