
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2012;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 			Information on how parallel threads are reserved on NUMA node
 * 			NodeId: ID of NUMA node where this query is chosen to run
 * 			ReservedThreads: number of reserved parallel thread on this NUMA node
 * 			
 * 
 * <p>Java class for ThreadReservationType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ThreadReservationType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="NodeId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="ReservedThreads" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ThreadReservationType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class ThreadReservationType_sql2012 {

    @XmlAttribute(name = "NodeId", required = true)
    protected int nodeId;
    @XmlAttribute(name = "ReservedThreads", required = true)
    protected int reservedThreads;

    /**
     * Gets the value of the nodeId property.
     * 
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * Sets the value of the nodeId property.
     * 
     */
    public void setNodeId(int value) {
        this.nodeId = value;
    }

    /**
     * Gets the value of the reservedThreads property.
     * 
     */
    public int getReservedThreads() {
        return reservedThreads;
    }

    /**
     * Sets the value of the reservedThreads property.
     * 
     */
    public void setReservedThreads(int value) {
        this.reservedThreads = value;
    }

}
