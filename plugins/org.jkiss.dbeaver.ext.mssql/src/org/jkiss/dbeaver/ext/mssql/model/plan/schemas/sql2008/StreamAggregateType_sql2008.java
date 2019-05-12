
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2008;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for StreamAggregateType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StreamAggregateType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="GroupBy" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *         &lt;element name="RollupInfo" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RollupInfoType" minOccurs="0"/>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StreamAggregateType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "groupBy",
    "rollupInfo",
    "relOp"
})
public class StreamAggregateType_sql2008
    extends RelOpBaseType_sql2008
{

    @XmlElement(name = "GroupBy", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceListType_sql2008 groupBy;
    @XmlElement(name = "RollupInfo", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected RollupInfoType_sql2008 rollupInfo;
    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected RelOpType_sql2008 relOp;

    /**
     * Gets the value of the groupBy property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2008 }
     *     
     */
    public ColumnReferenceListType_sql2008 getGroupBy() {
        return groupBy;
    }

    /**
     * Sets the value of the groupBy property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2008 }
     *     
     */
    public void setGroupBy(ColumnReferenceListType_sql2008 value) {
        this.groupBy = value;
    }

    /**
     * Gets the value of the rollupInfo property.
     * 
     * @return
     *     possible object is
     *     {@link RollupInfoType_sql2008 }
     *     
     */
    public RollupInfoType_sql2008 getRollupInfo() {
        return rollupInfo;
    }

    /**
     * Sets the value of the rollupInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link RollupInfoType_sql2008 }
     *     
     */
    public void setRollupInfo(RollupInfoType_sql2008 value) {
        this.rollupInfo = value;
    }

    /**
     * Gets the value of the relOp property.
     * 
     * @return
     *     possible object is
     *     {@link RelOpType_sql2008 }
     *     
     */
    public RelOpType_sql2008 getRelOp() {
        return relOp;
    }

    /**
     * Sets the value of the relOp property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelOpType_sql2008 }
     *     
     */
    public void setRelOp(RelOpType_sql2008 value) {
        this.relOp = value;
    }

}
