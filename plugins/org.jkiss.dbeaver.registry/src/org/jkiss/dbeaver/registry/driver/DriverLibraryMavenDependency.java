/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
