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
