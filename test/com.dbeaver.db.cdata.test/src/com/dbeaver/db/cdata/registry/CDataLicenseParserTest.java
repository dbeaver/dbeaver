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

import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class CDataLicenseParserTest extends DBeaverUnitTest {
    @TempDir
    Path tempDirectory;

    @Test
    public void parseDriverInformation() {
        Assertions.assertEquals(CDataLicenseStatus.NOT_INSTALLED, parseInformation("No License").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.TRIAL_ACTIVE, parseInformation("Trial license, 20 days remaining").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.TRIAL_EXPIRING, parseInformation("Trial license expires in 3 days").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.TRIAL_EXPIRED, parseInformation("Trial license has expired").getStatus());
        Assertions.assertEquals(
            CDataLicenseStatus.TRIAL_ACTIVE,
            parseInformation("Limited Trial Version - EXPIRING TRIAL [29 DAYS LEFT]").getStatus()
        );
        Assertions.assertEquals(
            CDataLicenseStatus.TRIAL_EXPIRING,
            parseInformation("Limited Trial Version - EXPIRING TRIAL [3 DAYS LEFT]").getStatus()
        );
        Assertions.assertEquals(
            CDataLicenseStatus.TRIAL_EXPIRED,
            parseInformation("Limited Trial Version - EXPIRED").getStatus()
        );
        Assertions.assertEquals(CDataLicenseStatus.PURCHASED_ACTIVE, parseInformation("Single Developer License").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.PURCHASED_EXPIRING, parseInformation("License expires in 2 days").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.PURCHASED_EXPIRING, parseInformation("Single Developer License, 2 days left").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.MACHINE_MISMATCH, parseInformation("License machine mismatch").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.WRONG_MAJOR_VERSION, parseInformation("License version mismatch").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, parseInformation("Unknown License").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, parseInformation("Unexpected vendor response").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, parseInformation("Trial license not installed").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, parseInformation("Trial validation unavailable").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.INVALID_KEY, parseInformation("Invalid trial license").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.INVALID_KEY, parseInformation("Trial license is invalid").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, parseInformation("Trial license validation failed").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, parseInformation("Purchased license validation failed").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, parseInformation("Single Developer License inactive").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, parseInformation("Single Developer License revoked").getStatus());
        Assertions.assertEquals(
            CDataLicenseStatus.VALIDATION_UNAVAILABLE,
            CDataLicenseParser.parseInformation(Map.of("NodeId", "test-node")).getStatus()
        );
    }

    @Test
    public void parseActivationResult() {
        Assertions.assertEquals(
            CDataLicenseStatus.TRIAL_ACTIVE,
            CDataLicenseParser.parseActivation(
                0,
                "Downloading license data... Verifying license data... License installation succeeded.",
                CDataLicenseType.TRIAL,
                true
            )
        );
        Assertions.assertEquals(
            CDataLicenseStatus.PURCHASED_ACTIVE,
            CDataLicenseParser.parseActivation(
                0,
                "License installation succeeded.",
                CDataLicenseType.PURCHASED,
                true
            )
        );
        Assertions.assertEquals(
            CDataLicenseStatus.INVALID_KEY,
            CDataLicenseParser.parseActivation(
                1,
                "Error validating user input: Invalid product key [code: C nodeid: R4P8GJ6B].",
                CDataLicenseType.PURCHASED,
                false
            )
        );
        Assertions.assertEquals(
            CDataLicenseStatus.VALIDATION_UNAVAILABLE,
            CDataLicenseParser.parseActivation(0, "License installation succeeded.", CDataLicenseType.TRIAL, false)
        );
    }

    @Test
    public void licenseStateFlags() {
        Assertions.assertTrue(CDataLicenseStatus.TRIAL_ACTIVE.isValid());
        Assertions.assertTrue(CDataLicenseStatus.TRIAL_ACTIVE.isTrial());
        Assertions.assertTrue(CDataLicenseStatus.PURCHASED_EXPIRING.isValid());
        Assertions.assertFalse(CDataLicenseStatus.PURCHASED_EXPIRING.isTrial());
        Assertions.assertFalse(CDataLicenseStatus.EXPIRED.isValid());
    }

    @Test
    public void activationOutputDoesNotExposeUserData() {
        CDataLicenseActivationRequest request = new CDataLicenseActivationRequest(
            "Test User",
            "test@example.org",
            CDataLicenseType.PURCHASED,
            "secret-key"
        );
        String output = CDataLicenseActivator.sanitizeOutput(
            "Name: Test User Email: test@example.org Product Key: secret-key Invalid product key",
            request
        );
        Assertions.assertFalse(output.contains(request.name()));
        Assertions.assertFalse(output.contains(request.email()));
        Assertions.assertFalse(output.contains(request.productKey()));
        Assertions.assertTrue(output.contains("Invalid product key"));
        Assertions.assertEquals(
            "Please enter your Product Key: (you may use \"TRIAL\" as product key) Product Key: Verifying license data...",
            CDataLicenseActivator.sanitizeOutput(
                "Please enter your Product Key:\n  (you may use \"TRIAL\" as product key)\nProduct Key: Verifying license data...",
                request
            )
        );
    }

    @Test
    public void answerInteractiveActivationPrompts() throws Exception {
        Path testBundle = Path.of(CDataPromptProcess.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .toURI());
        CDataProcessExecutor.ProcessResult result = CDataProcessExecutor.execute(
            new VoidProgressMonitor(),
            List.of(
                GeneralUtils.findJavaExecutable(),
                "-cp",
                testBundle.toString(),
                CDataPromptProcess.class.getName()
            ),
            testBundle.toFile().isDirectory() ? testBundle : testBundle.getParent(),
            "CDATA prompt test",
            List.of(
                new CDataProcessExecutor.PromptResponse("Name:", "Test User"),
                new CDataProcessExecutor.PromptResponse("Email Address:", "test@example.org"),
                new CDataProcessExecutor.PromptResponse("Product Key:", "TRIAL", true),
                new CDataProcessExecutor.PromptResponse("Press any key to exit", "")
            )
        );
        Assertions.assertEquals(0, result.exitCode());
        Assertions.assertTrue(result.output().contains("License installation succeeded"));
    }

    @Test
    public void useCanonicalCDataLicensePaths() throws Exception {
        Path licensePath = CDataDriverLoaderDescriptor.getLicensePath("cdata.jdbc.aas");
        Assertions.assertEquals(
            RuntimeUtils.getUserHomePath().resolve(".CData/cdata.jdbc.aas.lic"),
            licensePath
        );
        Assertions.assertEquals("cdata.jdbc.aas.jar", CDataLicenseActivator.getCanonicalJarName(licensePath));
        Assertions.assertEquals(
            RuntimeUtils.getUserHomePath().resolve(".CData/cdata.jdbc.aas.lic"),
            CDataDriverLoaderDescriptor.getLicensePath("cdata.jdbc.aas")
        );
        Assertions.assertEquals(
            RuntimeUtils.getUserHomePath().resolve(
                ".CData/drivers/cdata.jdbc.salesforce/test-fingerprint/cdata.jdbc.salesforce.jar"
            ),
            CDataDriverLoaderDescriptor.getCanonicalDriverPath("cdata.jdbc.salesforce", "test-fingerprint")
        );
    }

    @Test
    public void resolveCDataDriverIconUrl() {
        Assertions.assertEquals(
            "https://www.cdata.com/ui/img/drivers/icon-amazonmarketplace.png",
            CDataDriverIconLoader.getIconUri("amazonmarketplace").toString()
        );
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> CDataDriverIconLoader.getIconUri("../amazonmarketplace")
        );
    }

    @Test
    public void licenseProbeReportsFailureCause() throws Exception {
        Path modelBundle = Path.of(CDataLicenseProbe.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .toURI());
        CDataProcessExecutor.ProcessResult result = CDataProcessExecutor.execute(
            new VoidProgressMonitor(),
            List.of(
                GeneralUtils.findJavaExecutable(),
                "-cp",
                modelBundle.toString(),
                CDataLicenseProbe.class.getName(),
                "missing.Driver"
            ),
            modelBundle.toFile().isDirectory() ? modelBundle : modelBundle.getParent(),
            "CDATA probe diagnostic test",
            List.of()
        );
        Assertions.assertEquals(2, result.exitCode());
        Assertions.assertTrue(result.output().contains(CDataLicenseProbe.ERROR_PREFIX));
    }

    @Test
    public void restorePreviousLicenseAfterFailedValidation() throws Exception {
        Path target = tempDirectory.resolve("cdata.jdbc.test.lic");
        Path staged = tempDirectory.resolve("staged.lic");
        Files.writeString(target, "previous-license");
        Files.writeString(staged, "new-license");

        CDataLicenseActivationException exception = Assertions.assertThrows(
            CDataLicenseActivationException.class,
            () -> CDataLicenseActivator.installAndValidateLicense(
                staged,
                target,
                tempDirectory.resolve("backup.lic"),
                () -> new CDataDriverLicense(CDataLicenseStatus.INVALID_KEY, "", null)
            )
        );

        Assertions.assertEquals(CDataLicenseStatus.INVALID_KEY, exception.getStatus());
        Assertions.assertEquals("previous-license", Files.readString(target));
    }

    @Test
    public void removeNewLicenseAfterFailedValidation() throws Exception {
        Path target = tempDirectory.resolve("cdata.jdbc.test.lic");
        Path staged = tempDirectory.resolve("staged.lic");
        Files.writeString(staged, "new-license");

        Assertions.assertThrows(
            CDataLicenseActivationException.class,
            () -> CDataLicenseActivator.installAndValidateLicense(
                staged,
                target,
                tempDirectory.resolve("backup.lic"),
                () -> new CDataDriverLicense(CDataLicenseStatus.VALIDATION_UNAVAILABLE, "", null)
            )
        );

        Assertions.assertFalse(Files.exists(target));
    }

    @Test
    public void keepLicenseAfterSuccessfulValidation() throws Exception {
        Path target = tempDirectory.resolve("cdata.jdbc.test.lic");
        Path staged = tempDirectory.resolve("staged.lic");
        Files.writeString(staged, "new-license");
        CDataDriverLicense verified = new CDataDriverLicense(CDataLicenseStatus.TRIAL_ACTIVE, "test-node", null);

        Assertions.assertSame(
            verified,
            CDataLicenseActivator.installAndValidateLicense(
                staged,
                target,
                tempDirectory.resolve("backup.lic"),
                () -> verified
            )
        );
        Assertions.assertEquals("new-license", Files.readString(target));
    }

    private static CDataDriverLicense parseInformation(String license) {
        return CDataLicenseParser.parseInformation(Map.of("License", license, "NodeId", "test-node"));
    }
}
