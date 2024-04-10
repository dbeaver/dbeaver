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
package org.jkiss.dbeaver.model.net.ssh;

import com.jcraft.jsch.Identity;
import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.common.Message;
import net.schmizz.sshj.common.SSHPacket;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.method.AbstractAuthMethod;
import org.jkiss.code.NotNull;

public class DBeaverAuthAgent extends AbstractAuthMethod {
    private final Identity identity;

    public DBeaverAuthAgent(@NotNull Identity identity) throws Buffer.BufferException {
        super("publickey");
        this.identity = identity;
    }

    @Override
    public void handle(Message cmd, SSHPacket buf) throws UserAuthException, TransportException {
        if (cmd == Message.USERAUTH_60) {
            sendSignedReq();
        } else {
            super.handle(cmd, buf);
        }
    }

    @Override
    protected SSHPacket buildReq() throws UserAuthException {
        return buildReq(false);
    }

    @NotNull
    private SSHPacket buildReq(boolean signed) throws UserAuthException {
        return putPubKey(super.buildReq().putBoolean(signed));
    }

    @NotNull
    private SSHPacket putPubKey(SSHPacket reqBuf) {
        reqBuf
            .putString(getAlgName())
            .putBytes(identity.getPublicKeyBlob());
        return reqBuf;
    }

    @NotNull
    private SSHPacket putSig(SSHPacket reqBuf) {
        final byte[] dataToSign = new Buffer.PlainBuffer()
            .putString(params.getTransport().getSessionID())
            .putBuffer(reqBuf) // & rest of the data for sig
            .getCompactData();

        reqBuf.putBytes(identity.getSignature(dataToSign, getAlgName()));

        return reqBuf;
    }

    private void sendSignedReq() throws UserAuthException, TransportException {
        params.getTransport().write(putSig(buildReq(true)));
    }

    @NotNull
    private String getAlgName() {
        final String name = identity.getAlgName();
        if ("ssh-rsa".equals(name)) {
            return "rsa-sha2-512";
        } else {
            return name;
        }
    }
}
