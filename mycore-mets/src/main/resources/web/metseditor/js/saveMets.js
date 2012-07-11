/* $Revision$ 
 * $Date$ 
 * $LastChangedBy$
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

/* returns an Array of invalid items (categories having not at least on child (a page)) */
function getInvalidItems(){
    log("getInvalidItems()");
    
    var tree = dijit.byId("itemTree");
    var model = tree.model;

    var invalidItems;
    
    model.getChildren(model.root, 
        function(items) {
            log("Validating tree"); 
            invalidItems = new Array();
            performValidation(items, invalidItems);
            log("Validating tree...done");
        }, 
        function() {
            log("Error occured in performValidation()")
        });
    return invalidItems;
}

/* actually performs the validation */
function performValidation(items, invalidItems){
    for(var i = 0; i < items.length; i++) {
        if(items[i].type == "item"){
            continue;
        } else {
            if(items[i].type == "category"){
                if(items[i].children.length == 0){
                    log("Found invalid item " + items[i].id);
                    invalidItems.push(items[i]);
                }
                
                for(var j = 0; j < items[i].children.length; j++){
                    log(items[i].children[j].name);
                    log(items[i].children[j].hide);
                    if(items[i].children[j].hide == true){
                        log("Structure contains a hidden page, please remove page " + items[i].children[j].name);
                        invalidItems.push(items[i]);
                    }
                }
                
                performValidation(items[i].children, invalidItems);
            }
        }
    }
}

function containsPages(anItem){
    var children = anItem.children;
    for(var c = 0; c < children.length; c++){
        if(children[c].type == "item"){
            return true;
        }
    }
    return false;
}

/* displays the invalid items to the user */
function displaySaveFailedDialog(invalidItems){
    log("displaySaveFailedDialog()");
    var msg = "";
    
    for(var i = 0; i < invalidItems.length; i++){
        msg += invalidItems[i].name;
        if(i + 1 < invalidItems.length){
            msg +=", ";
        }
    }
    document.getElementById('affectedItems').innerHTML = msg;  
    var dialog = dijit.byId("saveFailedDialog");
    dialog.show();
}

/* saves the tree/structure */
function save(){
   log("save()");
   var invalidItems = getInvalidItems(); 
   
   if(invalidItems.length > 0){
       log("Mets tree is in an invalid state");
       displaySaveFailedDialog(invalidItems);
       return;
   }
   var tree = buildDataStructure();
   log(dojo.toJson(tree));

   var data = dojo.toJson(tree);
   
   log("Submitting to Server...");
   log(data);
   dojo.xhrPost({
       url: webApplicationBaseURL + "servlets/SaveMetsServlet",
       handleAs: "text",
       postData: "jsontree=" + data + "&derivate=" + derivateId,
       load: function(response) {
            log('Mets successfully saved');
        },
        error: function(err, ioArgs){
            var secondDlg = new dijit.Dialog({
                title: "Zugriff verweigert",
                style: "width: 300px"
            });
            secondDlg.attr("content", "Mets konnte nicht erzeugt und gespeichert werden. Bitte wenden Sie sich an den Administrator.");
            secondDlg.show();
        }
   });
}