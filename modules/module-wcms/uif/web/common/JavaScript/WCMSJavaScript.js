/* WCMS ------------------------------------------------------------------------------------ */
function previewPicture(imagePath)
{
	var stuff = document.editContent.selectPicturePreview.options;
	var page = stuff[stuff.selectedIndex].value;
	
	if (page == "") {}
	else 
	  {
/*	    document.image.src=page;		  */
	    document.image.src=imagePath+page;		  
	  }
} 
function setHelpText() 
{
  /* set both help texts for 'choose action' */
  var temp = document.choose.action.options;
  var value = temp[temp.selectedIndex].value;
  
  if ( value == "edit") 
  {
	  var helpText_1 = document.createTextNode("Vorhandenen Inhalt bearbeiten hei�t, dass sie schon bestehende Webseiten ver�ndern k�nnen. Sie haben also hier die M�glichkeit den HTML-Quelltext der gew�hlten Seite direkt anzupassen.");  
	  var helpText_2 = document.createTextNode("Bitte w�hlen sie hier die Seite aus, die sie bearbeiten wollen.");  	  
  }
	  else if (value == "add_intern") 
	  {
		  var helpText_1 = document.createTextNode("Eine neue Webseite besteht aus einem Men�eintrag und zugeh�rigem HTML-Inhalt. Sie stellen also einen eigenen neuen Men�eintrag und die dazu geh�rige HTML-Seite in das System ein.");
		  var helpText_2 = document.createTextNode("Eine neue Webseite wird immer unter einem bestimmtem Obermen�punkt eingestellt. W�hlen sie also hier den Men�punkt aus, UNTER DEM die Seite angelegt werden soll. Hinweis: Einen Hauptmen�punkt k�nnen sie einstellen, indem sie direkt das entsprechende Men� ausw�hlen.");
	  }
		  else if (value == "add_extern") 
		  {
			  var helpText_1 = document.createTextNode("Ein Link besteht nur aus einem Men�eintrag. Sie stellen also hier einen neuen Men�eintrag in das System ein, der beim Anklicken auf die von ihnen angegebene Link-Adresse verweist.");
			  var helpText_2 = document.createTextNode("Eine neuer Link wird immer unter einem bestimmtem Obermen�punkt eingestellt. W�hlen sie also hier den Men�punkt aus, UNTER DEM die der Link angelegt werden soll. Hinweis: Einen Link als Hauptmen�punkt k�nnen sie einstellen, indem sie direkt das entsprechende Men� ausw�hlen.");			  
		  }	  
			  else if (value == "delete") 
			  {
				  var helpText_1 = document.createTextNode("Vorhandener Inhalt l�schen bedeutet, dass sie einen bestimmten Men�punkt nebst Inhalt l�schen. Hinweis: Wenn sie einen Link l�schen wird nat�rlich nur der Men�punkt gel�scht, da in dem Fall kein Inhalt existiert. ");
				  var helpText_2 = document.createTextNode("W�hlen sie hier den Inhalt aus, den sie l�schen m�chten.");				  
			  }	  		  
			  
   document.getElementById("helpText.chooseAction").replaceChild(helpText_1, document.getElementById("helpText.chooseAction").firstChild);
   document.getElementById("helpText.chooseLocation").replaceChild(helpText_2, document.getElementById("helpText.chooseLocation").firstChild);   
}
function refreshClose() 
{
	window.opener.location.reload(true);
	window.close();
}
/* END OF: WCMS ------------------------------------------------------------------------------------ */

