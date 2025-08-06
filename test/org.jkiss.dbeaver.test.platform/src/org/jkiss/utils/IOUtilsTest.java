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
package org.jkiss.utils;

import org.junit.Assert;
import org.junit.Test;

public class IOUtilsTest {

    @Test
    public void testLocalPaths() {
        Assert.assertTrue(IOUtils.isLocalFile("c:/absolute/path"));
        Assert.assertTrue(IOUtils.isLocalFile("c:\\absolute\\path"));
        Assert.assertTrue(IOUtils.isLocalFile("/absolute/path"));
        Assert.assertTrue(IOUtils.isLocalFile("\\absolute\\path"));
        Assert.assertTrue(IOUtils.isLocalFile("file:/some/path"));
        Assert.assertTrue(IOUtils.isLocalFile("file://some/path"));
        Assert.assertTrue(IOUtils.isLocalFile("file:some/path"));
        Assert.assertFalse(IOUtils.isLocalFile("s3:/some/path"));
        Assert.assertFalse(IOUtils.isLocalFile("s3://some/path"));
        Assert.assertFalse(IOUtils.isLocalFile("gs://some/path"));
    }
}
