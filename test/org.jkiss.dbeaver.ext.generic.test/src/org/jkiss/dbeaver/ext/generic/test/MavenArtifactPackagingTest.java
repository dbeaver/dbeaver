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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.driver.DriverLibraryMavenArtifact;
import org.jkiss.dbeaver.registry.driver.DriverLibraryMavenDependency;
import org.jkiss.dbeaver.registry.maven.MavenArtifact;
import org.jkiss.dbeaver.registry.maven.MavenArtifactDependency;
import org.jkiss.dbeaver.registry.maven.MavenArtifactVersion;
import org.jkiss.dbeaver.registry.maven.MavenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class MavenArtifactPackagingTest {

    @TempDir
    private Path repositoryDir;
    private MavenRepository repository;
    private DBRProgressMonitor monitor;

    @BeforeEach
    void setUp() {
        repository = new MavenRepository("test", "test", repositoryDir.toUri().toString(), MavenRepository.RepositoryType.LOCAL);
        monitor = mock(DBRProgressMonitor.class);
    }

    @Test
    public void pomOnlyArtifactHasNoJarLocation() throws Exception {
        Path artifactDir = repositoryDir.resolve("org/apache/curator/apache-curator/4.2.0");
        Files.createDirectories(artifactDir);
        Files.writeString(artifactDir.resolve("apache-curator-4.2.0.pom"), """
            <project><modelVersion>4.0.0</modelVersion><packaging>pom</packaging>
            <groupId>org.apache.curator</groupId><artifactId>apache-curator</artifactId>
            <version>4.2.0</version></project>
            """);

        MavenArtifact artifact = new MavenArtifact(repository, "org.apache.curator", "apache-curator", null, null);
        MavenArtifactVersion version = artifact.resolveVersion(monitor, "4.2.0", false);

        assertEquals(MavenArtifact.FILE_POM, version.getPackaging());
        assertEquals(artifactDir.resolve("apache-curator-4.2.0.pom").toUri().toString(), version.getExternalURL());
    }

    @Test
    void jarDependencyUsesJarDespitePomPackaging() throws Exception {
        MavenArtifactVersion root = createArtifact("root", "jar", dependency("thrift", ""));
        MavenArtifactVersion thrift = createArtifact("thrift", "pom", "");
        Path jar = repositoryDir.resolve("test/thrift/1/thrift-1.jar");
        Files.writeString(jar, "driver library");

        MavenArtifactDependency dependency = root.getDependencies().getFirst();
        DriverLibraryMavenDependency library = new DriverLibraryMavenDependency(
            mock(DriverLibraryMavenArtifact.class), thrift, dependency
        );

        assertEquals(jar, library.getLocalFile());
        assertEquals(jar.toUri().toString(), library.getExternalURL(monitor));
        assertEquals(MavenArtifact.FILE_POM, thrift.getPackaging());
        assertEquals(MavenArtifact.FILE_JAR, dependency.getType());
    }

    @Test
    void explicitPomDependencyKeepsPomType() throws Exception {
        MavenArtifactVersion root = createArtifact("root", "jar", dependency("aggregator", "<type>pom</type>"));

        assertEquals(MavenArtifact.FILE_POM, root.getDependencies().getFirst().getType());
    }

    @Test
    void dependencyManagementMatchesTypeAndClassifier() throws Exception {
        MavenArtifactVersion root = createArtifact("root", "jar", """
            <dependencyManagement><dependencies>
              <dependency><groupId>test</groupId><artifactId>client</artifactId><type>pom</type><version>2</version></dependency>
              <dependency><groupId>test</groupId><artifactId>client</artifactId><classifier>standalone</classifier><version>3</version></dependency>
              <dependency><groupId>test</groupId><artifactId>client</artifactId><version>1</version></dependency>
            </dependencies></dependencyManagement>
            <dependencies>
              <dependency><groupId>test</groupId><artifactId>client</artifactId></dependency>
              <dependency><groupId>test</groupId><artifactId>client</artifactId><type>pom</type></dependency>
              <dependency><groupId>test</groupId><artifactId>client</artifactId><classifier>standalone</classifier></dependency>
            </dependencies>
            """);

        assertEquals(List.of("1", "2", "3"), root.getDependencies().stream().map(MavenArtifactDependency::getVersion).toList());
        assertEquals(List.of("jar", "pom", "jar"), root.getDependencies().stream().map(MavenArtifactDependency::getType).toList());
    }

    @NotNull
    private MavenArtifactVersion createArtifact(
        @NotNull String artifactId,
        @NotNull String packaging,
        @NotNull String content
    ) throws Exception {
        Path directory = repositoryDir.resolve("test/" + artifactId + "/1");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(artifactId + "-1.pom"), """
            <project><modelVersion>4.0.0</modelVersion>
            <groupId>test</groupId><artifactId>%s</artifactId><version>1</version><packaging>%s</packaging>
            %s</project>
            """.formatted(artifactId, packaging, content));
        return new MavenArtifact(repository, "test", artifactId, null, null).resolveVersion(monitor, "1", false);
    }

    @NotNull
    private static String dependency(@NotNull String artifactId, @NotNull String type) {
        return """
            <dependencies><dependency><groupId>test</groupId><artifactId>%s</artifactId><version>1</version>%s</dependency></dependencies>
            """.formatted(artifactId, type);
    }

}
