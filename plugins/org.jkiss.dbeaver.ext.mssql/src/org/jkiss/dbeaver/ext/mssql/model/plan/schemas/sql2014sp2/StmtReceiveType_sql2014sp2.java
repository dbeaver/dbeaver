
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014sp2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * The cursor type that might have one or more cursor operations, used in DECLARE CURSOR, OPEN CURSOR and FETCH CURSOR
 * 
 * <p>Java class for StmtReceiveType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StmtReceiveType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}BaseStmtInfoType">
 *       &lt;sequence>
 *         &lt;element name="ReceivePlan" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ReceivePlanType"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StmtReceiveType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "receivePlan"
})
public class StmtReceiveType_sql2014sp2
    extends BaseStmtInfoType_sql2014sp2
{

    @XmlElement(name = "ReceivePlan", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected ReceivePlanType_sql2014sp2 receivePlan;

    /**
     * Gets the value of the receivePlan property.
     * 
     * @return
     *     possible object is
     *     {@link ReceivePlanType_sql2014sp2 }
     *     
     */
    public ReceivePlanType_sql2014sp2 getReceivePlan() {
        return receivePlan;
    }

    /**
     * Sets the value of the receivePlan property.
     * 
     * @param value
     *     allowed object is
     *     {@link ReceivePlanType_sql2014sp2 }
     *     
     */
    public void setReceivePlan(ReceivePlanType_sql2014sp2 value) {
        this.receivePlan = value;
    }

}
