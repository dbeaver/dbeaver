
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * This is the root element
 * 
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="BatchSequence">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Batch" maxOccurs="unbounded">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element name="Statements" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}StmtBlockType" maxOccurs="unbounded"/>
 *                           &lt;/sequence>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="Version" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Build" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "batchSequence"
})
@XmlRootElement(name = "ShowPlanXML", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class ShowPlanXML {

    @XmlElement(name = "BatchSequence", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected ShowPlanXML.BatchSequence_sql2005 batchSequence;
    @XmlAttribute(name = "Version", required = true)
    protected String version;
    @XmlAttribute(name = "Build", required = true)
    protected String build;

    /**
     * Gets the value of the batchSequence property.
     * 
     * @return
     *     possible object is
     *     {@link ShowPlanXML.BatchSequence_sql2005 }
     *     
     */
    public ShowPlanXML.BatchSequence_sql2005 getBatchSequence() {
        return batchSequence;
    }

    /**
     * Sets the value of the batchSequence property.
     * 
     * @param value
     *     allowed object is
     *     {@link ShowPlanXML.BatchSequence_sql2005 }
     *     
     */
    public void setBatchSequence(ShowPlanXML.BatchSequence_sql2005 value) {
        this.batchSequence = value;
    }

    /**
     * Gets the value of the version property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersion(String value) {
        this.version = value;
    }

    /**
     * Gets the value of the build property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBuild() {
        return build;
    }

    /**
     * Sets the value of the build property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBuild(String value) {
        this.build = value;
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
     *         &lt;element name="Batch" maxOccurs="unbounded">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;sequence>
     *                   &lt;element name="Statements" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}StmtBlockType" maxOccurs="unbounded"/>
     *                 &lt;/sequence>
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
    @XmlType(name = "", propOrder = {
        "batch"
    })
    public static class BatchSequence_sql2005 {

        @XmlElement(name = "Batch", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
        protected List<ShowPlanXML.BatchSequence_sql2005 .Batch_sql2005> batch;

        /**
         * Gets the value of the batch property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the batch property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getBatch().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link ShowPlanXML.BatchSequence_sql2005 .Batch_sql2005 }
         * 
         * 
         */
        public List<ShowPlanXML.BatchSequence_sql2005 .Batch_sql2005> getBatch() {
            if (batch == null) {
                batch = new ArrayList<ShowPlanXML.BatchSequence_sql2005 .Batch_sql2005>();
            }
            return this.batch;
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
         *         &lt;element name="Statements" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}StmtBlockType" maxOccurs="unbounded"/>
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
            "statements"
        })
        public static class Batch_sql2005 {

            @XmlElement(name = "Statements", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
            protected List<StmtBlockType_sql2005> statements;

            /**
             * Gets the value of the statements property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the JAXB object.
             * This is why there is not a <CODE>set</CODE> method for the statements property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getStatements().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link StmtBlockType_sql2005 }
             * 
             * 
             */
            public List<StmtBlockType_sql2005> getStatements() {
                if (statements == null) {
                    statements = new ArrayList<StmtBlockType_sql2005>();
                }
                return this.statements;
            }

        }

    }

}
