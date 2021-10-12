/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import com.jcraft.jsch.Buffer;
import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSchException;

import java.util.List;
import java.util.Vector;

public class DBeaverIdentityRepository implements IdentityRepository {
    
    private SSHImplementationAbstract impl;
    private List<SSHAgentIdentity> identities;

    public DBeaverIdentityRepository(SSHImplementationAbstract impl, List<SSHAgentIdentity> identities) {
        this.impl = impl;
        this.identities = identities;
    }

    @Override
    public Vector<?> getIdentities() {
        Vector<com.jcraft.jsch.Identity> result = new Vector<com.jcraft.jsch.Identity>(); 

        for (SSHAgentIdentity identity : identities) {
            byte [] blob = identity.getBlob();
            byte [] comment = identity.getComment();
            com.jcraft.jsch.Identity id = new com.jcraft.jsch.Identity() {
                String algname = new String((new Buffer(blob)).getString());
                public boolean setPassphrase(byte[] passphrase) throws JSchException {
                	return true;
                }
                public byte[] getPublicKeyBlob() { return blob; }
                public byte[] getSignature(byte[] data){
                	return impl.agentSign(blob, data);
                }
                public boolean decrypt() { return true; }
                public String getAlgName() { return algname; }
                public String getName() { return new String(comment); }
                public boolean isEncrypted() { return false; }
                public void clear() { /* NO NEED TO IMPLEMENT */ }
            };
            result.addElement(id);
        }

        return result;
    }

    @Override
    public boolean add(byte[] identity) {
        return false;
    }

    @Override
    public boolean remove(byte[] blob) {
        return false;
    }

    @Override
    public void removeAll() {
        /* NO NEED TO IMPLEMENT */
    }

    @Override
    public String getName() {
        return "DBeaver Identity Repository for jsch";
    }

    @Override
    public int getStatus() {
        return RUNNING;
    }
}
