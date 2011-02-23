/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.lucene;

import java.io.File;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

/**
 * Use Lucene Analyzer to normalize strings
 * 
 * @author Harald Richter
 * 
 * @version $Revision$ $Date$
 * 
 */
public class MCRLuceneTools {
    MCRConfiguration config = MCRConfiguration.instance();

    private static Map<String, Analyzer> analyzerMap = new HashMap<String, Analyzer>();

    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCRLuceneTools.class);

    /**
     * Use Lucene Analyzer to normalize strings
     * 
     * @param value
     *            string to convert
     * @param ID
     *            The classes that do the normalization come from the lucene package
     *            and are configured by the property
     *            <tt>MCR.Lucene.Analyzer.<ID>.Class</tt> in mycore.properties.
     * 
     * @return the normalized string
     */
    public static String luceneNormalize(String value, String ID) throws Exception {
        Analyzer analyzer = analyzerMap.get(ID);
        if (null == analyzer) {
            analyzer = (Analyzer) MCRConfiguration.instance().getInstanceOf("MCR.Lucene.Analyzer." + ID + ".Class");
            analyzerMap.put(ID, analyzer);
        }

        StringBuilder sb = new StringBuilder();

        TokenStream ts = analyzer.tokenStream(null, new StringReader(value));
        while (ts.incrementToken()) {
            TermAttribute ta = ts.getAttribute(TermAttribute.class);
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(ta.termBuffer());
        }

        return sb.toString();
    }

    /**
     * Get Lucene Writer
     * 
     * @param indexDir
     *            directory where lucene index is store first check existance of
     *            index directory, if it does nor exist create it
     * 
     * @return the lucene writer, calling programm must close writer
     */
    public static IndexWriter getLuceneWriter(FSDirectory indexDir, boolean first) throws Exception {
        IndexWriter writer;
        Analyzer analyzer = new GermanAnalyzer(Version.LUCENE_CURRENT);

        // does directory for text index exist, if not build it
        if (first) {
            File file = indexDir.getFile();
            if (!file.exists()) {
                LOGGER.info("The Directory doesn't exist: " + indexDir + " try to build it");

                IndexWriter writer2 = new IndexWriter(indexDir, analyzer, true, IndexWriter.MaxFieldLength.UNLIMITED);
                writer2.close();
            } else if (file.isDirectory()) {
                if (0 == file.list().length) {
                    LOGGER.info("No Entries in Directory, initialize: " + indexDir);

                    IndexWriter writer2 = new IndexWriter(indexDir, analyzer, true, IndexWriter.MaxFieldLength.UNLIMITED);
                    writer2.close();
                }
            }
        } // if ( first

        writer = new IndexWriter(indexDir, analyzer, false, IndexWriter.MaxFieldLength.UNLIMITED);
        // writer.mergeFactor = 200;
        writer.setMergeFactor(200);
        // writer.maxMergeDocs = 2000;
        writer.setMaxMergeDocs(2000);

        return writer;
    }

    /**
     * Delete all documents in Lucene with id
     * 
     * @param fieldname
     *            string name of lucene field with stored id
     * @param id
     *            string document id
     * @param indexDir *
     *            the directory where index is stored
     * 
     */
    public static synchronized void deleteLuceneDocument(String fieldname, String id, FSDirectory indexDir) throws Exception {
        Term te1 = new Term(fieldname, id);
        getLuceneWriter(indexDir, false).deleteDocuments(te1);
    }

    static long getLongValue(String value) throws ParseException {
        SimpleDateFormat df = null;
        switch (value.length()) {
        case 19://"yyyy-MM-dd hh:mm:ss"
            df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        case 24://"yyyy-MM-ddThh:mm:ss.SSSZ
        	if (df == null) {
                df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");
            }
        case 4: //"yyyy""
        	if (df == null) {
                df = new SimpleDateFormat("yyyy");
            }
        case 10://"yyyy-MM-dd"
            if (df == null) {
                df = new SimpleDateFormat("yyyy-MM-dd");
            }
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            return df.parse(value).getTime();
        case 8://"hh:mm:ss"
            short hour = Short.parseShort(value.substring(0, 2));
            short minute = Short.parseShort(value.substring(3, 5));
            short second = Short.parseShort(value.substring(6));
            return (hour * 60 + minute) * 60 + second;
        default:
            throw new MCRException("Could not parse to long value: " + value);
        }
    }

}
