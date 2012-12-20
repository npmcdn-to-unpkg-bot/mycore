/**
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.buildtools.anttasks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Ant task that allows 'mycore.properties' manipulation via ant.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRConfigurationTask extends Task {
    private static final Charset PROPERTY_CHARSET = Charset.forName("ISO-8859-1");

    // some constants
    private static final String MCR_CONFIGURATION_INCLUDE_DEFAULT = "MCR.Configuration.Include";

    private Pattern includePattern;

    // some fields setable and getable via methods
    private String action;

    private String key;

    private String value;

    private File propertyFile;

    private File mergeFile;

    // some fields needed for processing
    private boolean valuePresent = false;

    private boolean propertiesLoaded = false;

    private boolean fileChanged = false;

    private int lineNumber = -1;

    private ArrayList<String> propLines;

    /**
     * Execute the requested operation.
     * 
     * @throws BuildException
     *             if an error occurs
     */
    @Override
    public void execute() throws BuildException {
        checkPreConditions();
        if (action != null && (action.equals("addInclude") || action.equals("removeInclude"))) {
            loadLines();
            if (!propertiesLoaded) {
                throw new BuildException("Could not load: " + propertyFile.getName());
            }
            if (action.equals("addInclude")) {
                addInclude();
            } else if (action.equals("removeInclude")) {
                removeInclude();
            }
            if (fileChanged) {
                writeLines();
            }
        } else {
            if (action != null && (action.equals("substituteVariables"))) {
                try {
                    Properties orig = getProperties(propertyFile);
                    substituteVariables(orig);
                    AlphabeticallyPropertyOutputter.store(orig, propertyFile, "automatically generated by "
                            + this.getClass().getName());
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new BuildException("Error while substituting properties.", e);
                }
            } else {
                try {
                    Properties orig = getProperties(propertyFile);
                    super.log("Merging property file: " + mergeFile);
                    orig.putAll(getProperties(mergeFile));
                    orig.remove("MCR.Configuration.Include");
                    AlphabeticallyPropertyOutputter.store(orig, propertyFile, "automatically generated by "
                            + this.getClass().getName());
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new BuildException("Error while merging properties.", e);
                }
            }
        }
        reset();
    }

    private Properties getProperties(File pFile) throws FileNotFoundException, IOException {
        Properties returns = new Properties();
        if (pFile.exists()) {
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(pFile);
                returns.load(stream);
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        }
        return returns;
    }

    /**
     * checks whether all preconditions are met
     */
    private void checkPreConditions() throws BuildException {
        setIncludePattern(key);
        if (action == null && mergeFile == null) {
            throw new BuildException("Must specify 'action' attribute");
        }
        if (propertyFile == null) {
            throw new BuildException("Must specify 'propertyfile' attribute");
        }
        if (value == null && mergeFile == null && (action == null || !action.equals("substituteVariables"))) {
            throw new BuildException("Must specify 'value' attribute");
        }
        if (mergeFile == null && (action == null || !action.equals("addInclude") && !action.equals("removeInclude") && !action.equals("substituteVariables"))) {
            throw new BuildException("action must be either 'addInclude' or 'removeInclude'");
        }
        if (!propertyFile.exists() && mergeFile == null) {
            throw new BuildException(new FileNotFoundException(propertyFile + " does not exists."));
        }
        if (mergeFile != null && !mergeFile.exists()) {
            throw new BuildException(new FileNotFoundException(mergeFile + "does not exists."));
        }
    }

    /**
     * resets all local fields
     */
    private void reset() {
        action = null;
        key = null;
        propertyFile = null;
        mergeFile = null;
        lineNumber = -1;
        propertiesLoaded = false;
        fileChanged = false;
        valuePresent = false;
    }

    /**
     * adds an include
     */
    private void addInclude() {
        if (valuePresent) {
            handleOutput(new StringBuffer("Not changing ").append(propertyFile.getName()).append(": '").append(value)
                    .append("' already included.").toString());
            return;
        }
        fileChanged = true;
        String prop = propLines.get(lineNumber);
        String newProp = prop;
        if (prop.charAt(prop.length() - 1) != '=') {
            newProp += ",";
        }
        newProp += value;
        propLines.remove(lineNumber);
        propLines.add(lineNumber, newProp);
        handleOutput(new StringBuffer(propertyFile.getName()).append(':').append(lineNumber).append(" added '").append(
                value).append("' to ").append(getKey()).toString());
    }

    /**
     * removes an include
     */
    private void removeInclude() {
        if (!valuePresent) {
            handleOutput(new StringBuffer("Not changing ").append(propertyFile.getName()).append(": '").append(value)
                    .append("' not present.").toString());
            return;
        }
        fileChanged = true;
        String newProp = propLines.get(lineNumber).replaceAll("," + value, "");
        propLines.remove(lineNumber);
        propLines.add(lineNumber, newProp);
        handleOutput(new StringBuffer(propertyFile.getName()).append(':').append(lineNumber).append(" removed '")
                .append(value).append("' from ").append(getKey()).toString());
    }

    /**
     * writes back the property file together with changed properties
     */
    private void writeLines() {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(propertyFile), PROPERTY_CHARSET));
            for (Object element : propLines) {
                writer.write(element.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            handleErrorOutput("Error while writing '" + propertyFile.getName() + "': " + e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    handleErrorOutput("Error while closing file '" + propertyFile.getName() + "': " + e.getMessage());
                }
            }
        }
    }

    /**
     * Fixes references on other properties in the property values, e.g. path2=%path1%/subdir/ (replaces %path1% by path1's value)
     */
    private void substituteVariables(Properties prop) {
        for (String name : prop.stringPropertyNames()) {
            String value = prop.getProperty(name);
            if (propertyHasVariables(value)) {
                String newvalue = propertyReplaceVariables(value,prop);
                System.out.println("New Value: " + name + "=" + newvalue);
                prop.put(name, newvalue);
            }
        }
    }
    
    private boolean propertyHasVariables(String value) {
        int p1 = value.indexOf('%');
        if (p1 == -1) return false;
        if (p1 == value.length()) return false;
        int p2 = value.indexOf('%',p1+1);
        return p2 != -1;
    }
    
    private String propertyReplaceVariables(String value, Properties prop) {
        String valueout = value;
        boolean property_not_found = false;
        int lastocc = -1;
        int p1, p2;
        while ((p1 = value.indexOf('%', lastocc + 1)) > -1) {
            p2 = value.indexOf('%', p1 + 1);
            if (p2 < 0)
                break;
            lastocc = p2;
            String ref = value.substring(p1 + 1, p2);
            String refValue = getPropValue(prop, ref);
            if (refValue == null) {
                property_not_found = true;
                continue;
            }
            valueout = value.substring(0, p1) + refValue.trim() + value.substring(p2 + 1);
        }
        if (!property_not_found && propertyHasVariables(valueout)) {
            valueout = propertyReplaceVariables(valueout, prop);
        }
        return valueout;
    }

    /**
     * Returns the value of a property that's name is given.
     */
    private String getPropValue(Properties prop, String name) {
        for (String _name : prop.stringPropertyNames()) {
            String _value = prop.getProperty(_name);
            if (_name.equals(name))
                return _value;
        }

        return null;
    }

    /*
     * loads the property file and marks the occurence of
     * MCR.Configuration.Include
     */
    private void loadLines() {
        BufferedReader reader = null;
        propertiesLoaded = false;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(propertyFile), PROPERTY_CHARSET));
            propLines = new ArrayList<String>(1000);
            int i = 0;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                // add each line of the property file to the array list
                propLines.add(line);
                if (lineNumber < 0 && includePattern.matcher(line).find()) {
                    // found the MCR.Configuration.Include line
                    lineNumber = i;
                    if (line.indexOf(value) > 0) {
                        // value is included
                        valuePresent = true;
                    }
                }
                i++;
            }
            if (lineNumber < 0) {
                propLines.add(key + "=");
                lineNumber = propLines.size() - 1;
            }
            propertiesLoaded = true;
        } catch (IOException e) {
            handleErrorOutput("Error while reading '" + propertyFile.getName() + "': " + e.getMessage());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                handleErrorOutput("Error while closing file '" + propertyFile.getName() + "': " + e.getMessage());
            }
        }
    }

    public String getAction() {
        return action;
    }

    /**
     * sets the action the task should perform.
     * 
     * @param action
     *            either "addInclude" or "removeInclude"
     */
    public void setAction(String action) {
        this.action = action;
    }

    public File getPropertyFile() {
        return propertyFile;
    }

    /**
     * sets the property file that needs to be changed.
     * 
     * @param action
     *            a 'mycore.properties' file
     */
    public void setPropertyFile(File propertyFile) {
        this.propertyFile = propertyFile;
    }

    public File getMergeFile() {
        return mergeFile;
    }

    public void setMergeFile(File mergeFile) {
        this.mergeFile = mergeFile;
    }

    public String getValue() {
        return value;
    }

    /**
     * sets the value for the action to be performed. For 'addInclude' a value
     * of "mycore.properties.moduleXY" would result in adding
     * ",mycore.properties.moduleXY" to the property
     * "MCR.Configuration.Include".
     * 
     * @param action
     *            a 'mycore.properties' file
     */
    public void setValue(String value) {
        this.value = value;
    }

    public String getKey() {
        if (key == null) {
            return MCR_CONFIGURATION_INCLUDE_DEFAULT;
        } else {
            return key;
        }
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setIncludePattern(String key) {
        if (key == null) {
            includePattern = Pattern.compile(MCR_CONFIGURATION_INCLUDE_DEFAULT);
        } else {
            includePattern = Pattern.compile(key);
        }
    }

    private static class AlphabeticallyPropertyOutputter {
        public static void store(Properties properties, File file, String comments) throws IOException {
            try {
                store(properties, new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "8859_1")),
                        comments);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        private static void store(Properties properties, BufferedWriter bw, String comments) throws IOException {
            boolean escUnicode = true;
            if (comments != null) {
                writeComments(bw, comments);
            }
            bw.write("#" + new Date().toString());
            bw.newLine();
            synchronized (properties) {
                List<String> list = new ArrayList<String>(properties.size());
                for (Entry<Object, Object> entry : properties.entrySet()) {
                    list.add(entry.getKey().toString());
                }
                Collections.sort(list);
                for (String key : list) {
                    String val = (String) properties.get(key);
                    key = saveConvert(key, true, escUnicode);
                    /*
                     * No need to escape embedded and trailing spaces for value,
                     * hence pass false to flag.
                     */
                    val = saveConvert(val, false, escUnicode);
                    bw.write(key + "=" + val);
                    bw.newLine();
                }
            }
            bw.flush();
        }

        private static void writeComments(BufferedWriter bw, String comments) throws IOException {
            bw.write("#");
            int len = comments.length();
            int current = 0;
            int last = 0;
            char[] uu = new char[6];
            uu[0] = '\\';
            uu[1] = 'u';
            while (current < len) {
                char c = comments.charAt(current);
                if (c > '\u00ff' || c == '\n' || c == '\r') {
                    if (last != current) {
                        bw.write(comments.substring(last, current));
                    }
                    if (c > '\u00ff') {
                        uu[2] = toHex(c >> 12 & 0xf);
                        uu[3] = toHex(c >> 8 & 0xf);
                        uu[4] = toHex(c >> 4 & 0xf);
                        uu[5] = toHex(c & 0xf);
                        bw.write(new String(uu));
                    } else {
                        bw.newLine();
                        if (c == '\r' && current != len - 1 && comments.charAt(current + 1) == '\n') {
                            current++;
                        }
                        if (current == len - 1 || comments.charAt(current + 1) != '#'
                                && comments.charAt(current + 1) != '!') {
                            bw.write("#");
                        }
                    }
                    last = current + 1;
                }
                current++;
            }
            if (last != current) {
                bw.write(comments.substring(last, current));
            }
            bw.newLine();
        }

        /*
         * Converts unicodes to encoded &#92;uxxxx and escapes special
         * characters with a preceding slash
         */
        private static String saveConvert(String theString, boolean escapeSpace, boolean escapeUnicode) {
            int len = theString.length();
            int bufLen = len * 2;
            if (bufLen < 0) {
                bufLen = Integer.MAX_VALUE;
            }
            StringBuffer outBuffer = new StringBuffer(bufLen);

            for (int x = 0; x < len; x++) {
                char aChar = theString.charAt(x);
                // Handle common case first, selecting largest block that
                // avoids the specials below
                if (aChar > 61 && aChar < 127) {
                    if (aChar == '\\') {
                        outBuffer.append('\\');
                        outBuffer.append('\\');
                        continue;
                    }
                    outBuffer.append(aChar);
                    continue;
                }
                switch (aChar) {
                case ' ':
                    if (x == 0 || escapeSpace) {
                        outBuffer.append('\\');
                    }
                    outBuffer.append(' ');
                    break;
                case '\t':
                    outBuffer.append('\\');
                    outBuffer.append('t');
                    break;
                case '\n':
                    outBuffer.append('\\');
                    outBuffer.append('n');
                    break;
                case '\r':
                    outBuffer.append('\\');
                    outBuffer.append('r');
                    break;
                case '\f':
                    outBuffer.append('\\');
                    outBuffer.append('f');
                    break;
                case '=': // Fall through
                case ':': // Fall through
                case '#': // Fall through
                case '!':
                    outBuffer.append('\\');
                    outBuffer.append(aChar);
                    break;
                default:
                    if ((aChar < 0x0020 || aChar > 0x007e) & escapeUnicode) {
                        outBuffer.append('\\');
                        outBuffer.append('u');
                        outBuffer.append(toHex(aChar >> 12 & 0xF));
                        outBuffer.append(toHex(aChar >> 8 & 0xF));
                        outBuffer.append(toHex(aChar >> 4 & 0xF));
                        outBuffer.append(toHex(aChar & 0xF));
                    } else {
                        outBuffer.append(aChar);
                    }
                }
            }
            return outBuffer.toString();
        }

        /**
         * Convert a nibble to a hex character
         * 
         * @param nibble
         *            the nibble to convert.
         */
        private static char toHex(int nibble) {
            return hexDigit[(nibble & 0xF)];
        }

        /** A table of hex digits */
        private static final char[] hexDigit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
                'E', 'F' };

    }

}
