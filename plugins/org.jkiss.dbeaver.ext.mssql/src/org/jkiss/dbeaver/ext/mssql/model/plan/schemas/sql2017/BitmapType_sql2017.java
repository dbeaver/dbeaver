
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for BitmapType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="BitmapType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="HashKeys" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType"/>
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
@XmlType(name = "BitmapType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "hashKeys",
    "relOp"
})
public class BitmapType_sql2017
    extends RelOpBaseType_sql2017
{

    @XmlElement(name = "HashKeys", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected ColumnReferenceListType_sql2017 hashKeys;
    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected RelOpType_sql2017 relOp;

    /**
     * Gets the value of the hashKeys property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2017 }
     *     
     */
    public ColumnReferenceListType_sql2017 getHashKeys() {
        return hashKeys;
    }

    /**
     * Sets the value of the hashKeys property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2017 }
     *     
     */
    public void setHashKeys(ColumnReferenceListType_sql2017 value) {
        this.hashKeys = value;
    }

    /**
     * Gets the value of the relOp property.
     * 
     * @return
     *     possible object is
     *     {@link RelOpType_sql2017 }
     *     
     */
    public RelOpType_sql2017 getRelOp() {
        return relOp;
    }

    /**
     * Sets the value of the relOp property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelOpType_sql2017 }
     *     
     */
    public void setRelOp(RelOpType_sql2017 value) {
        this.relOp = value;
    }

}
