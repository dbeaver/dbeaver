package org.jkiss.dbeaver.registry.maven.versioning;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Describes an artifact version in terms of its components, converts it to/from a string and
 * compares two versions.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface ArtifactVersion
    extends Comparable<ArtifactVersion>
{
    int getMajorVersion();

    int getMinorVersion();

    int getIncrementalVersion();

    int getBuildNumber();

    String getQualifier();

    void parseVersion(String version);
}
