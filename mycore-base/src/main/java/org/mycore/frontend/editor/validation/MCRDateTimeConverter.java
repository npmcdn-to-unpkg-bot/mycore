package org.mycore.frontend.editor.validation;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.mycore.common.MCRException;

public class MCRDateTimeConverter {

    private List<SimpleDateFormat> formats = new ArrayList<SimpleDateFormat>();

    public MCRDateTimeConverter(MCRValidator validator) {
        String patterns = validator.getProperty("format");
        for (String pattern : patterns.split(";"))
            formats.add(getDateFormat(pattern.trim()));
    }

    protected SimpleDateFormat getDateFormat(String pattern) {
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        df.setLenient(false);
        return df;
    }

    public Date string2date(String input) throws MCRException {
        for (SimpleDateFormat format : formats) {
            if (format.toPattern().length() != input.length())
                continue;
            try {
                ParsePosition pp = new ParsePosition(0);
                Date value = format.parse(input, pp);
                if (pp.getIndex() == input.length())
                    return value;
            } catch (Exception ignored) {
            }
        }

        throw new MCRException("DateTime value can not be parsed: " + input);
    }
}
