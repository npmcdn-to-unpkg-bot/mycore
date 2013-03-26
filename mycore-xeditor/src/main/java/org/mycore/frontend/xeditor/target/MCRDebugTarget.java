/*
 * $Revision$ 
 * $Date$
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

package org.mycore.frontend.xeditor.target;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletContext;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCREditorSession;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRDebugTarget extends MCREditorTargetBase {

    @Override
    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String parameter) throws Exception {
        job.getResponse().setContentType("text/html; charset=UTF-8");
        PrintWriter out = job.getResponse().getWriter();
        out.println("<html><body>");

        if (session.getSourceURI() != null) {
            out.println("<h3>When loading from source URI:</h3>");
            outputXML(MCRSourceContent.getInstance(session.getSourceURI()).asXML(), out);
        }

        out.println("<h3>After form transformation, before submit:</h3>");
        outputXML(session.getEditedXML(), out);

        out.println("<h3>Submitted parameters:</h3>");
        outputParameters(job, out);

        super.handleSubmission(context, job, session, parameter);

        out.println("<h3>After submit:</h3>");
        outputXML(session.getEditedXML(), out);

        out.println("</body></html>");
        out.close();
    }

    private void outputParameters(MCRServletJob job, PrintWriter out) {
        out.println("<p><pre>");

        for (Enumeration<String> parameters = job.getRequest().getParameterNames(); parameters.hasMoreElements();) {
            String name = parameters.nextElement();
            for (String value : job.getRequest().getParameterValues(name)) {
                out.println(name + " = " + value);
            }
        }

        out.println("</pre></p>");
    }

    private Format format = Format.getPrettyFormat().setLineSeparator("\n").setOmitDeclaration(true);

    private void outputXML(Document xml, PrintWriter out) throws IOException {
        XMLOutputter outputter = new XMLOutputter(format);
        out.println("<p>");
        Element pre = new Element("pre");
        pre.addContent(outputter.outputString(xml));
        outputter.output(pre, out);
        out.println("</p>");
    }
}
