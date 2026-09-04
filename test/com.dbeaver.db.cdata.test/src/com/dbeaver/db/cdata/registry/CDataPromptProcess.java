/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dbeaver.db.cdata.registry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class CDataPromptProcess {
    private CDataPromptProcess() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String name = prompt(reader, "Name:");
        String email = prompt(reader, "Email Address:");
        System.out.println("Please enter your Product Key:");
        System.out.println("  (you may use \"TRIAL\" as product key)");
        System.out.flush();
        Thread.sleep(250);
        if (reader.ready()) {
            System.out.println("Product key was sent before the prompt");
            System.exit(2);
        }
        String key = prompt(reader, "Product Key:");
        if ("Test User".equals(name) && "test@example.org".equals(email) && "TRIAL".equals(key)) {
            System.out.println("License installation succeeded");
        } else {
            System.out.println("Invalid input");
        }
        prompt(reader, "Press any key to exit");
    }

    private static String prompt(BufferedReader reader, String prompt) throws IOException {
        System.out.print(prompt);
        System.out.flush();
        return reader.readLine();
    }
}
