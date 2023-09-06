
package org.jkiss.dbeaver.ext.yashandb.debug.internal.impl;

import org.jkiss.dbeaver.debug.DBGObjectDescriptor;

import java.util.Map;

@SuppressWarnings("nls")
public class YashanDBDebugObjectDescriptor implements DBGObjectDescriptor {

    private final Integer oid;

    private final String oname;

    private final String owner;

    private  int subprogramId;

    private  int version;

    private final String datSourceId;

    private final String opath;


    public YashanDBDebugObjectDescriptor(Integer oid, String oname,String datSourceId, String owner, String opath) {
        super();
        this.oid = oid;
        this.oname=oname;
        this.datSourceId=datSourceId;
        this.opath=opath;
        this.owner=owner;
    }

    public String getSchemaOname(){
        return this.owner+"."+this.oname;
    }

    @Override
    public Object getID() {
        return oid;
    }

    @Override
    public String getName() {
        return oname;
    }

    @Override
    public Map<String, Object> toMap() {
        return null;
    }

    public Integer getOid() {
        return oid;
    }

    public String getOname() {
        return oname;
    }

    public String getOwner() {
        return owner;
    }

    public String getDatSourceId() {
        return datSourceId;
    }

    public String getOpath() {
        return opath;
    }

    public int getSubprogramId() {
        return subprogramId;
    }

    public void setSubprogramId(int subprogramId) {
        this.subprogramId = subprogramId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    
}
