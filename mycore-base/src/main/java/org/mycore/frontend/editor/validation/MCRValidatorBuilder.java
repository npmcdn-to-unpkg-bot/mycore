package org.mycore.frontend.editor.validation;

import org.mycore.frontend.editor.validation.pair.MCRDateTimePairValidator;
import org.mycore.frontend.editor.validation.pair.MCRDecimalPairValidator;
import org.mycore.frontend.editor.validation.pair.MCRIntegerPairValidator;
import org.mycore.frontend.editor.validation.pair.MCRStringPairValidator;
import org.mycore.frontend.editor.validation.value.MCRDateTimeValidator;
import org.mycore.frontend.editor.validation.value.MCRDecimalValidator;
import org.mycore.frontend.editor.validation.value.MCRIntegerValidator;
import org.mycore.frontend.editor.validation.value.MCRMaxDateTimeValidator;
import org.mycore.frontend.editor.validation.value.MCRMaxDecimalValidator;
import org.mycore.frontend.editor.validation.value.MCRMaxIntegerValidator;
import org.mycore.frontend.editor.validation.value.MCRMaxLengthValidator;
import org.mycore.frontend.editor.validation.value.MCRMaxStringValidator;
import org.mycore.frontend.editor.validation.value.MCRMinDateTimeValidator;
import org.mycore.frontend.editor.validation.value.MCRMinDecimalValidator;
import org.mycore.frontend.editor.validation.value.MCRMinIntegerValidator;
import org.mycore.frontend.editor.validation.value.MCRMinLengthValidator;
import org.mycore.frontend.editor.validation.value.MCRMinStringValidator;
import org.mycore.frontend.editor.validation.value.MCRRegExpValidator;
import org.mycore.frontend.editor.validation.value.MCRXSLConditionValueValidator;
import org.mycore.frontend.editor.validation.xml.MCRExternalXMLValidator;
import org.mycore.frontend.editor.validation.xml.MCRXSLConditionElementValidator;

public class MCRValidatorBuilder {

    public static MCRCombinedValidator buildPredefinedCombinedValidator() {
        MCRCombinedValidator validator = new MCRCombinedValidator();
        validator.addValidator(new MCRMaxLengthValidator());
        validator.addValidator(new MCRMinLengthValidator());
        validator.addValidator(new MCRRegExpValidator());
        validator.addValidator(new MCRXSLConditionValueValidator());
        validator.addValidator(new MCRExternalValidator());
        validator.addValidator(new MCRDateTimeValidator());
        validator.addValidator(new MCRMaxDateTimeValidator());
        validator.addValidator(new MCRMinDateTimeValidator());
        validator.addValidator(new MCRIntegerValidator());
        validator.addValidator(new MCRMaxIntegerValidator());
        validator.addValidator(new MCRMinIntegerValidator());
        validator.addValidator(new MCRDecimalValidator());
        validator.addValidator(new MCRMaxDecimalValidator());
        validator.addValidator(new MCRMinDecimalValidator());
        validator.addValidator(new MCRMaxStringValidator());
        validator.addValidator(new MCRMinStringValidator());
        return validator;
    }

    public static MCRCombinedValidator buildPredefinedCombinedPairValidator() {
        MCRCombinedValidator validator = new MCRCombinedValidator();
        validator.addValidator(new MCRStringPairValidator());
        validator.addValidator(new MCRIntegerPairValidator());
        validator.addValidator(new MCRDecimalPairValidator());
        validator.addValidator(new MCRDateTimePairValidator());
        validator.addValidator(new MCRExternalValidator());
        return validator;
    }

    public static MCRCombinedValidator buildPredefinedCombinedElementValidator() {
        MCRCombinedValidator validator = new MCRCombinedValidator();
        validator.addValidator(new MCRExternalXMLValidator());
        validator.addValidator(new MCRXSLConditionElementValidator());
        return validator;
    }
}
