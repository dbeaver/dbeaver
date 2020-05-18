package org.jkiss.dbeaver.model.net.ssh;

import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.common.Message;
import net.schmizz.sshj.common.SSHPacket;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.method.AbstractAuthMethod;

public class DBeaverAuthAgent extends AbstractAuthMethod {

    private final SSHAgentIdentity identity;
    private final String algorithm;
    private final SSHImplementationAbstract impl;

    public DBeaverAuthAgent(SSHImplementationAbstract impl, SSHAgentIdentity identity) throws Buffer.BufferException {
        super("publickey");
        this.identity = identity;
        this.algorithm = (new Buffer.PlainBuffer(identity.getBlob())).readString();
        this.impl = impl;
    }

    /** Internal use. */
    @Override
    public void handle(Message cmd, SSHPacket buf) throws UserAuthException, TransportException {
        if (cmd == Message.USERAUTH_60)
            sendSignedReq();
        else
            super.handle(cmd, buf);
    }

    protected SSHPacket putPubKey(SSHPacket reqBuf) throws UserAuthException {
        reqBuf
            .putString(algorithm)
            .putBytes(identity.getBlob()).getCompactData();
        return reqBuf;
    }

    private SSHPacket putSig(SSHPacket reqBuf) throws UserAuthException {
        final byte[] dataToSign = new Buffer.PlainBuffer()
                .putString(params.getTransport().getSessionID())
                .putBuffer(reqBuf) // & rest of the data for sig
                .getCompactData();

        reqBuf.putBytes(impl.agentSign(identity.getBlob(), dataToSign));

        return reqBuf;
    }

    private void sendSignedReq() throws UserAuthException, TransportException {
        params.getTransport().write(putSig(buildReq(true)));
    }

    private SSHPacket buildReq(boolean signed) throws UserAuthException {
        return putPubKey(super.buildReq().putBoolean(signed));
    }

    @Override
    protected SSHPacket buildReq() throws UserAuthException {
        return buildReq(false);
    }
}
