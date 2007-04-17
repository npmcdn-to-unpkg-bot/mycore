/************************************************/
/*  											*/
/* Module - MCR-Broadcasting 1.0, 05-2007  		*/
/* +++++++++++++++++++++++++++++++++++++		*/
/*  											*/
/* Andreas Trappe 	- concept, devel. 			*/
/*  											*/
/************************************************/
// ================================================================================================== //
function receiveBroadcast(sender, registerServlet) {
	
	var req;
	// transmit
	if (window.XMLHttpRequest) {
		req = new XMLHttpRequest();
	}
		else if (window.ActiveXObject) {
			req = new ActiveXObject("Microsoft.XMLHTTP");
		}
	req.open("GET", sender, true);
	
	req.onreadystatechange=function() {
	  if (req.readyState==4) {
	   if (req.status==200) {
	   
		   // alert message
		   var answerXML = req.responseXML;		   
		   var message = answerXML.getElementsByTagName("onAir")[0].firstChild.nodeValue;		   
		   alert(message);
		   
		   // register user as already received a message
		   addReceiver(registerServlet);
		   
		   // ask in peridical time spaces for new messages 
			var reCall = "receiveBroadcast('"+sender+"','"+registerServlet+"')";
	        setTimeout(reCall, 3000);
	   }
	  }		
	}
	req.send(null);		
}
// ================================================================================================== //
function addReceiver(registerServlet) {
	
	var note = registerServlet+"?mode=addReceiver";
	var req;
	// transmit
	if (window.XMLHttpRequest) {
		req = new XMLHttpRequest();
	}
		else if (window.ActiveXObject) {
			req = new ActiveXObject("Microsoft.XMLHTTP");
		}
	req.open("GET", note, true);
	
	req.onreadystatechange=function() {
	  if (req.readyState==4) {
	   if (req.status==200) {
		   var answerXML = req.responseXML;
		   var message = answerXML.getElementsByTagName("addReceiver")[0].firstChild.nodeValue;
		   //alert("addReceiver="+message);
	   }
	  }		
	}
	req.send(null);		
}

// ================================================================================================== //

