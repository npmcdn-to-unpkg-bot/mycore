package org.mycore.importer;

import java.util.ArrayList;
import java.util.List;

import org.mycore.common.MCRException;

/**
 * A field is the smallest information unit for the convert process.
 * It is simply described by a value and an id.
 * 
 * @author Matthias Eichner
 */
public class MCRImportField {

    public final static String DEFAULT_SEPARATOR = ".";
    
    private String id;
    private String value;
    private List<MCRImportField> subFieldList;

    private String seperator;

    public MCRImportField(String id, String value) {
        this(id, value, DEFAULT_SEPARATOR);
    }

    public MCRImportField(String id, String value, String separator) throws InvalidIdException {
        this.id = id;
        this.value = value;
        this.subFieldList = new ArrayList<MCRImportField>();
        this.seperator = separator;
        validateId();
    }

    public String getId() {
        return id;
    }
    public void setId(String id) throws InvalidIdException {
        this.id = id;
        validateId();
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Adds a new subfield.
     * 
     * @param subField the field to add
     */
    public void addField(MCRImportField subField) {
        this.subFieldList.add(subField);
    }
    public void removeField(MCRImportField subField) {
        this.subFieldList.remove(subField);
    }
    public List<MCRImportField> getSubFieldList() {
        return subFieldList;
    }

    public void setSeperator(String seperator) {
        this.seperator = seperator;
    }
    public String getSeperator() {
        return seperator;
    }

    /**
     * This method tries to fiend the field which matches with
     * the fieldId. All subfields and their children are
     * compared.
     * 
     * @param fieldId fieldId to match
     * @return field with the matched id or null if nothing found
     */
    public List<MCRImportField> getFields(String fieldId) {
        List<MCRImportField> fieldList = new ArrayList<MCRImportField>();
        if(this.id.equals(fieldId)) {
            fieldList.add(this);
        } else if(fieldId.startsWith(this.id)) {
            String subId = fieldId.substring(this.id.length());
            if(subId.startsWith(this.seperator)) {
                subId = subId.substring(this.seperator.length());
                for(MCRImportField subField : subFieldList) {
                    List<MCRImportField> matchedSubFields = subField.getFields(subId);
                    if(!matchedSubFields.isEmpty())
                        fieldList.addAll(matchedSubFields);
                }
            }
        }
        return fieldList;
    }

    public boolean isEmpty() {
        if(this.value != null && !this.value.equals(""))
            return false;
        for(MCRImportField subfiField : subFieldList)
            if(!subfiField.isEmpty())
                return false;
        return true;
    }

    @Override
    public String toString() {
        return id + ": " + value;
    }

    private void validateId() throws InvalidIdException {
        if(this.id.contains(this.seperator))
            throw new InvalidIdException("The id '" + id +
                    "' contains the separator string '" + this.seperator +
                    "'!");
    }

    public static class InvalidIdException extends MCRException {
        public InvalidIdException(String msg) {
            super(msg);
        }
    }
}