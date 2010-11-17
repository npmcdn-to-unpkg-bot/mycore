package org.mycore.frontend.editor.validation;

import java.util.Date;

public class MCRDateTimePairValidator extends MCRComparingValidator {

    @Override
    public boolean hasRequiredProperties() {
        return super.hasRequiredProperties() && "datetime".equals(getProperty("type"));
    }

    protected int compare(String valueA, String valueB) throws Exception {
        String patterns = getProperty("format");
        MCRDateTimeConverter converter = new MCRDateTimeConverter(patterns);

        Date a = converter.string2date(valueA);
        Date b = converter.string2date(valueB);

        return Long.signum(a.getTime() - b.getTime());
    }
}
