// maybe move this to iview2.xsl
var ToolbarImporter = function (Iview2, titles) {

				Iview2.getToolbarMgr = function() {
					if (!this.toolbarMgr) {
						this.toolbarMgr = new ToolbarManager();
					}
					return this.toolbarMgr;
				}
				
				Iview2.getToolbarCtrl = function() {
					if (!this.toolbarCtrl) {
						this.toolbarCtrl = new ToolbarController(this);
			
						this.toolbarCtrl.getViewer = function() {
							return this.parent;
						}
					}
					return this.toolbarCtrl;
				}
				
				// entweder Mgr macht alles und �bergabe des related... (Modelprovider) oder Models k�mmern sich untereinander und sch�ne Form (siehe unten)
				// Iview[viewID].getToolbarCtrl() oder Iview[viewID].toolbarCtrl verwenden?
				// vom Drop Down Menu nur die View oder auch ein Model im ToolbarManager?
				
				// Toolbar Manager
				Iview2.getToolbarMgr().setTitles(titles);
				
				Iview2.getToolbarMgr().addModel(new PreviewToolbarModelProvider("previewTb", titles).getModel());
				
				// Toolbar Controller
				Iview2.getToolbarCtrl().addView(new ToolbarView("previewTbView", Iview2.viewerContainer.find(".toolbars")));
				
				// holt alle bisherigen Models in den Controller und setzt diese entsprechend um
				Iview2.getToolbarCtrl().catchModels();

				
				// Permalink
				Iview2.getPermalinkCtrl = function() {
					if (!this.permalinkCtrl) {
						this.permalinkCtrl = new iview.Permalink.Controller(this);
						
						//iview.Permalink.Controller.prototype.getViewer = function() {
						this.permalinkCtrl.getViewer = function() {
							return this.parent;
						}
					}
					return this.permalinkCtrl;
				}

				Iview2.getPermalinkCtrl().addView(new iview.Permalink.View("permalinkView", Iview2.viewerContainer.find(".toolbars").parent()));
};