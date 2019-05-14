
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ForeignKeyReferencesCheckType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ForeignKeyReferencesCheckType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType"/>
 *         &lt;element name="ForeignKeyReferenceCheck" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ForeignKeyReferenceCheckType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="ForeignKeyReferencesCount" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="NoMatchingIndexCount" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="PartialMatchingIndexCount" type="{http://www.w3.org/2001/XMLSchema}int" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ForeignKeyReferencesCheckType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "relOp",
    "foreignKeyReferenceCheck"
})
public class ForeignKeyReferencesCheckType_sql2017
    extends RelOpBaseType_sql2017
{

    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected RelOpType_sql2017 relOp;
    @XmlElement(name = "ForeignKeyReferenceCheck", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<ForeignKeyReferenceCheckType_sql2017> foreignKeyReferenceCheck;
    @XmlAttribute(name = "ForeignKeyReferencesCount")
    protected Integer foreignKeyReferencesCount;
    @XmlAttribute(name = "NoMatchingIndexCount")
    protected Integer noMatchingIndexCount;
    @XmlAttribute(name = "PartialMatchingIndexCount")
    protected Integer partialMatchingIndexCount;

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
     * Gets the value of the foreignKeyReferenceCheck property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the foreignKeyReferenceCheck property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getForeignKeyReferenceCheck().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ForeignKeyReferenceCheckType_sql2017 }
     * 
     * 
     */
    public List<ForeignKeyReferenceCheckType_sql2017> getForeignKeyReferenceCheck() {
        if (foreignKeyReferenceCheck == null) {
            foreignKeyReferenceCheck = new ArrayList<ForeignKeyReferenceCheckType_sql2017>();
        }
        return this.foreignKeyReferenceCheck;
    }

    /**
     * Gets the value of the foreignKeyReferencesCount property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getForeignKeyReferencesCount() {
        return foreignKeyReferencesCount;
    }

    /**
     * Sets the value of the foreignKeyReferencesCount property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setForeignKeyReferencesCount(Integer value) {
        this.foreignKeyReferencesCount = value;
    }

    /**
     * Gets the value of the noMatchingIndexCount property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getNoMatchingIndexCount() {
        return noMatchingIndexCount;
    }

    /**
     * Sets the value of the noMatchingIndexCount property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setNoMatchingIndexCount(Integer value) {
        this.noMatchingIndexCount = value;
    }

    /**
     * Gets the value of the partialMatchingIndexCount property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getPartialMatchingIndexCount() {
        return partialMatchingIndexCount;
    }

    /**
     * Sets the value of the partialMatchingIndexCount property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setPartialMatchingIndexCount(Integer value) {
        this.partialMatchingIndexCount = value;
    }

}
