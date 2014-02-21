/*
 * @package wcms.navigation
 * @description editor for a tree item
 */
var wcms = wcms || {};
wcms.navigation = wcms.navigation || {};

wcms.navigation.RootItemEditor = function() {
	this.constructor();

	// links
	this.hrefTextBox = null;
	this.hrefStartingPageTextBox = null;
	this.dirTextBox = null;
	// titles & layout
	this.mainTitleTextBox = null;
	this.historyTitleTextBox = null;
	this.templateSelect = null;
};

( function() {

	// i18n text
	// ie-bug: use var instead of const
	var mainHeaderText = "component.wcms.navigation.rootItemEditor.mainHeader";
	var titleAndLayoutCaption = "component.wcms.navigation.rootItemEditor.titleAndLayoutCaption";

	var hrefText = "component.wcms.navigation.rootItemEditor.href";
	var hrefStartingPageText = "component.wcms.navigation.rootItemEditor.hrefStartingPage";
	var dirText = "component.wcms.navigation.rootItemEditor.dir";
	var mainTitleText = "component.wcms.navigation.rootItemEditor.mainTitle";
	var historyTitleText = "component.wcms.navigation.rootItemEditor.historyTitle";
	var templateText = "component.wcms.navigation.itemEditor.template";
	var templateNoneText = "component.wcms.navigation.itemEditor.template.none";

	function create() {
		// create dijit components
		// links
		this.hrefTextBox = new dijit.form.TextBox({intermediateChanges: true});
		this.hrefStartingPageTextBox = new dijit.form.TextBox({intermediateChanges: true});
		this.dirTextBox = new dijit.form.TextBox({intermediateChanges: true});
		// titles & layout
		this.mainTitleTextBox = new dijit.form.TextBox({intermediateChanges: true});
		this.historyTitleTextBox = new dijit.form.TextBox({intermediateChanges: true});
		this.templateSelect = new dijit.form.Select({
			maxHeight: "300",
		});

		// layout
		getTemplateList(dojo.hitch(this, function(data) {
			this.templateSelect.addOption({i18n: templateNoneText});
			this.templateSelect.addOption({});
			for(var i in data) {
				this.templateSelect.addOption({value: data[i], label: data[i]});
			}
			if(this.currentItem != null && this.currentItem.template != "none") {
				this.templateSelect.set("value", this.currentItem.template);
				var value = this.templateSelect.get("value");
				if(this.currentItem.template != value) {
					console.log("Unable to set template to " + this.currentItem.template +
								". This template does not exist!");
				}
			}
			I18nManager.getInstance().updateI18nSelect(this.templateSelect);
		}));

		var buildTableFunc = dojo.hitch(this, buildTable);
		buildTableFunc();

		// -href
		dojo.connect(this.hrefTextBox, "onChange", this, function(/*String*/ value) {
			if(this.currentItem == null)
				return;
			if(!equal(this.currentItem.href, value)) {
				this.currentItem.href = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});
		// -href starting page
		dojo.connect(this.hrefStartingPageTextBox, "onChange", this, function(/*String*/ value) {
			if(this.currentItem == null)
				return;
			if(!equal(this.currentItem.hrefStartingPage, value)) {
				this.currentItem.hrefStartingPage = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});
		// -directory
		dojo.connect(this.dirTextBox, "onChange", this, function(/*String*/ value) {
			if(this.currentItem == null)
				return;
			if(!equal(this.currentItem.dir, value)) {
				this.currentItem.dir = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});
		// -main title
		dojo.connect(this.mainTitleTextBox, "onChange", this, function(/*String*/ value) {
			if(this.currentItem == null)
				return;
			if(!equal(this.currentItem.mainTitle, value)) {
				this.currentItem.mainTitle = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});
		// -history title
		dojo.connect(this.historyTitleTextBox, "onChange", this, function(/*String*/ value) {
			if(this.currentItem == null)
				return;
			if(!equal(this.currentItem.historyTitle, value)) {
				this.currentItem.historyTitle = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});
		// -template select
		dojo.connect(this.templateSelect, "onChange", this, function(/*String*/ value) {
			if(this.currentItem == null)
				return;
			if(!equal(this.currentItem.template, value)) {
				this.currentItem.template = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});
	}

	function buildTable() {
		// caption
		this.setHeader(mainHeaderText);

		// links
		this.addElement(hrefText, this.hrefTextBox.domNode);
		this.addElement(hrefStartingPageText, this.hrefStartingPageTextBox.domNode);
		this.addElement(dirText, this.dirTextBox.domNode);
		// titles and layouts
		this.addCaption(titleAndLayoutCaption);
		this.addElement(mainTitleText, this.mainTitleTextBox.domNode);
		this.addElement(historyTitleText, this.historyTitleTextBox.domNode);
		this.addElement(templateText, this.templateSelect.domNode);
		// update i18n texts
		this.updateLang();
	}

	function updateEditor(/*JSON*/ item) {
		this.currentItem = item;
		// general
		this.setValue(this.hrefTextBox, item.href);
		this.setValue(this.hrefStartingPageTextBox, item.hrefStartingPage);
		this.setValue(this.dirTextBox, item.dir);
		// titles & layout
		this.setValue(this.mainTitleTextBox, item.mainTitle);
		this.setValue(this.historyTitleTextBox, item.historyTitle);
		this.setValue(this.templateSelect, item.template);
	}

	function reset() {
		if(this.currentItem != null)
			this.currentItem = null;
		// links
		this.hrefTextBox.set("value", null);
		this.hrefStartingPageTextBox.set("value", null);
		this.dirTextBox.set("value", null);
		// titles and layouts
		this.mainTitleText.set("value", null);
		this.historyTitleTextBox.set("value", null);
		this.templateSelect.set("value", null);
	}

	function setDisabled(/*boolean*/ value) {
		this.disabled = value;
		// links
		this.hrefTextBox.set("disabled", this.disabled);
		this.hrefStartingPageTextBox.set("disabled", this.disabled);
		this.dirTextBox.set("disabled", this.disabled);
		// titles and layouts
		this.mainTitleTextBox.set("disabled", this.disabled);
		this.historyTitleTextBox.set("disabled", this.disabled);
		this.templateSelect.set("disabled", this.disabled);
	}

	function updateLang() {
		// update labels
		wcms.gui.ContentEditor.prototype.updateLang.call(this);
		// update drop down labels
		I18nManager.getInstance().updateI18nSelect(this.templateSelect);
	}

	// inheritance
	wcms.navigation.RootItemEditor.prototype = new wcms.gui.ContentEditor;

	// own methods
	wcms.navigation.RootItemEditor.prototype.create = create;
	wcms.navigation.RootItemEditor.prototype.updateEditor = updateEditor;
	wcms.navigation.RootItemEditor.prototype.reset = reset;
	wcms.navigation.RootItemEditor.prototype.setDisabled = setDisabled;
	wcms.navigation.RootItemEditor.prototype.updateLang = updateLang;
})();
