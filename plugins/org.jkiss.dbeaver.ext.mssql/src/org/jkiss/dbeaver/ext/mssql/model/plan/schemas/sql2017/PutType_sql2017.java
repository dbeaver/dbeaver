
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PutType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PutType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RemoteQueryType">
 *       &lt;sequence>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="ShuffleType" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="ShuffleColumn" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PutType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "relOp"
})
public class PutType_sql2017
    extends RemoteQueryType_sql2017
{

    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected RelOpType_sql2017 relOp;
    @XmlAttribute(name = "ShuffleType")
    protected String shuffleType;
    @XmlAttribute(name = "ShuffleColumn")
    protected String shuffleColumn;

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

    /**
     * Gets the value of the shuffleType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getShuffleType() {
        return shuffleType;
    }

    /**
     * Sets the value of the shuffleType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setShuffleType(String value) {
        this.shuffleType = value;
    }

    /**
     * Gets the value of the shuffleColumn property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getShuffleColumn() {
        return shuffleColumn;
    }

    /**
     * Sets the value of the shuffleColumn property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setShuffleColumn(String value) {
        this.shuffleColumn = value;
    }

}
