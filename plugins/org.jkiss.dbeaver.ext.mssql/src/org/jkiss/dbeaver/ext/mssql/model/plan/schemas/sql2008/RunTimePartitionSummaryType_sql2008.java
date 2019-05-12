
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2008;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * Runtime partition information provided in statistics xml for each relational iterator that support partitioning
 * 
 * <p>Java class for RunTimePartitionSummaryType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RunTimePartitionSummaryType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="PartitionsAccessed">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="PartitionRange" maxOccurs="unbounded" minOccurs="0">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;attribute name="Start" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                           &lt;attribute name="End" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *                 &lt;attribute name="PartitionCount" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RunTimePartitionSummaryType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "partitionsAccessed"
})
public class RunTimePartitionSummaryType_sql2008 {

    @XmlElement(name = "PartitionsAccessed", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected RunTimePartitionSummaryType_sql2008 .PartitionsAccessed_sql2008 partitionsAccessed;

    /**
     * Gets the value of the partitionsAccessed property.
     * 
     * @return
     *     possible object is
     *     {@link RunTimePartitionSummaryType_sql2008 .PartitionsAccessed_sql2008 }
     *     
     */
    public RunTimePartitionSummaryType_sql2008 .PartitionsAccessed_sql2008 getPartitionsAccessed() {
        return partitionsAccessed;
    }

    /**
     * Sets the value of the partitionsAccessed property.
     * 
     * @param value
     *     allowed object is
     *     {@link RunTimePartitionSummaryType_sql2008 .PartitionsAccessed_sql2008 }
     *     
     */
    public void setPartitionsAccessed(RunTimePartitionSummaryType_sql2008 .PartitionsAccessed_sql2008 value) {
        this.partitionsAccessed = value;
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
     *         &lt;element name="PartitionRange" maxOccurs="unbounded" minOccurs="0">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;attribute name="Start" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *                 &lt;attribute name="End" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *               &lt;/restriction>
     *             &lt;/complexContent>
     *           &lt;/complexType>
     *         &lt;/element>
     *       &lt;/sequence>
     *       &lt;attribute name="PartitionCount" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "partitionRange"
    })
    public static class PartitionsAccessed_sql2008 {

        @XmlElement(name = "PartitionRange", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
        protected List<RunTimePartitionSummaryType_sql2008 .PartitionsAccessed_sql2008 .PartitionRange_sql2008> partitionRange;
        @XmlAttribute(name = "PartitionCount", required = true)
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger partitionCount;

        /**
         * Gets the value of the partitionRange property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the partitionRange property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getPartitionRange().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link RunTimePartitionSummaryType_sql2008 .PartitionsAccessed_sql2008 .PartitionRange_sql2008 }
         * 
         * 
         */
        public List<RunTimePartitionSummaryType_sql2008 .PartitionsAccessed_sql2008 .PartitionRange_sql2008> getPartitionRange() {
            if (partitionRange == null) {
                partitionRange = new ArrayList<RunTimePartitionSummaryType_sql2008 .PartitionsAccessed_sql2008 .PartitionRange_sql2008>();
            }
            return this.partitionRange;
        }

        /**
         * Gets the value of the partitionCount property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getPartitionCount() {
            return partitionCount;
        }

        /**
         * Sets the value of the partitionCount property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setPartitionCount(BigInteger value) {
            this.partitionCount = value;
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
         *       &lt;attribute name="Start" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
         *       &lt;attribute name="End" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
         *     &lt;/restriction>
         *   &lt;/complexContent>
         * &lt;/complexType>
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "")
        public static class PartitionRange_sql2008 {

            @XmlAttribute(name = "Start", required = true)
            @XmlSchemaType(name = "unsignedLong")
            protected BigInteger start;
            @XmlAttribute(name = "End", required = true)
            @XmlSchemaType(name = "unsignedLong")
            protected BigInteger end;

            /**
             * Gets the value of the start property.
             * 
             * @return
             *     possible object is
             *     {@link BigInteger }
             *     
             */
            public BigInteger getStart() {
                return start;
            }

            /**
             * Sets the value of the start property.
             * 
             * @param value
             *     allowed object is
             *     {@link BigInteger }
             *     
             */
            public void setStart(BigInteger value) {
                this.start = value;
            }

            /**
             * Gets the value of the end property.
             * 
             * @return
             *     possible object is
             *     {@link BigInteger }
             *     
             */
            public BigInteger getEnd() {
                return end;
            }

            /**
             * Sets the value of the end property.
             * 
             * @param value
             *     allowed object is
             *     {@link BigInteger }
             *     
             */
            public void setEnd(BigInteger value) {
                this.end = value;
            }

        }

    }

}
