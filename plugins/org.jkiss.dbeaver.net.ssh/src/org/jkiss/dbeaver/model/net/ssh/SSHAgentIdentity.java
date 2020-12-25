package org.jkiss.dbeaver.model.net.ssh;

public class SSHAgentIdentity {
    private byte [] blob;
    private byte [] comment;
    public byte[] getBlob() {
        return blob;
    }
    public void setBlob(byte[] blob) {
        this.blob = blob;
    }
    public byte[] getComment() {
        return comment;
    }
    public void setComment(byte[] comment) {
        this.comment = comment;
    }


}
