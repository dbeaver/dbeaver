/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.utils.rest;

import com.google.gson.Gson;
import org.jkiss.code.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class RestTest {
    public static void main(String[] args) throws IOException {
        final Gson gson = new Gson();
        final RestServer<Controller> server = new RestServer<>(new ControllerImpl(), Controller.class, gson, 5432);
        final Controller client = RestClient.create(URI.create("http://localhost:5432"), Controller.class, gson);

        System.out.println("client.getVersion() = " + client.getVersion());
        System.out.println("client.getSettings() = " + client.getSettings());

        client.log("Hello");
        client.log("Hello %s", "DBeaver");

        server.stop(0);
    }

    public interface Controller {
        @NotNull
        @RequestMapping("version")
        String getVersion();

        @NotNull
        @RequestMapping("settings")
        Map<String, Object> getSettings();

        @RequestMapping("log")
        void log(@RequestParameter("message") @NotNull String message);

        @RequestMapping("log/formatted")
        void log(@RequestParameter("format") @NotNull String format, @RequestParameter("args") @NotNull Object... args);
    }

    public static class ControllerImpl implements Controller {
        @NotNull
        @Override
        public String getVersion() {
            return "1.0";
        }

        @NotNull
        @Override
        public Map<String, Object> getSettings() {
            return Map.of("version", "1.0", "name", "rest");
        }

        @Override
        public void log(@NotNull String message) {
            System.out.println(message);
        }

        @Override
        public void log(@NotNull String format, @NotNull Object... args) {
            System.out.printf(format + "%n", args);
        }
    }
}
