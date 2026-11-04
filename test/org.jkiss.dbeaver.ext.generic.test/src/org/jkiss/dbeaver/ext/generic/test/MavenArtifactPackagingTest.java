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
package org.jkiss.dbeaver.ext.generic.test;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.maven.MavenArtifact;
import org.jkiss.dbeaver.registry.maven.MavenArtifactVersion;
import org.jkiss.dbeaver.registry.maven.MavenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class MavenArtifactPackagingTest {

    @Test
    public void pomOnlyArtifactHasNoJarLocation(@TempDir Path repositoryDir) throws Exception {
        Path artifactDir = repositoryDir.resolve("org/apache/curator/apache-curator/4.2.0");
        Files.createDirectories(artifactDir);
        Files.writeString(artifactDir.resolve("apache-curator-4.2.0.pom"), """
            <project><modelVersion>4.0.0</modelVersion><packaging>pom</packaging>
            <groupId>org.apache.curator</groupId><artifactId>apache-curator</artifactId>
            <version>4.2.0</version></project>
            """);

        MavenRepository repository = new MavenRepository(
            "test", "test", repositoryDir.toUri().toString(), MavenRepository.RepositoryType.LOCAL);
        MavenArtifact artifact = new MavenArtifact(repository, "org.apache.curator", "apache-curator", null, null);
        MavenArtifactVersion version = artifact.resolveVersion(mock(DBRProgressMonitor.class), "4.2.0", false);

        assertEquals(MavenArtifact.FILE_POM, version.getPackaging());
        assertEquals(artifactDir.resolve("apache-curator-4.2.0.pom").toUri().toString(), version.getExternalURL());
    }
}
