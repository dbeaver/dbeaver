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

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.maven.MavenArtifactDependency;
import org.jkiss.dbeaver.registry.maven.MavenArtifactReference;
import org.jkiss.dbeaver.registry.maven.MavenArtifactVersion;

import java.util.List;

/**
 * DriverLibraryDescriptor
 */
public class DriverLibraryMavenDependency extends DriverLibraryMavenArtifact
{
    private DriverLibraryMavenArtifact parent;
    private MavenArtifactDependency source;

    public DriverLibraryMavenDependency(DriverLibraryMavenArtifact parent, MavenArtifactVersion localVersion, MavenArtifactDependency source) {
        super(parent.getDriver(), FileType.jar, PATH_PREFIX + localVersion.toString(), null);
        this.parent = parent;
        this.localVersion = localVersion;
        this.source = source;
    }

    protected boolean isDependencyExcluded(DBRProgressMonitor monitor, MavenArtifactDependency dependency) {
        List<MavenArtifactReference> exclusions = source.getExclusions();
        if (exclusions != null) {
            for (MavenArtifactReference exReference : exclusions) {
                if (exReference.getGroupId().equals(dependency.getGroupId()) && exReference.getArtifactId().equals(dependency.getArtifactId())) {
                    return true;
                }
            }
        }

        return parent.isDependencyExcluded(monitor, dependency);
    }

}
