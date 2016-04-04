package com.jeremydyer.processors.salesforce.domain;

import java.io.Serializable;

/**
 * Created by jdyer on 3/31/16.
 */
public class SObjectFieldDescription
        implements Serializable {

    private boolean aggregatable;
    private boolean autoNumber;
    private int byteLength;
    private boolean calculated;
    private String calculatedFormula;
    private boolean cascadeDelete;
    private boolean caseSensitive;
    private String controllerName;
    private boolean creatable;
    private boolean custom;
    private String defaultValue;
    private String defaultValueFormula;
    private boolean defaultedOnCreate;
    private boolean dependentPicklist;
    private boolean deprecatedAndHidden;
    private int digits;
    private boolean displayLocationInDecimal;
    private boolean encrypted;
    private boolean externalId;
    private Object extraTypeInfo;
    private boolean filterable;
    private Object filteredLookupInfo;
    private boolean groupable;
    private Object highScaleNumber;
    private boolean htmlFormatted;
    private boolean idLookup;
    private String inlineHelpText;
    private String label;
    private int length;
    private Object mask;
    private Object maskType;
    private String name;
    private Object nameField;
    private boolean namePointing;
    private boolean nillable;
    private boolean permissionable;
    private Object picklistValues;
    private double precision;
    private boolean queryByDistance;
    private Object referenceTargetField;
    private Object referenceTo;
    private String relationshipName;
    private Object relationshipOrder;
    private boolean restrictedDelete;
    private boolean restrictedPicklist;
    private double scale;
    private String soapType;
    private boolean sortable;
    private String type;
    private boolean unique;
    private boolean updateable;
    private boolean writeRequiresMasterRead;


    public boolean isAggregatable() {
        return aggregatable;
    }

    public void setAggregatable(boolean aggregatable) {
        this.aggregatable = aggregatable;
    }

    public boolean isAutoNumber() {
        return autoNumber;
    }

    public void setAutoNumber(boolean autoNumber) {
        this.autoNumber = autoNumber;
    }

    public int getByteLength() {
        return byteLength;
    }

    public void setByteLength(int byteLength) {
        this.byteLength = byteLength;
    }

    public boolean isCalculated() {
        return calculated;
    }

    public void setCalculated(boolean calculated) {
        this.calculated = calculated;
    }

    public String getCalculatedFormula() {
        return calculatedFormula;
    }

    public void setCalculatedFormula(String calculatedFormula) {
        this.calculatedFormula = calculatedFormula;
    }

    public boolean isCascadeDelete() {
        return cascadeDelete;
    }

    public void setCascadeDelete(boolean cascadeDelete) {
        this.cascadeDelete = cascadeDelete;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public String getControllerName() {
        return controllerName;
    }

    public void setControllerName(String controllerName) {
        this.controllerName = controllerName;
    }

    public boolean isCreatable() {
        return creatable;
    }

    public void setCreatable(boolean creatable) {
        this.creatable = creatable;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDefaultValueFormula() {
        return defaultValueFormula;
    }

    public void setDefaultValueFormula(String defaultValueFormula) {
        this.defaultValueFormula = defaultValueFormula;
    }

    public boolean isDefaultedOnCreate() {
        return defaultedOnCreate;
    }

    public void setDefaultedOnCreate(boolean defaultedOnCreate) {
        this.defaultedOnCreate = defaultedOnCreate;
    }

    public boolean isDependentPicklist() {
        return dependentPicklist;
    }

    public void setDependentPicklist(boolean dependentPicklist) {
        this.dependentPicklist = dependentPicklist;
    }

    public boolean isDeprecatedAndHidden() {
        return deprecatedAndHidden;
    }

    public void setDeprecatedAndHidden(boolean deprecatedAndHidden) {
        this.deprecatedAndHidden = deprecatedAndHidden;
    }

    public int getDigits() {
        return digits;
    }

    public void setDigits(int digits) {
        this.digits = digits;
    }

    public boolean isDisplayLocationInDecimal() {
        return displayLocationInDecimal;
    }

    public void setDisplayLocationInDecimal(boolean displayLocationInDecimal) {
        this.displayLocationInDecimal = displayLocationInDecimal;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public boolean isExternalId() {
        return externalId;
    }

    public void setExternalId(boolean externalId) {
        this.externalId = externalId;
    }

    public Object getExtraTypeInfo() {
        return extraTypeInfo;
    }

    public void setExtraTypeInfo(Object extraTypeInfo) {
        this.extraTypeInfo = extraTypeInfo;
    }

    public boolean isFilterable() {
        return filterable;
    }

    public void setFilterable(boolean filterable) {
        this.filterable = filterable;
    }

    public Object getFilteredLookupInfo() {
        return filteredLookupInfo;
    }

    public void setFilteredLookupInfo(Object filteredLookupInfo) {
        this.filteredLookupInfo = filteredLookupInfo;
    }

    public boolean isGroupable() {
        return groupable;
    }

    public void setGroupable(boolean groupable) {
        this.groupable = groupable;
    }

    public Object getHighScaleNumber() {
        return highScaleNumber;
    }

    public void setHighScaleNumber(Object highScaleNumber) {
        this.highScaleNumber = highScaleNumber;
    }

    public boolean isHtmlFormatted() {
        return htmlFormatted;
    }

    public void setHtmlFormatted(boolean htmlFormatted) {
        this.htmlFormatted = htmlFormatted;
    }

    public boolean isIdLookup() {
        return idLookup;
    }

    public void setIdLookup(boolean idLookup) {
        this.idLookup = idLookup;
    }

    public String getInlineHelpText() {
        return inlineHelpText;
    }

    public void setInlineHelpText(String inlineHelpText) {
        this.inlineHelpText = inlineHelpText;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public Object getMask() {
        return mask;
    }

    public void setMask(Object mask) {
        this.mask = mask;
    }

    public Object getMaskType() {
        return maskType;
    }

    public void setMaskType(Object maskType) {
        this.maskType = maskType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getNameField() {
        return nameField;
    }

    public void setNameField(Object nameField) {
        this.nameField = nameField;
    }

    public boolean isNamePointing() {
        return namePointing;
    }

    public void setNamePointing(boolean namePointing) {
        this.namePointing = namePointing;
    }

    public boolean isNillable() {
        return nillable;
    }

    public void setNillable(boolean nillable) {
        this.nillable = nillable;
    }

    public boolean isPermissionable() {
        return permissionable;
    }

    public void setPermissionable(boolean permissionable) {
        this.permissionable = permissionable;
    }

    public Object getPicklistValues() {
        return picklistValues;
    }

    public void setPicklistValues(Object picklistValues) {
        this.picklistValues = picklistValues;
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public boolean isQueryByDistance() {
        return queryByDistance;
    }

    public void setQueryByDistance(boolean queryByDistance) {
        this.queryByDistance = queryByDistance;
    }

    public Object getReferenceTargetField() {
        return referenceTargetField;
    }

    public void setReferenceTargetField(Object referenceTargetField) {
        this.referenceTargetField = referenceTargetField;
    }

    public Object getReferenceTo() {
        return referenceTo;
    }

    public void setReferenceTo(Object referenceTo) {
        this.referenceTo = referenceTo;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public void setRelationshipName(String relationshipName) {
        this.relationshipName = relationshipName;
    }

    public Object getRelationshipOrder() {
        return relationshipOrder;
    }

    public void setRelationshipOrder(Object relationshipOrder) {
        this.relationshipOrder = relationshipOrder;
    }

    public boolean isRestrictedDelete() {
        return restrictedDelete;
    }

    public void setRestrictedDelete(boolean restrictedDelete) {
        this.restrictedDelete = restrictedDelete;
    }

    public boolean isRestrictedPicklist() {
        return restrictedPicklist;
    }

    public void setRestrictedPicklist(boolean restrictedPicklist) {
        this.restrictedPicklist = restrictedPicklist;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public String getSoapType() {
        return soapType;
    }

    public void setSoapType(String soapType) {
        this.soapType = soapType;
    }

    public boolean isSortable() {
        return sortable;
    }

    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public boolean isUpdateable() {
        return updateable;
    }

    public void setUpdateable(boolean updateable) {
        this.updateable = updateable;
    }

    public boolean isWriteRequiresMasterRead() {
        return writeRequiresMasterRead;
    }

    public void setWriteRequiresMasterRead(boolean writeRequiresMasterRead) {
        this.writeRequiresMasterRead = writeRequiresMasterRead;
    }
}
