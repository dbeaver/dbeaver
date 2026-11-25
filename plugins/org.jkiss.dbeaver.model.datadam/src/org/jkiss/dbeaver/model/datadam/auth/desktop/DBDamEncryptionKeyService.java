/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.datadam.auth.desktop;

import com.dbeaver.datadam.dam.remote.api.model.DDGatewayKeyMaterial;
import com.dbeaver.datadam.dam.remote.api.model.DDGatewayKeyRegistrationStatus;
import com.dbeaver.datadam.dam.remote.api.service.web.DDGatewayKeyService;
import com.dbeaver.datadam.sso.api.DDPermissionIds;
import com.dbeaver.messaging.rpc.MessagingRpcException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.DDAccountClient;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.desktop.keys.DDGatewayKeyMaterialGenerator;
import org.jkiss.dbeaver.model.datadam.auth.desktop.keys.DDRecoveryPhraseGenerator;
import org.jkiss.dbeaver.model.datadam.rpc.DBDamSessionService;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.net.URI;
import java.util.UUID;

/** Desktop entry point for account encryption-key operations. Call its remote checks outside the UI thread. */
public final class DBDamEncryptionKeyService {
    public static final int GENERATION_WORK_UNITS = 4;

    private static final Log log = Log.getLog(DBDamEncryptionKeyService.class);
    private static DBDamEncryptionKeyService instance;

    private final DDAccountClient account;
    private final DBDamAccessService access;
    private final DDRecoveryPhraseGenerator phraseGenerator;
    private final DDGatewayKeyMaterialGenerator materialGenerator;
    DBDamEncryptionKeyService(
        @NotNull DDAccountClient account,
        @NotNull DBDamAccessService access,
        @NotNull DDRecoveryPhraseGenerator phraseGenerator,
        @NotNull DDGatewayKeyMaterialGenerator materialGenerator
    ) {
        this.account = account;
        this.access = access;
        this.phraseGenerator = phraseGenerator;
        this.materialGenerator = materialGenerator;
    }

    @NotNull
    public static synchronized DBDamEncryptionKeyService getInstance() {
        if (instance == null) {
            DBDamAccessService access = DBDamAccessService.getInstance();
            instance = new DBDamEncryptionKeyService(
                new DDAccountClient(URI.create(DDEndpoints.getAccountBaseUrl()), access), access,
                new DDRecoveryPhraseGenerator(), new DDGatewayKeyMaterialGenerator());
        }
        return instance;
    }

    /** Checks the account's encrypted bundle, not whether this installation has imported the keys. */
    public boolean isKeysPresent() throws DBException {
        try {
            return getGatewayKeyService().isKeysPresent();
        } catch (MessagingRpcException e) {
            throw new DBException("Could not read DataDam encryption key status", e);
        }
    }

    /** Checks the current account permission for server-side key management. */
    public boolean canManageKeys() throws DBException {
        return account.hasPermission(DDPermissionIds.STORAGE_KEY_MANAGE);
    }

    /**
     * Registers new keys and returns the phrase only after registration or recovery is confirmed.
     * The caller owns the monitor's task lifecycle, with {@link #GENERATION_WORK_UNITS} work units.
     * Cancellation is checked only before registration; any result after that must still reach the UI.
     */
    @NotNull
    public String generateNewKey(@NotNull DBRProgressMonitor monitor) throws DBException, InterruptedException {
        requireNotCanceled(monitor);
        UUID accountId = prepareAccountForGeneration(monitor);
        log.debug("Starting DataDam encryption key generation");
        String phrase = generatePhrase(monitor);
        DDGatewayKeyMaterial material = generateMaterial(phrase, monitor);
        DDGatewayKeyRegistrationStatus status;
        try {
            status = getGatewayKeyService().registerKey(material);
            if (status == null) {
                throw new DBException("Invalid DataDam encryption key registration response");
            }
        } catch (DBException | MessagingRpcException registrationFailure) {
            String verified = verifyAfterRegistrationFailure(accountId, phrase, registrationFailure);
            monitor.worked(1);
            return verified;
        }

        monitor.worked(1);
        return switch (status) {
            case REGISTERED -> phrase;
            case ALREADY_EXISTS -> throw new DBException("DataDam encryption keys already exist for this account");
            case INVALID_MATERIAL -> throw new DBException("Invalid DataDam encryption key material");
            case NOT_ALLOWED -> throw new DBException("DataDam account cannot generate encryption keys");
        };
    }

    @NotNull
    private UUID prepareAccountForGeneration(@NotNull DBRProgressMonitor monitor) throws DBException, InterruptedException {
        UUID accountId = account.getAccountId();
        if (!canManageKeys()) {
            throw new DBException("DataDam account cannot generate encryption keys");
        }
        if (isKeysPresent()) {
            throw new DBException("DataDam encryption keys already exist for this account");
        }
        monitor.worked(1);
        requireNotCanceled(monitor);
        return accountId;
    }

    @NotNull
    private String generatePhrase(@NotNull DBRProgressMonitor monitor) throws DBException, InterruptedException {
        String phrase = phraseGenerator.generate();
        monitor.worked(1);
        requireNotCanceled(monitor);
        return phrase;
    }

    @NotNull
    private DDGatewayKeyMaterial generateMaterial(
        @NotNull String phrase, @NotNull DBRProgressMonitor monitor
    ) throws DBException, InterruptedException {
        DDGatewayKeyMaterial material = materialGenerator.generate(phrase);
        monitor.worked(1);
        requireNotCanceled(monitor);
        return material;
    }

    @NotNull
    private DDGatewayKeyService getGatewayKeyService() throws DBException {
        return DBDamSessionService.getInstance().getRpc()
            .proxy(DDGatewayKeyService.SERVICE_ID, DDGatewayKeyService.class);
    }

    private void requireNotCanceled(@NotNull DBRProgressMonitor monitor) throws InterruptedException {
        if (monitor.isCanceled()) {
            throw new InterruptedException("DataDam encryption key generation was canceled");
        }
    }

    @NotNull
    private String verifyAfterRegistrationFailure(
        @NotNull UUID accountId, @NotNull String phrase, @NotNull Exception registrationFailure
    ) throws DBException {
        try {
            DDKeyBundle verified = access.prepareKeyImport(phrase);
            if (!accountId.equals(verified.accountId())) {
                throw new DBException("DataDam account changed during encryption key generation");
            }
            log.info("DataDam encryption keys verified despite a registration error");
            return phrase;
        } catch (DBException verificationFailure) {
            registrationFailure.addSuppressed(verificationFailure);
            if (registrationFailure instanceof DBException dbException) {
                throw dbException;
            }
            throw new DBException("Could not register DataDam encryption keys", registrationFailure);
        }
    }
}
