
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014sp2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SplitType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SplitType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="ActionColumn" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SingleColumnReferenceType" minOccurs="0"/>
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
@XmlType(name = "SplitType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "actionColumn",
    "relOp"
})
public class SplitType_sql2014sp2
    extends RelOpBaseType_sql2014sp2
{

    @XmlElement(name = "ActionColumn", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SingleColumnReferenceType_sql2014sp2 actionColumn;
    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected RelOpType_sql2014sp2 relOp;

    /**
     * Gets the value of the actionColumn property.
     * 
     * @return
     *     possible object is
     *     {@link SingleColumnReferenceType_sql2014sp2 }
     *     
     */
    public SingleColumnReferenceType_sql2014sp2 getActionColumn() {
        return actionColumn;
    }

    /**
     * Sets the value of the actionColumn property.
     * 
     * @param value
     *     allowed object is
     *     {@link SingleColumnReferenceType_sql2014sp2 }
     *     
     */
    public void setActionColumn(SingleColumnReferenceType_sql2014sp2 value) {
        this.actionColumn = value;
    }

    /**
     * Gets the value of the relOp property.
     * 
     * @return
     *     possible object is
     *     {@link RelOpType_sql2014sp2 }
     *     
     */
    public RelOpType_sql2014sp2 getRelOp() {
        return relOp;
    }

    /**
     * Sets the value of the relOp property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelOpType_sql2014sp2 }
     *     
     */
    public void setRelOp(RelOpType_sql2014sp2 value) {
        this.relOp = value;
    }

}
