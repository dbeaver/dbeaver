
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014sp2;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for UpdateType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="UpdateType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RowsetType">
 *       &lt;sequence>
 *         &lt;element name="SetPredicate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SetPredicateElementType" maxOccurs="2" minOccurs="0"/>
 *         &lt;element name="ProbeColumn" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SingleColumnReferenceType" minOccurs="0"/>
 *         &lt;element name="ActionColumn" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SingleColumnReferenceType" minOccurs="0"/>
 *         &lt;element name="OriginalActionColumn" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SingleColumnReferenceType" minOccurs="0"/>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="WithOrderedPrefetch" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="WithUnorderedPrefetch" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="DMLRequestSort" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UpdateType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "setPredicate",
    "probeColumn",
    "actionColumn",
    "originalActionColumn",
    "relOp"
})
public class UpdateType_sql2014sp2
    extends RowsetType_sql2014sp2
{

    @XmlElement(name = "SetPredicate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected List<SetPredicateElementType_sql2014sp2> setPredicate;
    @XmlElement(name = "ProbeColumn", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SingleColumnReferenceType_sql2014sp2 probeColumn;
    @XmlElement(name = "ActionColumn", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SingleColumnReferenceType_sql2014sp2 actionColumn;
    @XmlElement(name = "OriginalActionColumn", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SingleColumnReferenceType_sql2014sp2 originalActionColumn;
    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected RelOpType_sql2014sp2 relOp;
    @XmlAttribute(name = "WithOrderedPrefetch")
    protected Boolean withOrderedPrefetch;
    @XmlAttribute(name = "WithUnorderedPrefetch")
    protected Boolean withUnorderedPrefetch;
    @XmlAttribute(name = "DMLRequestSort")
    protected Boolean dmlRequestSort;

    /**
     * Gets the value of the setPredicate property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the setPredicate property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSetPredicate().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SetPredicateElementType_sql2014sp2 }
     * 
     * 
     */
    public List<SetPredicateElementType_sql2014sp2> getSetPredicate() {
        if (setPredicate == null) {
            setPredicate = new ArrayList<SetPredicateElementType_sql2014sp2>();
        }
        return this.setPredicate;
    }

    /**
     * Gets the value of the probeColumn property.
     * 
     * @return
     *     possible object is
     *     {@link SingleColumnReferenceType_sql2014sp2 }
     *     
     */
    public SingleColumnReferenceType_sql2014sp2 getProbeColumn() {
        return probeColumn;
    }

    /**
     * Sets the value of the probeColumn property.
     * 
     * @param value
     *     allowed object is
     *     {@link SingleColumnReferenceType_sql2014sp2 }
     *     
     */
    public void setProbeColumn(SingleColumnReferenceType_sql2014sp2 value) {
        this.probeColumn = value;
    }

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
     * Gets the value of the originalActionColumn property.
     * 
     * @return
     *     possible object is
     *     {@link SingleColumnReferenceType_sql2014sp2 }
     *     
     */
    public SingleColumnReferenceType_sql2014sp2 getOriginalActionColumn() {
        return originalActionColumn;
    }

    /**
     * Sets the value of the originalActionColumn property.
     * 
     * @param value
     *     allowed object is
     *     {@link SingleColumnReferenceType_sql2014sp2 }
     *     
     */
    public void setOriginalActionColumn(SingleColumnReferenceType_sql2014sp2 value) {
        this.originalActionColumn = value;
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

    /**
     * Gets the value of the withOrderedPrefetch property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getWithOrderedPrefetch() {
        return withOrderedPrefetch;
    }

    /**
     * Sets the value of the withOrderedPrefetch property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setWithOrderedPrefetch(Boolean value) {
        this.withOrderedPrefetch = value;
    }

    /**
     * Gets the value of the withUnorderedPrefetch property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getWithUnorderedPrefetch() {
        return withUnorderedPrefetch;
    }

    /**
     * Sets the value of the withUnorderedPrefetch property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setWithUnorderedPrefetch(Boolean value) {
        this.withUnorderedPrefetch = value;
    }

    /**
     * Gets the value of the dmlRequestSort property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getDMLRequestSort() {
        return dmlRequestSort;
    }

    /**
     * Sets the value of the dmlRequestSort property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDMLRequestSort(Boolean value) {
        this.dmlRequestSort = value;
    }

}
