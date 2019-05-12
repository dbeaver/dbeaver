
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005sp2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SpoolType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SpoolType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="SeekPredicate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SeekPredicateType" minOccurs="0"/>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Stack" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="PrimaryNodeId" type="{http://www.w3.org/2001/XMLSchema}int" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SpoolType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "seekPredicate",
    "relOp"
})
public class SpoolType_sql2005sp2
    extends RelOpBaseType_sql2005sp2
{

    @XmlElement(name = "SeekPredicate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SeekPredicateType_sql2005sp2 seekPredicate;
    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected RelOpType_sql2005sp2 relOp;
    @XmlAttribute(name = "Stack")
    protected Boolean stack;
    @XmlAttribute(name = "PrimaryNodeId")
    protected Integer primaryNodeId;

    /**
     * Gets the value of the seekPredicate property.
     * 
     * @return
     *     possible object is
     *     {@link SeekPredicateType_sql2005sp2 }
     *     
     */
    public SeekPredicateType_sql2005sp2 getSeekPredicate() {
        return seekPredicate;
    }

    /**
     * Sets the value of the seekPredicate property.
     * 
     * @param value
     *     allowed object is
     *     {@link SeekPredicateType_sql2005sp2 }
     *     
     */
    public void setSeekPredicate(SeekPredicateType_sql2005sp2 value) {
        this.seekPredicate = value;
    }

    /**
     * Gets the value of the relOp property.
     * 
     * @return
     *     possible object is
     *     {@link RelOpType_sql2005sp2 }
     *     
     */
    public RelOpType_sql2005sp2 getRelOp() {
        return relOp;
    }

    /**
     * Sets the value of the relOp property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelOpType_sql2005sp2 }
     *     
     */
    public void setRelOp(RelOpType_sql2005sp2 value) {
        this.relOp = value;
    }

    /**
     * Gets the value of the stack property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getStack() {
        return stack;
    }

    /**
     * Sets the value of the stack property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setStack(Boolean value) {
        this.stack = value;
    }

    /**
     * Gets the value of the primaryNodeId property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getPrimaryNodeId() {
        return primaryNodeId;
    }

    /**
     * Sets the value of the primaryNodeId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setPrimaryNodeId(Integer value) {
        this.primaryNodeId = value;
    }

}
