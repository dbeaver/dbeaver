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
package org.jkiss.utils;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.rest.RequestMapping;
import org.jkiss.utils.rest.RequestParameter;
import org.jkiss.utils.rest.RestClient;
import org.jkiss.utils.rest.RestServer;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Map;

public class RestTest extends DBeaverUnitTest {
    @Test
    public void restClientServerTest() {
        final RestServer<Controller> server = RestServer
            .builder(Controller.class, new ControllerImpl())
            .setFilter(address -> address.getAddress().isLoopbackAddress())
            .create();

        final Controller client = RestClient
            .builder(URI.create("http://localhost:" + server.getAddress().getPort()), Controller.class)
            .create();

        Assert.assertEquals("1.0", client.getVersion());
        Assert.assertEquals(Map.of("version", "1.0", "name", "dbeaver"), client.getSettings());
        Assert.assertEquals("1.0", client.getSetting("version"));
        Assert.assertEquals("dbeaver", client.getSetting("name"));
        Assert.assertEquals("cool", client.getSetting("something", "cool"));
        Assert.assertNull(client.getSetting("something"));

        server.stop();
    }

    private interface Controller {
        @NotNull
        @RequestMapping("version")
        String getVersion();

        @NotNull
        @RequestMapping("settings")
        Map<String, Object> getSettings();

        @Nullable
        @RequestMapping("setting")
        Object getSetting(@RequestParameter("key") @NotNull String key);

        @Nullable
        @RequestMapping("setting/default")
        Object getSetting(@RequestParameter("key") @NotNull String key, @RequestParameter("default") @Nullable Object def);
    }

    private static class ControllerImpl implements Controller {
        @NotNull
        @Override
        public String getVersion() {
            return "1.0";
        }

        @NotNull
        @Override
        public Map<String, Object> getSettings() {
            return Map.of("version", "1.0", "name", "dbeaver");
        }

        @Nullable
        @Override
        public Object getSetting(@NotNull String key) {
            return getSettings().get(key);
        }

        @Nullable
        @Override
        public Object getSetting(@NotNull String key, @Nullable Object def) {
            return getSettings().getOrDefault(key, def);
        }
    }
}
