package org.mycore.frontend.editor.validation.pair;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;
import org.mycore.frontend.editor.validation.pair.MCRDecimalPairValidator;

public class MCRDecimalPairValidatorTest extends MCRComparingValidatorTest {

    @Before
    public void setup() {
        validator = new MCRDecimalPairValidator();
        validator.setProperty("format", "de");
        lowerValue = "3,14";
        higherValue = "3,15";
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("type", "decimal");
        validator.setProperty("operator", "=");
        assertTrue(validator.hasRequiredProperties());
    }
}
