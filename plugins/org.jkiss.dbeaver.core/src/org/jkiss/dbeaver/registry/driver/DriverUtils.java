/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.registry.driver;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

/**
 * DriverUtils
 */
public class DriverUtils
{

    public static boolean isBetaVersion(@NotNull String versionInfo) {
        return versionInfo.contains("beta") || versionInfo.contains("alpha");
    }

    @Nullable
    public static String findLatestVersion(@NotNull Collection<String> allVersions) {
        String latest = null;
        for (String version : allVersions) {
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
                if (latest == null || compareVersions(version, latest) > 0) {
                    latest = version;
                }
            }
        }
        return latest;
    }

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
}
