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
package org.jkiss.dbeaver.ext.cdata.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class CDataLicenseCheckTest extends DBeaverUnitTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    @TempDir
    Path tempDirectory;

    @Test
    public void parseDeveloperLicense() {
        CDataDriverLicense license = parse("""
            {
              "active": true,
              "expiration_date": "2027-09-18",
              "license_type": "Developer"
            }
            """);
        Assertions.assertEquals(CDataLicenseStatus.PURCHASED_ACTIVE, license.getStatus());
        Assertions.assertEquals(352, license.getRemainingDays());
        Assertions.assertFalse(license.isTrialLicense());
    }

    @Test
    public void recognizeExpirationBoundaries() {
        for (String type : List.of("Trial", "Developer")) {
            boolean trial = type.equals("Trial");
            for (int days : List.of(-1, 0, 1, 3, 4, 30)) {
                CDataDriverLicense license = parse(response(true, type, TODAY.plusDays(days)));
                CDataLicenseStatus expected;
                if (days < 0) {
                    expected = trial ? CDataLicenseStatus.TRIAL_EXPIRED : CDataLicenseStatus.EXPIRED;
                } else if (days <= 3) {
                    expected = trial ? CDataLicenseStatus.TRIAL_EXPIRING : CDataLicenseStatus.PURCHASED_EXPIRING;
                } else {
                    expected = trial ? CDataLicenseStatus.TRIAL_ACTIVE : CDataLicenseStatus.PURCHASED_ACTIVE;
                }
                Assertions.assertEquals(expected, license.getStatus());
                Assertions.assertEquals(days < 0 ? null : Integer.valueOf(days), license.getRemainingDays());
            }
            Assertions.assertEquals(trial ? CDataLicenseStatus.TRIAL_EXPIRED : CDataLicenseStatus.EXPIRED,
                parse(response(false, type, TODAY.minusDays(1))).getStatus());
            Assertions.assertEquals(trial ? CDataLicenseStatus.TRIAL_EXPIRED : CDataLicenseStatus.EXPIRED,
                parse(response(false, type, TODAY)).getStatus());
            Assertions.assertFalse(parse(response(false, type, TODAY.plusDays(30))).getStatus().allowsDriverUsage());
        }
    }

    @Test
    public void acceptLicenseWithoutExpiration() {
        for (String expiration : List.of("", ", \"expiration_date\": null", ", \"expiration_date\": \"\"")) {
            CDataDriverLicense license = parse("{\"active\": true, \"license_type\": \"Developer\"" + expiration + "}");
            Assertions.assertEquals(CDataLicenseStatus.PURCHASED_ACTIVE, license.getStatus());
            Assertions.assertNull(license.getRemainingDays());
        }
    }

    @Test
    public void rejectInactiveLicenseEvenWithIncompleteMetadata() {
        for (String output : List.of(
            "{\"active\":false}",
            "{\"active\":false,\"license_type\":null,\"expiration_date\":null}",
            "{\"active\":false,\"license_type\":\"Developer\",\"expiration_date\":\"invalid\"}"
        )) {
            Assertions.assertEquals(CDataLicenseStatus.INVALID_KEY, parse(output).getStatus());
            Assertions.assertFalse(parse(output).getStatus().allowsDriverUsage());
        }
    }

    @Test
    public void handleUnrecognizedOutput() {
        for (String output : List.of(
            "", "null", "[]", "{}", "CData driver help", "{",
            "{\"active\":\"true\",\"license_type\":\"Developer\"}",
            "{\"active\":1,\"license_type\":\"Developer\"}",
            "{\"active\":true}",
            "{\"active\":true,\"license_type\":false}",
            "{\"active\":true,\"license_type\":\"Developer\",\"expiration_date\":\"2026-02-30\"}",
            "{\"active\":true,\"license_type\":\"Developer\",\"expiration_date\":42}"
        )) {
            Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, parse(output).getStatus(), output);
        }
    }

    @Test
    public void checkLicenseThroughDriverJar() throws Exception {
        CDataResolvedDriver driver = createDriver();
        Files.writeString(driver.licensePath(), "installed-license");
        Path responsePath = driver.jarPath().resolveSibling("license-check.json");
        Files.writeString(responsePath, response(true, "Developer", LocalDate.now().plusDays(30)));
        Assertions.assertEquals(CDataLicenseStatus.PURCHASED_ACTIVE,
            CDataLicenseValidator.validate(new VoidProgressMonitor(), driver).getStatus());

        Files.writeString(responsePath, response(false, "Developer", LocalDate.now().plusDays(30)));
        Assertions.assertEquals(CDataLicenseStatus.INVALID_KEY,
            CDataLicenseValidator.validate(new VoidProgressMonitor(), driver).getStatus());
        Assertions.assertTrue(Files.exists(driver.licensePath()));

        Files.delete(driver.licensePath());
        Assertions.assertEquals(CDataLicenseStatus.NOT_INSTALLED,
            CDataLicenseValidator.validate(new VoidProgressMonitor(), driver).getStatus());
        Assertions.assertFalse(Files.exists(driver.jarPath().resolveSibling("legacy-check")));
    }

    @Test
    public void handleFailedLicenseCheck() throws Exception {
        CDataResolvedDriver driver = createDriver();
        Path responsePath = driver.jarPath().resolveSibling("license-check.json");
        Files.writeString(responsePath, "unsupported command");
        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE,
            CDataLicenseValidator.validate(new VoidProgressMonitor(), driver).getStatus());

        Files.createFile(driver.jarPath().resolveSibling("fail-check"));
        Files.writeString(driver.licensePath(), "installed-license");
        Files.writeString(responsePath, response(false, "Trial", LocalDate.now().minusDays(1)));
        Assertions.assertEquals(CDataLicenseStatus.TRIAL_EXPIRED,
            CDataLicenseValidator.validate(new VoidProgressMonitor(), driver).getStatus());

        Files.writeString(responsePath, response(true, "Developer", LocalDate.now().plusDays(30)));
        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE,
            CDataLicenseValidator.validate(new VoidProgressMonitor(), driver).getStatus());
    }

    @Test
    public void fallBackToDriverInformation() throws Exception {
        CDataResolvedDriver driver = createDriver();
        Path folder = driver.jarPath().getParent();
        Files.writeString(folder.resolve("license-information.txt"), "Trial license, 20 days remaining");
        for (String output : List.of("unsupported command", "CData driver help", "", "{}")) {
            for (boolean failed : List.of(false, true)) {
                Files.writeString(folder.resolve("license-check.json"), output);
                if (failed) {
                    Files.createFile(folder.resolve("fail-check"));
                }
                CDataDriverLicense license = CDataLicenseValidator.validate(new VoidProgressMonitor(), driver);
                Assertions.assertEquals(CDataLicenseStatus.TRIAL_ACTIVE, license.getStatus());
                Assertions.assertEquals(20, license.getRemainingDays());
                Assertions.assertEquals("test-node", license.getLicenseId());
                Assertions.assertTrue(Files.deleteIfExists(folder.resolve("legacy-check")));
                Files.deleteIfExists(folder.resolve("fail-check"));
            }
        }
    }

    @Test
    public void preserveLegacyLicenseStatusAndInstalledFile() throws Exception {
        CDataResolvedDriver driver = createDriver();
        Path folder = driver.jarPath().getParent();
        Files.writeString(folder.resolve("license-check.json"), "unsupported command");
        Files.writeString(folder.resolve("license-information.txt"), "No License");
        Assertions.assertEquals(CDataLicenseStatus.NOT_INSTALLED,
            CDataLicenseValidator.validate(new VoidProgressMonitor(), driver).getStatus());

        Files.writeString(driver.licensePath(), "installed-license");
        for (var entry : Map.of(
            "No License", CDataLicenseStatus.VALIDATION_UNAVAILABLE,
            "Single Developer License", CDataLicenseStatus.PURCHASED_ACTIVE,
            "Trial license has expired", CDataLicenseStatus.TRIAL_EXPIRED,
            "License machine mismatch", CDataLicenseStatus.MACHINE_MISMATCH
        ).entrySet()) {
            Files.delete(folder.resolve("legacy-check"));
            Files.writeString(folder.resolve("license-information.txt"), entry.getKey());
            Assertions.assertEquals(entry.getValue(),
                CDataLicenseValidator.validate(new VoidProgressMonitor(), driver).getStatus());
            Assertions.assertEquals("installed-license", Files.readString(driver.licensePath()));
        }
    }

    @Test
    public void doNotFallBackFromRecognizedLicenseCheck() throws Exception {
        CDataResolvedDriver driver = createDriver();
        Path folder = driver.jarPath().getParent();
        Files.writeString(driver.licensePath(), "installed-license");
        Files.writeString(folder.resolve("license-information.txt"), "Single Developer License");
        Files.createFile(folder.resolve("fail-check"));
        for (var entry : Map.of(
            response(false, "Trial", LocalDate.now().minusDays(1)), CDataLicenseStatus.TRIAL_EXPIRED,
            "{\"active\":false}", CDataLicenseStatus.INVALID_KEY,
            response(true, "Developer", LocalDate.now().plusDays(30)), CDataLicenseStatus.VALIDATION_UNAVAILABLE
        ).entrySet()) {
            Files.writeString(folder.resolve("license-check.json"), entry.getKey());
            Assertions.assertEquals(entry.getValue(),
                CDataLicenseValidator.validate(new VoidProgressMonitor(), driver).getStatus());
            Assertions.assertFalse(Files.exists(folder.resolve("legacy-check")));
        }
    }

    @NotNull
    private CDataResolvedDriver createDriver() throws Exception {
        Path folder = Files.createDirectories(tempDirectory.resolve("driver with spaces"));
        Path jar = folder.resolve("cdata.jdbc.test.jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Main-Class", CDataLicenseCheckProcess.class.getName());
        String classEntry = CDataLicenseCheckProcess.class.getName().replace('.', '/') + ".class";
        try (
            var output = new JarOutputStream(Files.newOutputStream(jar), manifest);
            var input = CDataLicenseCheckProcess.class.getResourceAsStream("/" + classEntry)
        ) {
            Assertions.assertNotNull(input);
            output.putNextEntry(new JarEntry(classEntry));
            input.transferTo(output);
            output.closeEntry();
        }
        return new CDataResolvedDriver(jar, CDataLicenseCheckProcess.class.getName(), folder.resolve("cdata.jdbc.test.lic"));
    }

    @NotNull
    private static CDataDriverLicense parse(@NotNull String output) {
        return CDataLicenseParser.parseLicenseCheck(output, TODAY);
    }

    @NotNull
    private static String response(boolean active, @NotNull String type, @NotNull LocalDate expiration) {
        return "{\"active\":" + active + ",\"license_type\":\"" + type + "\",\"expiration_date\":\"" + expiration + "\"}";
    }
}
