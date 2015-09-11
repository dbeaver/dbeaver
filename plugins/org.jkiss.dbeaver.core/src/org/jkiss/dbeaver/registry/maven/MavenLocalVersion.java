/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.registry.maven;

import org.jkiss.dbeaver.Log;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Maven artifact version info.
 * It is a resolved version information. This version exists in maven repository and can be obtained.
 * Also it is cached locally.
 */
public class MavenLocalVersion
{
    static final Log log = Log.getLog(MavenLocalVersion.class);

    private MavenArtifact artifact;
    private String version;
    private String fileName;
    private Date updateTime;
    private MavenArtifactVersion metaData;

    public MavenLocalVersion(MavenArtifact artifact, String version, String fileName, Date updateTime) {
        this.artifact = artifact;
        this.version = version;
        this.fileName = fileName;
        this.updateTime = updateTime;
    }

    public MavenArtifact getArtifact() {
        return artifact;
    }

    public String getVersion() {
        return version;
    }

    public String getFileName() {
        return fileName;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public File getCacheFile() {
        return new File(artifact.getRepository().getLocalCacheDir(), artifact.getGroupId() + "/" + fileName);
    }

    public String getExternalURL(String fileType) {
        return artifact.getFileURL(version, fileType);
    }

    public MavenArtifactVersion getMetaData() {
        if (metaData == null) {
            try {
                metaData = new MavenArtifactVersion(this);
            } catch (IOException e) {
                log.warn("Error fetching POM file", e);
                metaData = new MavenArtifactVersion(artifact.getArtifactId(), version);
            }
        }
        return metaData;
    }

    @Override
    public String toString() {
        return artifact.toString() + ":" + version + ":" + fileName;
    }

}
