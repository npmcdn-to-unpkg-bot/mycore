/**
 * @class
 * @constructor
 * @name		ToolbarModel
 * @description model of a toolbar with its functionalities to manage contained elements (e.g. buttonsets or divider)
 * @structure	
 * 		Object {
 * 			String:		id,					//identifier of the toolbar model
 * 			Array:		elements			//array of the contained elements within the toolbar (e.g. buttonset or divider)
 * 			Event:		events				//to trigger defined actions, while managing contained elements
 * 		}
 */
var ToolbarModel = function (id) {
    this.id = id;
    this.elements = [];
    
    this.events = new iview.Event(this);
};

ToolbarModel.prototype = {

	/**
	 * @public
	 * @function
	 * @name		getElementIndex
	 * @memberOf	ToolbarModel#
	 * @description returns the index of the given element
	 * @param		{String} elementName name that identifies a single element model
	 * @return		{integer} returns the index of the element model, identified by its elementName or -1 if not found
	 */
    getElementIndex : function(elementName) {
	    for (var i = 0; i < this.elements.length; i++) {
			if (this.elements[i].elementName == elementName) {
				return i;
			}
		}
	    return -1;
    },

	/**
	 * @public
	 * @function
	 * @name		getElement
	 * @memberOf	ToolbarModel#
	 * @description returns a single element model out of the toolbar containing elements
	 * @param		{String} elementName name that identifies a single element model
	 * @return		{Object} returns a single element model, identified by its elementName
	 */
    getElement : function(elementName) {
    	var i = this.getElementIndex(elementName);
    	if (i >= 0){
    		return this.elements[i];
    	}
    },

	/**
	 * @public
	 * @function
	 * @name		addElement
	 * @memberOf	ToolbarModel#
	 * @description adds a single element model to the toolbar,
	 *  notify the event-listener (to pass the informations to the toolbar (model) manager)
	 * @param		{Object} element defines the whole element model
	 * @param		{integer} index if set, defines the special position where to add the element between the other predefined elements
	 */
    addElement : function(element, index) {
    	element.relatedToolbar = this;
    	
    	var myself = this;
    	
    	if (!isNaN(index)) {
     		this.elements = this.elements.slice(0, index).concat(element, this.elements.slice(index, this.elements.length));
     		this.events.notify({'type' : "add", 'element' : jQuery.extend(element, {'index' : index})});
     	} else {
			this.elements.push(element);
			this.events.notify({'type' : "add", 'element' : element});
     	}
     	
    	if (element.type == "buttonset") {
    		// Events aus dem Buttonset-Model "weiterleiten"
    		element.events.attach(function (sender, args) {
    			myself.events.notify(jQuery.extend(args, {'elementName' : element.elementName}));
    		});
    		jQuery(element.buttons).each(function() {
    			element.events.notify({"type":"add", "button": 
    			{
    				"elementName":this.elementName,
    				"type" : this.type,
    				"subtype" : this.subtype,
    				"ui" : this.ui,
    				"title" : this.title,
    				"active" : this.active,
    				"loading" : this.loading,
    				"relatedButtonset": this.relatedButtonset}
    			})
    		});
    	}
		return element;
    },

	/**
	 * @public
	 * @function
	 * @name		removeElement
	 * @memberOf	ToolbarModel#
	 * @description removes a single element model from the toolbar,
	 *  notify the event-listener (to pass the informations to the toolbar (model) manager)
	 * @param		{String} elementName name that identifies a single element model
	 */     
    removeElement : function(elementName) {
     	var element = this.getElement(elementName);
     	this.elements.splice(element.index, 1);
     	this.events.notify({'type' : "del", 'element' : element});
    }
};
