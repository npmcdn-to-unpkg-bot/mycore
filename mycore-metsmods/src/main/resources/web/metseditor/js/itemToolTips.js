/* $Revision: 2988 $ 
 * $Date: 2010-09-23 12:09:04 +0200 (Thu, 23 Sep 2010) $ 
 * $LastChangedBy: shermann $
 * Copyright 2010 - Th�ringer Universit�ts- und Landesbibliothek Jena
 *  
 * Mets-Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mets-Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mets-Editor.  If not, see http://www.gnu.org/licenses/.
 */
function displayItemProperties(){
	log("displayItemProperties()");
	var tree = dijit.byId("itemTree");
	var selectedItem = tree.lastFocused.item;;
	var propString =  "id=" + selectedItem.id + "<br/>" 
					  + "path= \"" + selectedItem.path					
					  + "\", name= \"" + selectedItem.name
					  + "\", orderLabel = \"" + selectedItem.orderLabel
					  + "\", type (tree) = \"" + selectedItem.type
					  + "\", structureType = \"" + selectedItem.structureType +"\"";

	document.getElementById('displayItemProps').innerHTML = propString;
}
