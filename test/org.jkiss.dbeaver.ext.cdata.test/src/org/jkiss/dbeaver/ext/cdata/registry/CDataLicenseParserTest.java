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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

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
        Assertions.assertEquals(
            CDataLicenseStatus.PURCHASED_EXPIRING,
            parseInformation("Single Developer License, 2 days left").getStatus()
        );
        Assertions.assertEquals(CDataLicenseStatus.MACHINE_MISMATCH, parseInformation("License machine mismatch").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.WRONG_MAJOR_VERSION, parseInformation("License version mismatch").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, parseInformation("Unknown License").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, parseInformation("Unexpected vendor response").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, parseInformation("Trial license not installed").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, parseInformation("Trial validation unavailable").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.INVALID_KEY, parseInformation("Invalid trial license").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.INVALID_KEY, parseInformation("Trial license is invalid").getStatus());
        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, parseInformation("Trial license validation failed").getStatus());
        Assertions.assertEquals(
            CDataLicenseStatus.VALIDATION_UNAVAILABLE,
            parseInformation("Purchased license validation failed").getStatus()
        );
        Assertions.assertEquals(
            CDataLicenseStatus.VALIDATION_UNAVAILABLE,
            parseInformation("Single Developer License inactive").getStatus()
        );
        Assertions.assertEquals(
            CDataLicenseStatus.VALIDATION_UNAVAILABLE,
            parseInformation("Single Developer License revoked").getStatus()
        );
        Assertions.assertEquals(
            CDataLicenseStatus.VALIDATION_UNAVAILABLE,
            CDataLicenseParser.parseInformation(Map.of("NodeId", "test-node")).getStatus()
        );
        Assertions.assertEquals(29, parseInformation("Limited Trial Version - EXPIRING TRIAL [29 DAYS LEFT]").getRemainingDays());
        Assertions.assertNull(parseInformation("Single Developer License").getRemainingDays());
    }

    @Test
    public void parseActivationFailureReason() {
        Assertions.assertEquals(
            CDataLicenseStatus.INVALID_KEY,
            CDataLicenseParser.parseActivationFailure(
                "Error validating user input: Invalid product key [code: C nodeid: R4P8GJ6B]."
            )
        );
        Assertions.assertEquals(
            CDataLicenseStatus.MACHINE_MISMATCH,
            CDataLicenseParser.parseActivationFailure("This license belongs to a different machine.")
        );
        Assertions.assertEquals(
            CDataLicenseStatus.VALIDATION_UNAVAILABLE,
            CDataLicenseParser.parseActivationFailure("Unexpected vendor response")
        );
    }

    @Test
    public void recognizeExpiredLicenseAtQueryTime() {
        SQLException trialError = new SQLException("The trial license has expired [code: J nodeid: TEST].");
        Assertions.assertEquals(CDataLicenseStatus.TRIAL_EXPIRED, CDataLicenseParser.parseExpiredLicenseError(trialError));
        Assertions.assertEquals(CDataLicenseStatus.EXPIRED,
            CDataLicenseParser.parseExpiredLicenseError(new SQLException("The purchased license has expired.")));
        Assertions.assertEquals(CDataLicenseStatus.EXPIRED,
            CDataLicenseParser.parseExpiredLicenseError(new SQLException("License error [code: J nodeid: TEST].")));

        SQLException wrapper = new SQLException("Query failed");
        wrapper.setNextException(trialError);
        Assertions.assertEquals(CDataLicenseStatus.TRIAL_EXPIRED,
            CDataLicenseParser.parseExpiredLicenseError(new DBException("Execution failed", wrapper)));
    }

    @Test
    public void doNotTreatConnectionOrCredentialErrorsAsLicenseExpiration() {
        for (String message : List.of("Access token expired", "Password has expired", "License server unavailable", "Invalid key")) {
            Assertions.assertNull(CDataLicenseParser.parseExpiredLicenseError(new SQLException(message)));
        }
        SQLException first = new SQLException("Query failed");
        SQLException second = new SQLException("Connection lost");
        first.initCause(second);
        second.initCause(first);
        Assertions.assertNull(CDataLicenseParser.parseExpiredLicenseError(first));
    }

    @Test
    public void requirePurchasedKeyAfterAnyLicenseExpires() throws DBException {
        CDataDriverDescriptor driver = Mockito.mock(CDataDriverDescriptor.class);
        Mockito.when(driver.supportsTrialLicense()).thenCallRealMethod();
        Mockito.when(driver.requestTrialLicense(Mockito.any())).thenCallRealMethod();
        CDataLicenseActivationRequest request = new CDataLicenseActivationRequest(
            "Test User", "test@example.org", CDataLicenseType.TRIAL, null);

        for (CDataLicenseStatus status : List.of(CDataLicenseStatus.TRIAL_EXPIRED, CDataLicenseStatus.EXPIRED)) {
            Mockito.when(driver.getLicenseStatus()).thenReturn(status);
            Assertions.assertFalse(driver.supportsTrialLicense());
            Assertions.assertThrows(DBException.class, () -> driver.requestTrialLicense(new VoidProgressMonitor()));
            Assertions.assertThrows(DBException.class,
                () -> CDataLicenseActivator.activate(new VoidProgressMonitor(), driver, request));
        }
        Mockito.verify(driver, Mockito.never()).beginLicenseActivationProcess();
    }

    @Test
    public void reportRuntimeExpirationOnlyOnce() {
        CDataDriverDescriptor driver = Mockito.mock(CDataDriverDescriptor.class);
        CDataDriverLoaderDescriptor loader = new CDataDriverLoaderDescriptor("default", driver);
        Mockito.when(driver.getDefaultDriverLoader()).thenReturn(loader);
        Mockito.when(driver.reportLicenseError(Mockito.any())).thenCallRealMethod();
        SQLException error = new SQLException("The trial license has expired [code: J nodeid: TEST].");

        Assertions.assertTrue(driver.reportLicenseError(error));
        Assertions.assertFalse(driver.reportLicenseError(error));
        Mockito.when(driver.getLicenseStatus()).thenCallRealMethod();
        Assertions.assertEquals(CDataLicenseStatus.TRIAL_EXPIRED, driver.getLicenseStatus());
    }

    @Test
    public void keepReportedExpirationUntilLicenseFileChanges() throws Exception {
        CDataDriverDescriptor driver = Mockito.mock(CDataDriverDescriptor.class);
        CDataDriverLoaderDescriptor loader = new CDataDriverLoaderDescriptor("default", driver);
        Path licensePath = tempDirectory.resolve("cdata.jdbc.postgresql.lic");
        Files.writeString(licensePath, "expired-license");
        CDataResolvedDriver resolved = new CDataResolvedDriver(
            tempDirectory.resolve("cdata.jdbc.postgresql.jar"), "cdata.jdbc.postgresql.PostgreSQLDriver", licensePath);
        CDataDriverLicense unknown = new CDataDriverLicense(CDataLicenseStatus.VALIDATION_UNAVAILABLE, "", null);
        loader.updateInspectedLicense(resolved, unknown);
        loader.reportExpiredLicense(CDataLicenseStatus.TRIAL_EXPIRED);

        Assertions.assertEquals(CDataLicenseStatus.TRIAL_EXPIRED, loader.updateInspectedLicense(resolved, unknown).getStatus());

        Files.writeString(licensePath, "renewed-license");
        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, loader.updateInspectedLicense(resolved, unknown).getStatus());
    }

    @Test
    public void showOnlyTheReasonCDataGave() {
        // CData answers with its whole console session - prompts, diagnostic codes, support blurb
        String output = "Please enter your name and email address:\nName: Email Address: "
            + "Please enter your Product Key:\n  (you may use \"TRIAL\" as the product key)\n"
            + "Product Key: Error validating user input: Invalid product key [code: B nodeid: 11111111]. "
            + "Contact support@cdata.com for further help.\nPress any key to exit...";

        Assertions.assertEquals("Invalid product key", CDataLicenseParser.parseActivationMessage(output));
        Assertions.assertEquals(CDataLicenseStatus.INVALID_KEY, CDataLicenseParser.parseActivationFailure(output));

        // the reason is still found when CData omits the [code: ...] block
        Assertions.assertEquals(
            "Registration limit reached",
            CDataLicenseParser.parseActivationMessage("Error validating user input: Registration limit reached. Contact support.")
        );
        // nothing recognizable - the caller falls back to the status text
        Assertions.assertNull(CDataLicenseParser.parseActivationMessage("Downloading license data..."));
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
    public void unreadableLicenseStateDoesNotBlockTheDriver() {
        Assertions.assertTrue(CDataLicenseStatus.VALIDATION_UNAVAILABLE.isUnknown());
        Assertions.assertFalse(CDataLicenseStatus.VALIDATION_UNAVAILABLE.isValid());
        Assertions.assertTrue(CDataLicenseStatus.VALIDATION_UNAVAILABLE.allowsDriverUsage());
        Assertions.assertTrue(CDataLicenseStatus.TRIAL_ACTIVE.allowsDriverUsage());
        for (CDataLicenseStatus status : CDataLicenseStatus.values()) {
            Assertions.assertEquals(status.isValid() || status.isUnknown(), status.allowsDriverUsage());
        }
        Assertions.assertFalse(CDataLicenseStatus.TRIAL_EXPIRED.allowsDriverUsage());
        Assertions.assertFalse(CDataLicenseStatus.INVALID_KEY.allowsDriverUsage());
        Assertions.assertFalse(CDataLicenseStatus.NOT_INSTALLED.allowsDriverUsage());
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
            "CData prompt test",
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
    public void resolveDriverClassFromServiceEntry() throws Exception {
        Path jarPath = tempDirectory.resolve("driver.jar");
        try (var output = new JarOutputStream(Files.newOutputStream(jarPath))) {
            output.putNextEntry(new JarEntry("META-INF/services/java.sql.Driver"));
            output.write(("# cdata.jdbc.test.CommentedDriver\n\n  \n" +
                "other.jdbc.Driver\n cdata.jdbc.testing.OtherDriver\n  cdata.jdbc.test.TestDriver  \n")
                .getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        try (var jar = new JarFile(jarPath.toFile())) {
            Assertions.assertEquals("cdata.jdbc.test.TestDriver",
                CDataDriverLoaderDescriptor.readDriverClassName(jar, "cdata.jdbc.test."));
            Assertions.assertNull(CDataDriverLoaderDescriptor.readDriverClassName(jar, "cdata.jdbc.missing."));
        }
    }

    @Test
    public void ignoreJarWithoutDriverService() throws Exception {
        Path jarPath = tempDirectory.resolve("dependency.jar");
        try (var output = new JarOutputStream(Files.newOutputStream(jarPath))) {
            output.putNextEntry(new JarEntry("unrelated-resource"));
            output.closeEntry();
        }
        try (var jar = new JarFile(jarPath.toFile())) {
            Assertions.assertNull(CDataDriverLoaderDescriptor.readDriverClassName(jar, "cdata.jdbc.test."));
        }
    }

    @Test
    public void terminateCanceledActivationProcess() throws Exception {
        Path testBundle = Path.of(CDataPromptProcess.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path pidFile = tempDirectory.resolve("process.pid");
        VoidProgressMonitor monitor = Mockito.mock(VoidProgressMonitor.class);
        Mockito.when(monitor.isCanceled()).thenAnswer(invocation -> Files.isRegularFile(pidFile));

        DBException error = Assertions.assertThrows(DBException.class, () -> CDataProcessExecutor.execute(
            monitor,
            List.of(
                GeneralUtils.findJavaExecutable(), "-cp", testBundle.toString(), CDataPromptProcess.class.getName(), pidFile.toString()
            ),
            tempDirectory,
            "CData cancellation test",
            List.of(new CDataProcessExecutor.PromptResponse("unmatched prompt", ""))
        ));

        Assertions.assertEquals("CData cancellation test was canceled", error.getMessage());
        long pid = Long.parseLong(Files.readString(pidFile));
        Assertions.assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
    }

    @Test
    public void useCanonicalCDataLicensePaths() throws Exception {
        Path storagePath = CDataDriverLoaderDescriptor.getStoragePath();
        Assertions.assertEquals(
            DBWorkbench.getPlatform().getApplication().getGlobalDataPath()
                .resolve(DBConstants.DEFAULT_DRIVERS_FOLDER)
                .resolve("cdata"),
            storagePath
        );
        Path driverPath = CDataDriverLoaderDescriptor.getCanonicalDriverPath("cdata.jdbc.aas", "26");
        Assertions.assertEquals(storagePath.resolve("drivers/cdata.jdbc.aas/26/cdata.jdbc.aas.jar"), driverPath);

        // a CData license covers one major version - they must not share a folder
        Assertions.assertNotEquals(
            driverPath.getParent(),
            CDataDriverLoaderDescriptor.getCanonicalDriverPath("cdata.jdbc.aas", "25").getParent()
        );

        // CData only reads the license next to a JAR named cdata.jdbc.<source>.jar
        Path licensePath = CDataDriverLoaderDescriptor.getLicensePath(driverPath);
        Assertions.assertEquals(driverPath.resolveSibling("cdata.jdbc.aas.lic"), licensePath);
        Assertions.assertEquals("cdata.jdbc.aas.jar", CDataLicenseActivator.getCanonicalJarName(licensePath));

        // the path must not depend on the driver build, otherwise an update loses the license
        Assertions.assertFalse(driverPath.toString().contains("fingerprint"));
        Assertions.assertEquals(
            driverPath,
            CDataDriverLoaderDescriptor.getCanonicalDriverPath("cdata.jdbc.aas", "26")
        );
    }

    @Test
    public void findEveryPersistedMajorLicense() throws Exception {
        String packageName = "cdata.jdbc.postgresql";
        Path packageFolder = tempDirectory.resolve(packageName);
        Path version25 = Files.createDirectories(packageFolder.resolve("25")).resolve(packageName + ".lic");
        Path version26 = Files.createDirectories(packageFolder.resolve("26")).resolve(packageName + ".lic");
        Files.createFile(version25);
        Files.createFile(version26);
        Files.createFile(packageFolder.resolve("26/other.lic"));

        Assertions.assertEquals(
            List.of(version25, version26),
            CDataDriverLoaderDescriptor.findPersistedLicensePaths(packageFolder, packageName)
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
            "CData probe diagnostic test",
            List.of()
        );
        Assertions.assertEquals(2, result.exitCode());
        Assertions.assertTrue(result.output().contains(CDataLicenseProbe.ERROR_PREFIX));
    }

    @Test
    public void releaseActivationFileLockBeforeEnteringLoader() throws Exception {
        Path packageFolder = Files.createDirectories(
            CDataDriverLoaderDescriptor.getStoragePath().resolve("drivers/cdata.jdbc.postgresql"));
        Path majorFolder = Files.createTempDirectory(packageFolder, "26");
        try {
            CDataResolvedDriver resolved = resolvedAt(majorFolder);
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
            manifest.getMainAttributes().putValue("Implementation-Version", majorFolder.getFileName() + ".0");
            manifest.getMainAttributes().putValue("Main-Class", CDataPromptProcess.class.getName());
            String classEntry = CDataPromptProcess.class.getName().replace('.', '/') + ".class";
            try (
                var output = new JarOutputStream(Files.newOutputStream(resolved.jarPath()), manifest);
                var input = CDataPromptProcess.class.getResourceAsStream("/" + classEntry)
            ) {
                output.putNextEntry(new JarEntry(classEntry));
                input.transferTo(output);
                output.closeEntry();
                output.putNextEntry(new JarEntry("META-INF/services/java.sql.Driver"));
                output.write(resolved.driverClassName().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
            Files.writeString(resolved.licensePath(), "previous-license");
            CDataDriverDescriptor driver = Mockito.mock(CDataDriverDescriptor.class);
            Mockito.when(driver.getDriverInfo()).thenReturn(new CDataDriverInfo(
                "postgresql", "postgresql-jdbc", "Test driver", 2026, CDataDriverTier.PROFESSIONAL, "https://example.org"));
            Mockito.when(driver.beginLicenseActivationProcess()).thenReturn(true);
            Mockito.when(driver.getCurrentLicense()).thenCallRealMethod();
            DBPDriverLibrary library = Mockito.mock(DBPDriverLibrary.class);
            Mockito.when(library.matchesCurrentPlatform()).thenReturn(true);
            Mockito.when(library.getType()).thenReturn(DBPDriverLibrary.FileType.jar);
            Mockito.when(library.getLocalFile()).thenReturn(resolved.jarPath());
            Mockito.doReturn(List.of(library)).when(driver).getDriverLibraries();
            CDataDriverLoaderDescriptor loader = new CDataDriverLoaderDescriptor("default", driver);
            VoidProgressMonitor monitor = new VoidProgressMonitor();
            Mockito.when(driver.getDefaultDriverLoader()).thenAnswer(invocation -> {
                try (
                    var channel = FileChannel.open(majorFolder.resolve("cdata.jdbc.postgresql.lic.lock"), StandardOpenOption.WRITE);
                    var lock = channel.tryLock()
                ) {
                    Assertions.assertNotNull(lock, "activation must release the file lock before entering the loader");
                }
                return loader;
            });

            CDataDriverLicense license = CDataLicenseActivator.activate(monitor, driver, resolved,
                new CDataLicenseActivationRequest("Test User", "test@example.org", CDataLicenseType.PURCHASED, "test-key"));
            Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, license.getStatus());
            Mockito.verify(driver).getDefaultDriverLoader();
            Mockito.verify(driver).invalidateLicenseCaches();
            Assertions.assertSame(license, driver.getCurrentLicense());
            Mockito.verify(driver).endLicenseActivationProcess();
            Assertions.assertEquals("new-license", Files.readString(resolved.licensePath()));
        } finally {
            IOUtils.deleteDirectory(majorFolder);
        }
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
                () -> new CDataDriverLicense(CDataLicenseStatus.MACHINE_MISMATCH, "", null)
            )
        );

        Assertions.assertFalse(Files.exists(target));
    }

    @Test
    public void keepInstalledLicenseWhenValidationIsUnavailable() throws Exception {
        Path target = tempDirectory.resolve("cdata.jdbc.test.lic");
        Path staged = tempDirectory.resolve("staged.lic");
        Files.writeString(staged, "new-license");

        CDataDriverLicense installed = CDataLicenseActivator.installAndValidateLicense(
            staged,
            target,
            tempDirectory.resolve("backup.lic"),
            () -> new CDataDriverLicense(CDataLicenseStatus.VALIDATION_UNAVAILABLE, "", null)
        );

        Assertions.assertEquals(CDataLicenseStatus.VALIDATION_UNAVAILABLE, installed.getStatus());
        Assertions.assertEquals("new-license", Files.readString(target));
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

    @Test
    public void detectLicenseIssuedForAnotherMajorVersion() throws Exception {
        Path packageFolder = tempDirectory.resolve("cdata.jdbc.postgresql");
        Path v25 = Files.createDirectories(packageFolder.resolve("25"));
        Path v26 = Files.createDirectories(packageFolder.resolve("26"));
        CDataResolvedDriver on25 = resolvedAt(v25);
        CDataResolvedDriver on26 = resolvedAt(v26);

        // nothing activated anywhere
        Assertions.assertFalse(CDataDriverLoaderDescriptor.hasLicenseForAnotherMajor(on25));

        // the user owns a license for 2026 and switches the driver to 2025
        Files.writeString(on26.licensePath(), "license-for-2026");
        Assertions.assertTrue(CDataDriverLoaderDescriptor.hasLicenseForAnotherMajor(on25));

        // its own license does not count as "another major"
        Assertions.assertFalse(CDataDriverLoaderDescriptor.hasLicenseForAnotherMajor(on26));

        // activating 2025 must not touch the 2026 license
        Files.writeString(on25.licensePath(), "license-for-2025");
        Assertions.assertEquals("license-for-2026", Files.readString(on26.licensePath()));
    }

    @Test
    public void activationEntryPointsMustNotHoldTheLoaderMonitor() throws Exception {
        // The activation dialog blocks the UI thread while the activation itself re-enters the
        // loader from another thread. Holding this loader's monitor across it deadlocks the app.
        for (String name : new String[]{"loadDriver", "getDriverInstance"}) {
            for (Method method : CDataDriverLoaderDescriptor.class.getDeclaredMethods()) {
                if (method.getName().equals(name)) {
                    Assertions.assertFalse(
                        Modifier.isSynchronized(method.getModifiers()),
                        method + " must not be synchronized: it can show the activation dialog"
                    );
                }
            }
        }
    }

    @Test
    public void doNotDiscardALicenseCDataRefusesToRecognize() throws Exception {
        // A purchased license is bound to a registered calling class, so the external probe reports
        // it as "No License". Discarding it on that basis burns a paid activation.
        Path folder = Files.createDirectories(tempDirectory.resolve("cdata.jdbc.gmail/26"));
        CDataResolvedDriver resolved = resolvedAt(folder);

        Assertions.assertEquals(
            CDataLicenseStatus.NOT_INSTALLED,
            keep(CDataLicenseStatus.NOT_INSTALLED, resolved),
            "no license file - nothing is installed"
        );

        Files.writeString(resolved.licensePath(), "purchased-license");
        Assertions.assertEquals(
            CDataLicenseStatus.VALIDATION_UNAVAILABLE,
            keep(CDataLicenseStatus.NOT_INSTALLED, resolved),
            "the file is there - the state is unknown, not absent"
        );
        Assertions.assertTrue(keep(CDataLicenseStatus.NOT_INSTALLED, resolved).allowsDriverUsage());

        // a license CData does recognize as bad still blocks
        for (CDataLicenseStatus bad : new CDataLicenseStatus[]{
            CDataLicenseStatus.EXPIRED, CDataLicenseStatus.TRIAL_EXPIRED, CDataLicenseStatus.MACHINE_MISMATCH}) {
            Assertions.assertEquals(bad, keep(bad, resolved));
            Assertions.assertFalse(bad.allowsDriverUsage());
        }
        Assertions.assertEquals(CDataLicenseStatus.TRIAL_ACTIVE, keep(CDataLicenseStatus.TRIAL_ACTIVE, resolved));
    }

    @NotNull
    private static CDataLicenseStatus keep(
        @NotNull CDataLicenseStatus status,
        @NotNull CDataResolvedDriver resolved
    ) {
        return CDataLicenseValidator.keepInstalledLicense(
            new CDataDriverLicense(status, "", null), resolved).getStatus();
    }

    @NotNull
    private static CDataResolvedDriver resolvedAt(@NotNull Path majorFolder) {
        Path jar = majorFolder.resolve("cdata.jdbc.postgresql.jar");
        return new CDataResolvedDriver(
            jar,
            "cdata.jdbc.postgresql.PostgreSQLDriver",
            CDataDriverLoaderDescriptor.getLicensePath(jar)
        );
    }

    @NotNull
    private static CDataDriverLicense parseInformation(@NotNull String license) {
        return CDataLicenseParser.parseInformation(Map.of("License", license, "NodeId", "test-node"));
    }
}
