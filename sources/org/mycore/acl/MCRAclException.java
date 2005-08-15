/**
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
 **/

package org.mycore.acl;

///============================================================================�

import org.mycore.common.MCRException;

///============================================================================|

/**
 * Exception of this class are thrown when some illegal operation occurs during
 * administration of ACLs. Subclasses will define exceptions for special kinds
 * of problems.
 * 
 * @author Benno S�selbeck
 * @version 1.0.0, 01.11.2003
 *  
 */

public class MCRAclException extends MCRException {
	///============================================================================/

	/**
	 * Constructs an exception with an error message.
	 * 
	 * @param message
	 *            The error message describ�ng the cause of the exception.
	 */

	public MCRAclException(String message) {

		super(message);

	}

	//>============================================================================\
}