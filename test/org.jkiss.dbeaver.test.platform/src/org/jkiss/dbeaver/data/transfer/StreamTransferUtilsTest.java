/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.data.transfer;


import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class StreamTransferUtilsTest extends DBeaverUnitTest {

    @Test
    public void testDelimiterString() {
        Map<String, Object> props = new HashMap<>();
        props.put("delimiter", " \\t\\n\\r");
        Assert.assertEquals(" \t\n\r", StreamTransferUtils.getDelimiterString(props, "delimiter"));
        props.put("delimiter", "");
        Assert.assertEquals(",", StreamTransferUtils.getDelimiterString(props, "delimiter"));
        props.put("delimiter", null);
        Assert.assertEquals(",", StreamTransferUtils.getDelimiterString(props, "delimiter"));
    }
}
