var wcms = wcms || {};
wcms.access = wcms.access || {};

wcms.access.AccessTab = function(navContentEHandler) {
	// dom
	this.domNode = null;
	this.content = navContentEHandler;

	this.treeTable = undefined;
	this.editRuleDialog = undefined;
	this.removeRuleDialog = undefined;
	this.data = [];
	this.lang = "de";
	this.navigation = undefined;
	this.ruleEventHandler = new wcms.common.EventHandler(this);
};

(function() {

	function preload() {
		console.log("load accTab");
		this.lang = I18nManager.getInstance().getLang();
		this.domNode = new dijit.layout.ContentPane({
			id : "access",
			title : I18nManager.getInstance().getI18nTextAsString("component.wcms.access")
		});
		this.content.attach(dojo.hitch(this, handleContentEvents));
		this.ruleEventHandler.attach(dojo.hitch(this, createDialogs));
		dojo.hitch(this, loadRuleList)();
	}
	function getPreloadName() {
		return "Access";
	}
	function getPreloadWeight() {
		return 10;
	}

	function getRow(node, access) {
		var cell = dojo.create("div");

		// text
		var textString = undefined;
		if (access == "read" && node.config.readId != null && node.config.readDes != null)
			if (node.config.readId == "") {
				textString = "";
			} else {
				textString = node.config.readDes + " (" + node.config.readId + ")";
			}
		else if (access == "write" && node.config.writeId != null && node.config.writeDes != null)
			if (node.config.writeId == "") {
				textString = "";
			} else {
				textString = node.config.writeDes + " (" + node.config.writeId + ")";
			}
		var text = dojo.create("span", {
			style : "float: left; max-width: 300px; white-space: normal;",
			innerHTML : textString
		});
		cell.appendChild(text);

		// buttons
		if (node.tree.activeNode == node) {
			var buttonSpan = dojo.create("span", {
				style : "float: right;"
			});
			var editButton = dojo.create("a", {
				style : "cursor: pointer;padding-right: 4px;",
				innerHTML : "<img src='images/edit_16.png'>",
				onclick : dojo.hitch(this, function() {
					var parameter = {
						access : access,
						node : node
					};
					this.editRuleDialog.additionalData = parameter;
					this.editRuleDialog.show(parameter);
				})
			});
			var deleteButton = dojo.create("a", {
				style : "cursor: pointer;padding-right: 4px;",
				innerHTML : "<img src='images/remove_16.png'>",
				onclick : dojo.hitch(this, function() {
					var parameter = {
						access : access,
						node : node
					};
					this.removeRuleDialog.additionalData = parameter;
					this.removeRuleDialog.show();
				})
			});

			buttonSpan.appendChild(editButton);
			buttonSpan.appendChild(deleteButton);
			cell.appendChild(buttonSpan);
		}
		return cell;
	}

	function expandAll() {
		this.treeTable.expandAll();
	}

	function handleContentEvents(/* NavigationContent */source, /* Json */args) {
		if (args.type == "loaded") {
			if (args.navigation.hierarchy == null) {
				console.log("Error while loading: navigation.hierarchy is null!");
			} else {
				this.navigation = args.navigation;
				dojo.hitch(this, createTree)();
			}
		}
	}

	function createTree() {
		this.data.push({
			'id' : 1,
			'pid' : 0,
			title : this.navigation.items[0].mainTitle,
			readId : this.navigation.items[0].access.read.ruleID,
			readDes : this.navigation.items[0].access.read.ruleDes,
			writeId : this.navigation.items[0].access.write.ruleID,
			writeDes : this.navigation.items[0].access.write.ruleDes,
			href : this.navigation.items[0].hrefStartingPage,
		});

		for ( var i = 0; i < this.navigation.hierarchy[0].children.length; i++) {
			dojo.hitch(this, createData)(this.navigation.items, this.navigation.hierarchy[0].children[i], 1);
		}

		// create tree table
		this.treeTable = new TreeTable({
			nodes : this.data,
			indent : 20,
			cm : [ {
				text : I18nManager.getInstance().getI18nTextAsString("component.wcms.access.treeTable.entryHeader"),
				width : '300px'
			}, {
				text : I18nManager.getInstance().getI18nTextAsString("component.wcms.access.treeTable.readHeader"),
				renderer : dojo.hitch(this, function(node) {
					return dojo.hitch(this, getRow)(node, "read");
				}),
				width : '350px'
			}, {
				text : I18nManager.getInstance().getI18nTextAsString("component.wcms.access.treeTable.writeHeader"),
				renderer : dojo.hitch(this, function(node) {
					var getRowFunc = dojo.hitch(this, getRow);
					return getRowFunc(node, "write");
				}),
				width : '350px'
			} ],
			titleWidth : '60%'
		});
		this.domNode.set("content", this.treeTable.domNode);
		this.treeTable.render();
		this.treeTable.colorize();
		this.treeTable.expandAll();
	}

	function createData(itemList, hierarchy, pid) {
		var tempdata = itemList[hierarchy.wcmsId];
		var href = "";
		if (tempdata.dir != undefined) {
			href = tempdata.dir;
		} else {
			href = tempdata.href;
		}
		this.data.push({
			'id' : tempdata.wcmsId + 1,
			'pid' : pid,
			title : eval("tempdata.labelMap." + this.lang),
			readId : tempdata.access.read.ruleID,
			readDes : tempdata.access.read.ruleDes,
			writeId : tempdata.access.write.ruleID,
			writeDes : tempdata.access.write.ruleDes,
			href : href,
		});
		if (hierarchy.children != null) {
			for ( var i = 0; i < hierarchy.children.length; i++) {
				dojo.hitch(this, createData)(itemList, hierarchy.children[i], tempdata.wcmsId + 1);
			}
		}
	}

	function createDialogs(/* loadRuleList */loadsource, /* RuleList */args) {
		if (args.type == "loaded") {
			// dialogs
			dojo.hitch(this, createRemoveDialog)();
			this.editRuleDialog = new wcms.access.RuleDialog(args.ruleSet);
			this.editRuleDialog.eventHandler.attach(dojo.hitch(this, handleChangeAccess));
		}
	}

	function createRemoveDialog() {
		this.removeRuleDialog = new wcms.gui.SimpleDialog("yesNo", "component.wcms.access.ruleDialog.deleteCaption",
				"component.wcms.access.ruleDialog.deleteLabel")
		this.removeRuleDialog.eventHandler.attach(dojo.hitch(this, function(/* wcms.gui.SimpleDialog */source, /* Json */args) {
			if (args.type == "yesButtonClicked") {
				var access = args.additionalData.access;
				var node = args.additionalData.node;
				console.log("AccessMain: Remove " + access + " access of " + node.config.title);
				dojo.hitch(this, remove)(node, access);
			}
		}));
	}

	function handleChangeAccess(/* RuleDialog */source, /* changes */args) {
		dojo.hitch(this, updateOrCreate)(args.node, args.perm, args.ruleId);
		console.log("AccessMain: Change " + args.perm + " access of " + args.node.config.title);
	}

	function changeNode(node, perm, rule) {
		if (perm == "read") {
			node.config.readId = rule.ruleId;
			node.config.readDes = rule.ruleDes;
		} else {
			node.config.writeId = rule.ruleId;
			node.config.writeDes = rule.ruleDes;
		}
	}

	function updateOrCreate(node, perm, ruleID) {
		var webPageID = node.config.href;
		var url = wcms.settings.wcmsURL + "/access?webPageID=" + webPageID + "&perm=" + perm + "&ruleID=" + ruleID;
		var xhrArgs = {
			url : url,
			handleAs : "json",
			load : dojo.hitch(this, function(response) {
				dojo.hitch(this, onLoad)(response, node, perm);
			}),
			error : dojo.hitch(this, function(error, xhr) {
				dojo.hitch(this, onError)(error, xhr, webPageID);
			})
		};
		dojo.xhrPost(xhrArgs);
	}

	function remove(node, perm) {
		var webPageID = node.config.href;
		var url = wcms.settings.wcmsURL + "/access?webPageID=" + webPageID + "&perm=" + perm;
		var xhrArgs = {
			url : url,
			handleAs : "json",
			load : dojo.hitch(this, function(response) {
				dojo.hitch(this, onLoad)(response, node, perm);
			}),
			error : dojo.hitch(this, function(error, xhr) {
				dojo.hitch(this, onError)(error, xhr, webPageID);
			})
		};
		dojo.xhrDelete(xhrArgs);
	}

	function onLoad(response, node, perm) {
		if (response.type == "editDone") {
			console.log("Access changed for " + node.config.title);
			dojo.hitch(this, changeNode)(node, perm, response.edit);
		} else {
			console.log("error: " + response.errorType);
			if (response.errorType == "noPermission") {
				var errorDialog = new wcms.gui.ErrorDialog("component.wcms.access.error.noRightsCaption",
						"component.wcms.access.error.noRightsLabel", "");
				errorDialog.show();
			}
		}
	}

	function onError(error, xhr, webPageID) {
		var statusCode = xhr.xhr.status;
		if (statusCode == 404) {
			var errorDialog = new wcms.gui.ErrorDialog("component.wcms.access.error.cannotDeleteCaption",
					"component.wcms.access.error.cannotDeleteLabel", "");
			errorDialog.show();
		} else {
			wcms.util.ErrorUtils.show();
		}
	}

	function loadRuleList() {
		// load items
		var xhrArgs = {
			url : wcms.settings.wcmsURL + "/access",
			handleAs : "json",
			load : dojo.hitch(this, function(rules) {
				var ruleSet = rules;
				this.ruleEventHandler.notify({
					"type" : "loaded",
					"ruleSet" : ruleSet
				});
			}),
			error : function(error) {
				console.log("error while retrieving ruleList! " + error);
			}
		};
		dojo.xhrGet(xhrArgs);
	}

	function updateLang() {
		this.lang = I18nManager.getInstance().getLang();
		console.log("Language changed to: " + this.lang);
		this.data = [];
		dojo.hitch(this, createTree)();
		this.domNode.set("title", I18nManager.getInstance().getI18nTextAsString("component.wcms.access"));
		if (this.editRuleDialog != undefined) {
			this.editRuleDialog.updateLang();
		}
		dojo.hitch(this, createRemoveDialog)();
	}

	wcms.access.AccessTab.prototype.preload = preload;
	wcms.access.AccessTab.prototype.getPreloadName = getPreloadName;
	wcms.access.AccessTab.prototype.getPreloadWeight = getPreloadWeight;

	wcms.access.AccessTab.prototype.expandAll = expandAll;
	wcms.access.AccessTab.prototype.updateLang = updateLang;
})();