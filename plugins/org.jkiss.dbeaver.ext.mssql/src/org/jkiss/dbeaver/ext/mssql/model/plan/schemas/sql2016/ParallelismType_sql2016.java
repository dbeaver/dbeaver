
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ParallelismType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ParallelismType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="PartitionColumns" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *         &lt;element name="OrderBy" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}OrderByType" minOccurs="0"/>
 *         &lt;element name="HashKeys" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceListType" minOccurs="0"/>
 *         &lt;element name="ProbeColumn" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SingleColumnReferenceType" minOccurs="0"/>
 *         &lt;element name="Predicate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="Activation" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Object" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ObjectType" minOccurs="0"/>
 *                 &lt;/sequence>
 *                 &lt;attribute name="Type" use="required">
 *                   &lt;simpleType>
 *                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                       &lt;enumeration value="CloneLocation"/>
 *                       &lt;enumeration value="Resource"/>
 *                       &lt;enumeration value="SingleBrick"/>
 *                       &lt;enumeration value="Region"/>
 *                     &lt;/restriction>
 *                   &lt;/simpleType>
 *                 &lt;/attribute>
 *                 &lt;attribute name="FragmentElimination" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="BrickRouting" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Object" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ObjectType" minOccurs="0"/>
 *                   &lt;element name="FragmentIdColumn" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SingleColumnReferenceType" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="PartitioningType" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}PartitionType" />
 *       &lt;attribute name="Remoting" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="LocalParallelism" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="InRow" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ParallelismType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "partitionColumns",
    "orderBy",
    "hashKeys",
    "probeColumn",
    "predicate",
    "activation",
    "brickRouting",
    "relOp"
})
public class ParallelismType_sql2016
    extends RelOpBaseType_sql2016
{

    @XmlElement(name = "PartitionColumns", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceListType_sql2016 partitionColumns;
    @XmlElement(name = "OrderBy", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected OrderByType_sql2016 orderBy;
    @XmlElement(name = "HashKeys", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceListType_sql2016 hashKeys;
    @XmlElement(name = "ProbeColumn", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SingleColumnReferenceType_sql2016 probeColumn;
    @XmlElement(name = "Predicate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2016 predicate;
    @XmlElement(name = "Activation", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ParallelismType_sql2016 .Activation_sql2016 activation;
    @XmlElement(name = "BrickRouting", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ParallelismType_sql2016 .BrickRouting_sql2016 brickRouting;
    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected RelOpType_sql2016 relOp;
    @XmlAttribute(name = "PartitioningType")
    protected PartitionType_sql2016 partitioningType;
    @XmlAttribute(name = "Remoting")
    protected Boolean remoting;
    @XmlAttribute(name = "LocalParallelism")
    protected Boolean localParallelism;
    @XmlAttribute(name = "InRow")
    protected Boolean inRow;

    /**
     * Gets the value of the partitionColumns property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2016 }
     *     
     */
    public ColumnReferenceListType_sql2016 getPartitionColumns() {
        return partitionColumns;
    }

    /**
     * Sets the value of the partitionColumns property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2016 }
     *     
     */
    public void setPartitionColumns(ColumnReferenceListType_sql2016 value) {
        this.partitionColumns = value;
    }

    /**
     * Gets the value of the orderBy property.
     * 
     * @return
     *     possible object is
     *     {@link OrderByType_sql2016 }
     *     
     */
    public OrderByType_sql2016 getOrderBy() {
        return orderBy;
    }

    /**
     * Sets the value of the orderBy property.
     * 
     * @param value
     *     allowed object is
     *     {@link OrderByType_sql2016 }
     *     
     */
    public void setOrderBy(OrderByType_sql2016 value) {
        this.orderBy = value;
    }

    /**
     * Gets the value of the hashKeys property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceListType_sql2016 }
     *     
     */
    public ColumnReferenceListType_sql2016 getHashKeys() {
        return hashKeys;
    }

    /**
     * Sets the value of the hashKeys property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceListType_sql2016 }
     *     
     */
    public void setHashKeys(ColumnReferenceListType_sql2016 value) {
        this.hashKeys = value;
    }

    /**
     * Gets the value of the probeColumn property.
     * 
     * @return
     *     possible object is
     *     {@link SingleColumnReferenceType_sql2016 }
     *     
     */
    public SingleColumnReferenceType_sql2016 getProbeColumn() {
        return probeColumn;
    }

    /**
     * Sets the value of the probeColumn property.
     * 
     * @param value
     *     allowed object is
     *     {@link SingleColumnReferenceType_sql2016 }
     *     
     */
    public void setProbeColumn(SingleColumnReferenceType_sql2016 value) {
        this.probeColumn = value;
    }

    /**
     * Gets the value of the predicate property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2016 }
     *     
     */
    public ScalarExpressionType_sql2016 getPredicate() {
        return predicate;
    }

    /**
     * Sets the value of the predicate property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2016 }
     *     
     */
    public void setPredicate(ScalarExpressionType_sql2016 value) {
        this.predicate = value;
    }

    /**
     * Gets the value of the activation property.
     * 
     * @return
     *     possible object is
     *     {@link ParallelismType_sql2016 .Activation_sql2016 }
     *     
     */
    public ParallelismType_sql2016 .Activation_sql2016 getActivation() {
        return activation;
    }

    /**
     * Sets the value of the activation property.
     * 
     * @param value
     *     allowed object is
     *     {@link ParallelismType_sql2016 .Activation_sql2016 }
     *     
     */
    public void setActivation(ParallelismType_sql2016 .Activation_sql2016 value) {
        this.activation = value;
    }

    /**
     * Gets the value of the brickRouting property.
     * 
     * @return
     *     possible object is
     *     {@link ParallelismType_sql2016 .BrickRouting_sql2016 }
     *     
     */
    public ParallelismType_sql2016 .BrickRouting_sql2016 getBrickRouting() {
        return brickRouting;
    }

    /**
     * Sets the value of the brickRouting property.
     * 
     * @param value
     *     allowed object is
     *     {@link ParallelismType_sql2016 .BrickRouting_sql2016 }
     *     
     */
    public void setBrickRouting(ParallelismType_sql2016 .BrickRouting_sql2016 value) {
        this.brickRouting = value;
    }

    /**
     * Gets the value of the relOp property.
     * 
     * @return
     *     possible object is
     *     {@link RelOpType_sql2016 }
     *     
     */
    public RelOpType_sql2016 getRelOp() {
        return relOp;
    }

    /**
     * Sets the value of the relOp property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelOpType_sql2016 }
     *     
     */
    public void setRelOp(RelOpType_sql2016 value) {
        this.relOp = value;
    }

    /**
     * Gets the value of the partitioningType property.
     * 
     * @return
     *     possible object is
     *     {@link PartitionType_sql2016 }
     *     
     */
    public PartitionType_sql2016 getPartitioningType() {
        return partitioningType;
    }

    /**
     * Sets the value of the partitioningType property.
     * 
     * @param value
     *     allowed object is
     *     {@link PartitionType_sql2016 }
     *     
     */
    public void setPartitioningType(PartitionType_sql2016 value) {
        this.partitioningType = value;
    }

    /**
     * Gets the value of the remoting property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getRemoting() {
        return remoting;
    }

    /**
     * Sets the value of the remoting property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setRemoting(Boolean value) {
        this.remoting = value;
    }

    /**
     * Gets the value of the localParallelism property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getLocalParallelism() {
        return localParallelism;
    }

    /**
     * Sets the value of the localParallelism property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setLocalParallelism(Boolean value) {
        this.localParallelism = value;
    }

    /**
     * Gets the value of the inRow property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getInRow() {
        return inRow;
    }

    /**
     * Sets the value of the inRow property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setInRow(Boolean value) {
        this.inRow = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="Object" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ObjectType" minOccurs="0"/>
     *       &lt;/sequence>
     *       &lt;attribute name="Type" use="required">
     *         &lt;simpleType>
     *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *             &lt;enumeration value="CloneLocation"/>
     *             &lt;enumeration value="Resource"/>
     *             &lt;enumeration value="SingleBrick"/>
     *             &lt;enumeration value="Region"/>
     *           &lt;/restriction>
     *         &lt;/simpleType>
     *       &lt;/attribute>
     *       &lt;attribute name="FragmentElimination" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "object"
    })
    public static class Activation_sql2016 {

        @XmlElement(name = "Object", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
        protected ObjectType_sql2016 object;
        @XmlAttribute(name = "Type", required = true)
        protected String type;
        @XmlAttribute(name = "FragmentElimination")
        @XmlSchemaType(name = "anySimpleType")
        protected String fragmentElimination;

        /**
         * Gets the value of the object property.
         * 
         * @return
         *     possible object is
         *     {@link ObjectType_sql2016 }
         *     
         */
        public ObjectType_sql2016 getObject() {
            return object;
        }

        /**
         * Sets the value of the object property.
         * 
         * @param value
         *     allowed object is
         *     {@link ObjectType_sql2016 }
         *     
         */
        public void setObject(ObjectType_sql2016 value) {
            this.object = value;
        }

        /**
         * Gets the value of the type property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getType() {
            return type;
        }

        /**
         * Sets the value of the type property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setType(String value) {
            this.type = value;
        }

        /**
         * Gets the value of the fragmentElimination property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getFragmentElimination() {
            return fragmentElimination;
        }

        /**
         * Sets the value of the fragmentElimination property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setFragmentElimination(String value) {
            this.fragmentElimination = value;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="Object" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ObjectType" minOccurs="0"/>
     *         &lt;element name="FragmentIdColumn" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SingleColumnReferenceType" minOccurs="0"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "object",
        "fragmentIdColumn"
    })
    public static class BrickRouting_sql2016 {

        @XmlElement(name = "Object", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
        protected ObjectType_sql2016 object;
        @XmlElement(name = "FragmentIdColumn", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
        protected SingleColumnReferenceType_sql2016 fragmentIdColumn;

        /**
         * Gets the value of the object property.
         * 
         * @return
         *     possible object is
         *     {@link ObjectType_sql2016 }
         *     
         */
        public ObjectType_sql2016 getObject() {
            return object;
        }

        /**
         * Sets the value of the object property.
         * 
         * @param value
         *     allowed object is
         *     {@link ObjectType_sql2016 }
         *     
         */
        public void setObject(ObjectType_sql2016 value) {
            this.object = value;
        }

        /**
         * Gets the value of the fragmentIdColumn property.
         * 
         * @return
         *     possible object is
         *     {@link SingleColumnReferenceType_sql2016 }
         *     
         */
        public SingleColumnReferenceType_sql2016 getFragmentIdColumn() {
            return fragmentIdColumn;
        }

        /**
         * Sets the value of the fragmentIdColumn property.
         * 
         * @param value
         *     allowed object is
         *     {@link SingleColumnReferenceType_sql2016 }
         *     
         */
        public void setFragmentIdColumn(SingleColumnReferenceType_sql2016 value) {
            this.fragmentIdColumn = value;
        }

    }

}
