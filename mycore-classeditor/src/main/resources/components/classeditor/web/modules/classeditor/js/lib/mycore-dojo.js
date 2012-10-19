//>>built
require({cache:{"mycore/mycore-dojo-all":function(){define(["./common/common-all","./dijit/dijit-all","./util/util-all"],function(){console.warn("mycore-dojo-all may include much more code than your application actually requires. We strongly recommend that you investigate a custom build or the web build tool");return{}})},"mycore/common/common-all":function(){define("mycore/common/common-all","./CompoundEdit,./EventDelegator,./I18nStore,./I18nResolver,./I18nManager,./Preloader,./UndoableEdit,./UndoableMergeEdit,./UndoManager".split(","),
function(){console.warn("common-all may include much more code than your application actually requires. We strongly recommend that you investigate a custom build or the web build tool");return{}})},"mycore/common/CompoundEdit":function(){define("mycore/common/CompoundEdit",["dojo/_base/declare","mycore/common/UndoableEdit"],function(f,g){return f("mycore.common.CompoundEdit",g,{edits:null,constructor:function(e){this.edits=[];f.safeMixin(this,e)},addEdit:function(e){this.edits.push(e)},undo:function(){for(var e=
this.edits.length-1;0<=e;e--)this.edits[e].undo()},redo:function(){for(var e=0;e<this.edits.length;e++)this.edits[e].redo()}})})},"mycore/common/UndoableEdit":function(){define("mycore/common/UndoableEdit",["dojo/_base/declare"],function(f){return f("mycore.common.UndoableEdit",null,{undoManager:null,getLabel:function(){return"no label defined"},undo:function(){},redo:function(){}})})},"mycore/common/EventDelegator":function(){define("mycore/common/EventDelegator",["dojo/_base/declare","dojo/Evented",
"dojo/on","dojo/_base/lang","mycore/util/DOJOUtil"],function(f,g,e,b,a){return f("mycore.common.EventDelegator",g,{delegate:!0,event:"change",source:null,objects:null,_signals:null,_objectsToBlock:null,_fireAfterLastBlock:!1,constructor:function(a){if(a.source)this.objects={},this._signals={},this._objectsToBlock=[],f.safeMixin(this,a)},startDelegation:function(){this.delegate=!0},stopDelegation:function(){this.delegate=!1},register:function(a,d){this.objects[a]=d;this._signals[a]=e(d,this.event,
b.hitch({instance:this,id:a},this._handleEvent))},unregister:function(a){delete this.objects[a];this._signals[a].remove();delete this._signals[a];delete this._objectsToBlock[a]},block:function(b){"string"===typeof b?this._objectsToBlock.push(b):"[object Array]"===Object.prototype.toString.call(b)?this._objectsToBlock=this._objectsToBlock.concat(b):console.error("Invalid argument: call block() with json or string "+b);this._objectsToBlock=a.arrayUnique(this._objectsToBlock,[])},getEventObject:function(){return{}},
_handleEvent:function(a){var d=this.instance._objectsToBlock.indexOf(this.id);if(-1!=d){if(this.instance._objectsToBlock.splice(d,1),this.instance._fireAfterLastBlock&&0==this.instance._objectsToBlock.length)this.instance._fireAfterLastBlock=!1,b.hitch(this.instance,this.instance.fire)(a)}else this.instance.delegate&&b.hitch(this.instance,this.instance.fire)(a)},fire:function(a){e.emit(this.source,this.event,this.getEventObject(a))},fireAfterLastBlock:function(){this._fireAfterLastBlock=!0}})})},
"mycore/util/DOJOUtil":function(){define("mycore/util/DOJOUtil",["exports","dojo/_base/declare","dojo/_base/array"],function(f,g,e){f.isWidget=function(b){return"object"===typeof b&&void 0!=b.baseClass&&void 0!=b.declaredClass};f.isWidgetClass=function(b,a){return this.isWidget(b)&&b.declaredClass==a};f.instantiate=function(b,a){var c,d=window;c=b.split(".");for(var h=0;h<c.length;h++)if(d=d[c[h]],void 0==d)throw Error("Undefined class "+b);c=function(){};c.prototype=d.prototype;c=new c;d.apply(c,
a);c.constructor=d;return c};f.arrayUnique=function(b,a){if(null==b||null==a)return null==b&&null==a?null:null==b?a:b;var c=[],d=b.concat(a);e.forEach(d,function(a){null==a||-1<e.indexOf(c,a)||c.push(a)});return c};f.arrayEqual=function(b,a){return!!b&&!!a&&!(b<a||a<b)};f.deepEqual=function(b,a){function c(a,j){if(typeof a!=typeof j)return!1;if("function"==typeof a||"object"==typeof a){var b=0,i;for(i in a)b++;for(i in j)b--;if(0!=b)return!1;for(var n in a)if(d=c(a[n],j[n]),!d)return!1;return d}return a==
j}var d=!0;return c(b,a)}})},"mycore/common/I18nStore":function(){define("mycore/common/I18nStore",["dojo/_base/declare","dojo/_base/lang","dojo/_base/xhr"],function(f,g,e){return f("mycore.common.I18nStore",null,{cache:null,url:"",constructor:function(b){this.cache=[];f.safeMixin(this,b)},fetch:function(b,a){void 0==this.cache[b]&&(this.cache[b]={});var c={url:this.url+b+"/"+a+"*",sync:!0,handleAs:"json",load:g.hitch(this,function(a){for(var c in a)this.cache[b][c]=a[c]}),error:function(){}};e.get(c)},
mixin:function(b,a){var a=void 0===a?!0:a,c;for(c in b)if(this.cache[c])for(var d in b[c]){if(a||!this.cache[c][d])this.cache[c][d]=b[c][d]}else this.cache[c]=b[c]},_getI18nTextFromCache:function(b){if(!b.language)throw console.error("Undefined language"),"Undefined language";if(!b.label)throw console.error("Undefined label"),"Undefined label";if(void 0==this.cache[b.language])throw b="There are no i18n texts for language '"+b.language+"' defined!",console.error(b),b;return this.cache[b.language][b.label]},
getI18nTextFromCache:function(b){var a=this._getI18nTextFromCache(b);return void 0==a?"undefined ('"+b.label+"')":a},getI18nText:function(b){if(!b.load)throw console.error("Undefined load method"),"Undefined load method";var a=this._getI18nTextFromCache(b);void 0!=a?b.load(a,b.callbackData):this.get18nTextFromServer(b)},get18nTextFromServer:function(b){var a={url:this.url+b.language+"/"+b.label,load:g.hitch(this,function(a){this.cache[language][label]=a;b.load&&b.load(a,b.callbackData)}),error:function(a){b.error?
b.error(a,b.callbackData):console.error("error while retrieving i18n text:")}};e.get(a)}})})},"mycore/common/I18nResolver":function(){define("mycore/common/I18nResolver","dojo/_base/declare,dojo/dom-attr,dojo/dom-construct,mycore/util/DOMUtil,mycore/util/DOJOUtil,dijit/Tooltip".split(","),function(f,g,e,b,a){return f("mycore.common.I18nResolver",null,{store:null,constructor:function(a){f.safeMixin(this,a)},resolve:function(c,d){b.isNode(d)?this.resolveNode(c,d):a.isWidget(d)?a.isWidgetClass(d,"dijit.form.Select")?
this.resolveSelect(c,d):a.isWidgetClass(d,"dijit.form.ValidationTextBox")?this.resolveValidationTextBox(c,d):a.isWidgetClass(d,"dijit.form.CheckBox")?this.resolveCheckBox(c,d):this.resolveWidget(c,d):console.error("Cannot resolve object:")},resolveNode:function(a,b){this.store.getI18nText({language:a,label:g.get(b,"i18n"),load:function(a){b.innerHTML=a}})},resolveWidget:function(a,b){var h=b.get("i18n");h&&this.store.getI18nText({language:a,label:h,load:function(a){b.set("label",a)}});if(b.getChildren)for(var h=
b.getChildren(),j=0;j<h.length;j++)this.resolveWidget(a,h[j])},resolveValidationTextBox:function(a,b){b.i18nPromptMessage&&this.store.getI18nText({language:a,label:b.i18nPromptMessage,load:function(a){b.set("promptMessage",a)}});b.i18nInvalidMessage&&this.store.getI18nText({language:a,label:b.i18nInvalidMessage,load:function(a){b.set("invalidMessage",a)}});b.i18nMissingMessage&&this.store.getI18nText({language:a,label:b.i18nMissingMessage,load:function(a){b.set("missingMessage",a)}})},resolveSelect:function(a,
b){for(var h=b.getOptions(),j=0;j<h.length;j++){var e=h[j],i=e.i18n?e.i18n:null;null!=i&&this.store.getI18nText({language:a,label:i,callbackData:{select:b,option:e},load:function(a,b){b.option.label=a;b.select.updateOption(b.option)}})}},resolveCheckBox:function(a,b){this.store.getI18nText({language:a,label:b.get("i18n"),load:function(a){a=e.create("label",{"for":b.get("id"),innerHTML:a});e.place(a,b.domNode,"after")}})},resolveTooltip:function(a,b){this.store.getI18nText({language:a,label:b.i18nTooltip,
load:function(a){(new dijit.Tooltip({label:a})).addTarget(b.domNode)}})}})})},"mycore/util/DOMUtil":function(){define("mycore/util/DOMUtil","exports,dojo/_base/declare,dojo/_base/lang,dojo/Deferred,dojo/dom-construct,dojo/query,dojo/NodeList-manipulate".split(","),function(f,g,e,b,a,c){f.isNode=function(a){return"object"===typeof Node?a instanceof Node:a&&"object"===typeof a&&"number"===typeof a.nodeType&&"string"===typeof a.nodeName};f.isElement=function(a){return"object"===typeof HTMLElement?a instanceof
HTMLElement:a&&"object"===typeof a&&1===a.nodeType&&"string"===typeof a.nodeName};f.loadCSS=function(d){var h=new b,j=a.create("link",{rel:"stylesheet",type:"text/css",href:d,onload:function(){h.resolve("success")},onerror:function(a){h.reject({error:a,href:d})}});c("head").append(j);return h.promise};f.updateBodyTheme=function(a){null==a&&(a="claro");c("body").forEach(function(a){dojo.attr(a,"class","claro")})}})},"dojo/NodeList-manipulate":function(){define("dojo/NodeList-manipulate",["./query",
"./_base/lang","./_base/array","./dom-construct","./NodeList-dom"],function(f,g,e,b){function a(b){for(var c="",b=b.childNodes,i=0,d;d=b[i];i++)8!=d.nodeType&&(c=1==d.nodeType?c+a(d):c+d.nodeValue);return c}function c(a){for(;a.childNodes[0]&&1==a.childNodes[0].nodeType;)a=a.childNodes[0];return a}function d(a,c){"string"==typeof a?(a=b.toDom(a,c&&c.ownerDocument),11==a.nodeType&&(a=a.childNodes[0])):1==a.nodeType&&a.parentNode&&(a=a.cloneNode(!1));return a}var h=f.NodeList;g.extend(h,{_placeMultiple:function(a,
c){for(var i="string"==typeof a||a.nodeType?f(a):a,d=[],h=0;h<i.length;h++)for(var e=i[h],g=this.length,k=g-1,l;l=this[k];k--)0<h&&(l=this._cloneNode(l),d.unshift(l)),k==g-1?b.place(l,e,c):e.parentNode.insertBefore(l,e),e=l;d.length&&(d.unshift(0),d.unshift(this.length-1),Array.prototype.splice.apply(this,d));return this},innerHTML:function(a){return arguments.length?this.addContent(a,"only"):this[0].innerHTML},text:function(j){if(arguments.length){for(var c=0,i;i=this[c];c++)1==i.nodeType&&(b.empty(i),
i.appendChild(i.ownerDocument.createTextNode(j)));return this}for(var d="",c=0;i=this[c];c++)d+=a(i);return d},val:function(a){if(arguments.length){for(var b=g.isArray(a),i=0,c;c=this[i];i++){var d=c.nodeName.toUpperCase(),h=c.type,f=b?a[i]:a;if("SELECT"==d){d=c.options;for(h=0;h<d.length;h++){var k=d[h];k.selected=c.multiple?-1!=e.indexOf(a,k.value):k.value==f}}else"checkbox"==h||"radio"==h?c.checked=c.value==f:c.value=f}return this}if((c=this[0])&&1==c.nodeType){a=c.value||"";if("SELECT"==c.nodeName.toUpperCase()&&
c.multiple){a=[];d=c.options;for(h=0;h<d.length;h++)k=d[h],k.selected&&a.push(k.value);a.length||(a=null)}return a}},append:function(a){return this.addContent(a,"last")},appendTo:function(a){return this._placeMultiple(a,"last")},prepend:function(a){return this.addContent(a,"first")},prependTo:function(a){return this._placeMultiple(a,"first")},after:function(a){return this.addContent(a,"after")},insertAfter:function(a){return this._placeMultiple(a,"after")},before:function(a){return this.addContent(a,
"before")},insertBefore:function(a){return this._placeMultiple(a,"before")},remove:h.prototype.orphan,wrap:function(a){if(this[0])for(var a=d(a,this[0]),b=0,i;i=this[b];b++){var h=this._cloneNode(a);i.parentNode&&i.parentNode.replaceChild(h,i);c(h).appendChild(i)}return this},wrapAll:function(a){if(this[0]){a=d(a,this[0]);this[0].parentNode.replaceChild(a,this[0]);for(var a=c(a),b=0,i;i=this[b];b++)a.appendChild(i)}return this},wrapInner:function(a){if(this[0])for(var a=d(a,this[0]),b=0;b<this.length;b++){var c=
this._cloneNode(a);this._wrap(g._toArray(this[b].childNodes),null,this._NodeListCtor).wrapAll(c)}return this},replaceWith:function(a){for(var a=this._normalize(a,this[0]),b=0,c;c=this[b];b++)this._place(a,c,"before",0<b),c.parentNode.removeChild(c);return this},replaceAll:function(a){for(var a=f(a),b=this._normalize(this,this[0]),c=0,d;d=a[c];c++)this._place(b,d,"before",0<c),d.parentNode.removeChild(d);return this},clone:function(){for(var a=[],b=0;b<this.length;b++)a.push(this._cloneNode(this[b]));
return this._wrap(a,this,this._NodeListCtor)}});if(!h.prototype.html)h.prototype.html=h.prototype.innerHTML;return h})},"mycore/common/I18nManager":function(){define("mycore/common/I18nManager",["dojo/_base/declare","dojo/cookie","dojo/_base/json","mycore/common/I18nStore","mycore/common/I18nResolver"],function(f,g,e){f=f("mycore.common.I18nManager",[],{store:null,resolver:null,language:null,languages:null,constructor:function(){this.language=g("i18n.language")?g("i18n.language"):"de";this.languages=
g("i18n.languages")?e.fromJson(g("i18n.languages")):["de","en"]},init:function(a){this.store=a;this.resolver=new mycore.common.I18nResolver({store:this.store})},setLanguage:function(a){this.language=a;g("i18n.language",this.language,{expires:365})},setLanguages:function(a){this.languages=a;g("i18n.languages",e.toJson(this.languages),{expires:365})},getLanguage:function(){return this.language},getLanguages:function(){return this.languages},fetch:function(a){this.store.fetch(this.language,a)},get:function(a){if(!a.language)a.language=
this.language;this.store.getI18nText(a)},getFromCache:function(a){return this.store.getI18nTextFromCache({language:this.language,label:a})},resolve:function(a){this.resolver.resolve(this.language,a)},resolveTooltip:function(a){this.resolver.resolveTooltip(this.language,a)}});if(!b)var b=new f;return b})},"mycore/common/Preloader":function(){define("mycore/common/Preloader",["dojo/_base/declare","dojo/Evented","dojo/on","dojo/_base/lang"],function(f,g,e,b){return f("mycore.common.Preloader",g,{list:null,
_totalWeight:0,_currentWeight:0,constructor:function(a){this.list=[];f.safeMixin(this,a)},preload:function(){var a=this.list.length;e.emit(this,"started",{size:a});for(var c=this._totalWeight=this._currentWeight=0;c<a;c++){var d=this.list[c];if(!d.getPreloadWeight){console.error("No preload weight defined for:");return}if(!d.preload){console.error("Object is not preloadable:");return}d.getPreloadName?this._totalWeight+=d.getPreloadWeight():console.warn("Warning: no preload name defined for:")}for(c=
0;c<a;c++)d=this.list[c],e.emit(this,"preloadObject",{name:d.getPreloadName()}),d.preload(b.hitch({instance:this,object:d},this._onLoad))},_onLoad:function(){if(!this.instance||!this.object)console.error("Invalid scope of _onLoad:");else{this.instance._currentWeight+=this.object.getPreloadWeight();var a=100*(this.instance._currentWeight/this.instance._totalWeight);this.instance.list.splice(this.instance.list.indexOf(this.object),1);e.emit(this.instance,"preloadObjectFinished",{name:this.object.getPreloadName(),
progress:a});0==this.instance.list.length&&e.emit(this.instance,"finished")}}})})},"mycore/common/UndoableMergeEdit":function(){define("mycore/common/UndoableMergeEdit",["dojo/_base/declare","dojo/_base/lang","mycore/common/UndoableEdit"],function(f,g,e){return f("mycore.common.UndoableMergeEdit",e,{timeout:1E3,_timeoutMilli:null,constructor:function(b){this._timeoutMilli=(new Date).getTime();f.safeMixin(this,b)},merge:function(){},isAssociated:function(b){var a=!(this._timeoutMilli+this.timeout<
b._timeoutMilli);this._timeoutMilli=b._timeoutMilli;return a}})})},"mycore/common/UndoManager":function(){define("mycore/common/UndoManager",["dojo/_base/declare","dojo/on"],function(f,g){return f("mycore.common.UndoManager",null,{pointer:-1,limit:20,list:null,onExecute:!1,blockEvent:!1,_forceNoMergeSwitch:!1,constructor:function(e){this.list=[];f.safeMixin(this,e)},add:function(e){if(this.pointer<this.list.length-1)this.list=this.list.slice(0,this.pointer+1);if(this.list.length>=this.limit)this.list=
this.list.slice(1),this.pointer--;var b=this.list[this.pointer];!this._forceNoMergeSwitch&&null!=b&&e.merge&&b.merge&&b.isAssociated(e)?(b.merge(e),g.emit(this,"merged",{mergedEdit:e,undoableEdit:b})):(this.list.push(e),e.undoManager=this,this.pointer++,g.emit(this,"add",{undoableEdit:e}));this._forceNoMergeSwitch=!1},canUndo:function(){return 0<=this.pointer},canRedo:function(){return this.pointer<this.list.length-1},undo:function(){if(this.canUndo())this.onExecute=!0,this.list[this.pointer].undo(),
this.onExecute=!1,this.pointer--,this.forceNoMerge(),g.emit(this,"undo")},redo:function(){if(this.canRedo())this.pointer++,this.onExecute=!0,this.list[this.pointer].redo(),this.onExecute=!1,this.forceNoMerge(),g.emit(this,"redo")},forceNoMerge:function(){this._forceNoMergeSwitch=!0}})})},"mycore/dijit/dijit-all":function(){define("mycore/dijit/dijit-all","./AbstractDialog,./PlainButton,./ExceptionDialog,./I18nRow,./Preloader,./Repeater,./RepeaterRow,./SimpleDialog,./TextRow".split(","),function(){console.warn("dijit-all may include much more code than your application actually requires. We strongly recommend that you investigate a custom build or the web build tool");
return{}})},"mycore/dijit/AbstractDialog":function(){define("mycore/dijit/AbstractDialog","dojo/_base/declare,dijit/Dialog,dojo/on,dojo/_base/lang,dojo/dom-class,dojo/dom-construct,dojo/dom-style,dijit/form/Button,mycore/common/I18nStore,mycore/common/I18nResolver".split(","),function(f,g,e,b,a,c,d){return f("mycore.dijit.AbstractDialog",g,{Type:{ok:"ok",cancel:"cancel",okCancel:"okCancel",yesNo:"yesNo",yesNoCancel:"yesNoCancel"},defaultI18nCache:{de:{"mycore.dijit.dialog.ok":"Ok","mycore.dijit.dialog.cancel":"Abbruch",
"mycore.dijit.dialog.yes":"Ja","mycore.dijit.dialog.no":"Nein"},en:{"mycore.dijit.dialog.ok":"Ok","mycore.dijit.dialog.cancel":"Cancel","mycore.dijit.dialog.yes":"Yes","mycore.dijit.dialog.no":"No"}},i18nStore:null,i18nTitle:null,type:null,internalDialog:null,content:null,language:null,okButton:null,cancelButton:null,yesButton:null,noButton:null,additionalData:null,created:!1,constructor:function(a){this.language="de";this.i18nTitle="undefined";this.type=this.Type.ok;a.i18nStore?a.i18nStore.mixin(this.defaultI18nCache):
this.i18nStore=new mycore.common.I18nStore({cache:this.defaultI18nCache});f.safeMixin(this,a)},setTitle:function(a){this.i18nTitle=a;this.updateTitle(this.language)},getTitle:function(){return this.internalDialog.get("title")},_create:function(){this.internalDialog=new dijit.Dialog;this.okButton=new dijit.form.Button({i18n:"mycore.dijit.dialog.ok"});this.cancelButton=new dijit.form.Button({i18n:"mycore.dijit.dialog.cancel"});this.yesButton=new dijit.form.Button({i18n:"mycore.dijit.dialog.yes"});this.noButton=
new dijit.form.Button({i18n:"mycore.dijit.dialog.no"});a.add(this.internalDialog.domNode,"mycoreDialog");this.content=c.create("div");a.add(this.content,"content");this.internalDialog.set("content",this.content);var d=c.create("div");a.add(d,"controls");c.place(d,this.internalDialog.domNode);this.type==this.Type.ok?d.appendChild(this.okButton.domNode):this.type==this.Type.cancel?d.appendChild(this.cancelButton.domNode):this.type==this.Type.okCancel?(d.appendChild(this.cancelButton.domNode),d.appendChild(this.okButton.domNode)):
this.type==this.Type.yesNo?(d.appendChild(this.noButton.domNode),d.appendChild(this.yesButton.domNode)):this.type==this.Type.yesNoCancel&&(d.appendChild(this.cancelButton.domNode),d.appendChild(this.noButton.domNode),d.appendChild(this.yesButton.domNode));e(this.okButton,"click",b.hitch(this,function(){this.onBeforeOk();this.internalDialog.hide();this.onOk()}));e(this.cancelButton,"click",b.hitch(this,function(){this.onBeforeCancel();this.internalDialog.hide();this.onCancel()}));e(this.yesButton,
"click",b.hitch(this,function(){this.onBeforeYes();this.internalDialog.hide();this.onYes()}));e(this.noButton,"click",b.hitch(this,function(){this.onBeforeNo();this.internalDialog.hide();this.onNo()}));this.created=!0},show:function(){this.created||(this._create(),this.createContent&&(this.createContent(),this.updateLang(this.language)));this.beforeShow&&this.beforeShow();this.internalDialog.show()},updateLang:function(a){this.language=a;if(this.created){var b=new mycore.common.I18nResolver({store:this.i18nStore});
b.resolve(a,this.okButton);b.resolve(a,this.cancelButton);b.resolve(a,this.yesButton);b.resolve(a,this.noButton);this.updateTitle(a)}},updateTitle:function(a){this.i18nTitle&&this.i18nStore.getI18nText({language:a,label:this.i18nTitle,load:b.hitch(this,function(a){this.internalDialog.set("title",a)})})},setWidth:function(a){d.set(this.content,"width",a+"px")},onBeforeOk:function(){},onBeforeCancel:function(){},onBeforeYes:function(){},onBeforeNo:function(){},onOk:function(){},onCancel:function(){},
onYes:function(){},onNo:function(){}})})},"mycore/dijit/PlainButton":function(){require({cache:{"url:mycore/dijit/templates/PlainButton.html":'<a class="plain-button" role="presentation">\n\t<span data-dojo-attach-point="titleNode,focusNode" role="button" aria-labelledby="${id}_label"\n\t\tdata-dojo-attach-event="ondijitclick:_onClick">\n\t\t<span data-dojo-attach-point="iconNode" class="plain-icon"></span>\n\t\t<span data-dojo-attach-point="containerNode" class="plain-label">${label}</span>\n\t</span>\n\t<input ${!nameAttrSetting} type="${type}" value="${value}" class="dijitOffScreen"\n\t\t\ttabIndex="-1" role="presentation" data-dojo-attach-point="valueNode"/>\n</a>\n'}});
define("mycore/dijit/PlainButton","dojo/_base/declare,dijit/form/Button,dojo/text!./templates/PlainButton.html,dojo/on,dojo/_base/lang,dojo/dom-construct,dojo/dom-class,dojo/dom-style".split(","),function(f,g,e,b,a,c,d){return f("mycore.dijit.PlainButton",[g],{templateString:e,baseClass:"plainButton",constructor:function(a){f.safeMixin(this,a)},_setDisabledAttr:function(a){this.inherited(arguments);a?d.add(this.iconNode,"plain-button-disabled"):d.remove(this.iconNode,"plain-button-disabled")}})})},
"url:mycore/dijit/templates/PlainButton.html":'<a class="plain-button" role="presentation">\n\t<span data-dojo-attach-point="titleNode,focusNode" role="button" aria-labelledby="${id}_label"\n\t\tdata-dojo-attach-event="ondijitclick:_onClick">\n\t\t<span data-dojo-attach-point="iconNode" class="plain-icon"></span>\n\t\t<span data-dojo-attach-point="containerNode" class="plain-label">${label}</span>\n\t</span>\n\t<input ${!nameAttrSetting} type="${type}" value="${value}" class="dijitOffScreen"\n\t\t\ttabIndex="-1" role="presentation" data-dojo-attach-point="valueNode"/>\n</a>\n',
"mycore/dijit/ExceptionDialog":function(){define("mycore/dijit/ExceptionDialog","dojo/_base/declare,mycore/dijit/SimpleDialog,dojo/dom-class,dojo/dom-construct,dojo/dom-attr,dojo/dom-style,dojo/on,dojo/_base/lang".split(","),function(f,g,e,b){return f("mycore.dijit.ExceptionDialog",g,{exception:null,exceptionI18nCache:{de:{"mycore.dijit.exceptionDialog.title":"Fehler","mycore.dijit.exceptionDialog.text":"Es ist ein Fehler aufgetreten, bitte kontaktieren Sie Ihren Administrator."},en:{"mycore.dijit.exceptionDialog.title":"Exception",
"mycore.dijit.exceptionDialog.text":"An error occur, please contact your administrator."}},constructor:function(a){null==a.exception?console.error("No exception given in args"):(this.i18nStore.mixin(this.exceptionI18nCache,!1),this.i18nTitle="mycore.dijit.exceptionDialog.title",this.i18nText="mycore.dijit.exceptionDialog.text",f.safeMixin(this,a))},createContent:function(){this.inherited(arguments);this.content.appendChild(b.create("div",{style:"color: red",innerHTML:"<p>"+("["+this.exception.lineNumber+
"] "+this.exception.fileName)+"<br />"+this.exception.message+"</p>"}))}})})},"mycore/dijit/SimpleDialog":function(){define("mycore/dijit/SimpleDialog","dojo/_base/declare,mycore/dijit/AbstractDialog,dojo/dom-class,dojo/dom-construct,dojo/dom-attr,dojo/dom-style,dojo/on,dojo/_base/lang".split(","),function(f,g,e,b,a,c,d,h){return f("mycore.dijit.SimpleDialog",g,{i18nText:"undefined",imageURL:null,textTd:null,imageElement:null,setText:function(a){this.i18nText=a;this.updateText(this.language)},setImage:function(a){this.image=
a;this.updateImage()},createContent:function(){var a=b.create("table"),c=b.create("tr"),d=b.create("td");this.textTd=b.create("td");this.imageElement=b.create("img",{style:"padding-right: 10px;"});this.content.appendChild(a);a.appendChild(c);c.appendChild(d);c.appendChild(this.textTd);d.appendChild(this.imageElement);this.setImage(this.imageURL);this.setText(this.i18nText)},updateText:function(b){this.i18nText&&this.i18nStore.getI18nText({language:b,label:this.i18nText,load:h.hitch(this,function(b){a.set(this.textTd,
{innerHTML:b})})})},updateImage:function(){null==this.imageURL?c.set(this.imageElement,"display","none"):c.set(this.imageElement,"display","block");a.set(this.imageElement,{src:this.imageURL})},updateLang:function(a){this.inherited(arguments);this.created&&this.updateText(a)}})})},"mycore/dijit/I18nRow":function(){require({cache:{"url:mycore/dijit/templates/I18nRow.html":'<tr class="${baseClass}">\n\t<td class="content lang">\n\t\t<select data-dojo-attach-point="lang" data-dojo-type="dijit.form.Select"></select>\n\t</td>\n\t<td class="content text">\n\t\t<input data-dojo-attach-point="text" data-dojo-type="dijit.form.TextBox" data-dojo-props="intermediateChanges: true"/>\n\t</td>\n\t<td class="content description">\n\t\t<textarea data-dojo-attach-point="description" data-dojo-type="dijit.form.Textarea" data-dojo-props="intermediateChanges: true"></textarea>\n\t</td>\n\t<\!-- add other table columns here --\>\n\t<td class="control remove" data-dojo-attach-point="control">\n\t\t<button data-dojo-attach-point="removeRow" data-dojo-type="mycore.dijit.PlainButton"\n\t\t\t\tdata-dojo-props="showLabel: false, iconClass:\'icon-cancel\'"></button>\n\t</td>\n</tr>\n'}});
define("mycore/dijit/I18nRow","dojo/_base/declare,mycore/dijit/RepeaterRow,dijit/_Templated,dojo/text!./templates/I18nRow.html,dojo/on,dojo/_base/lang,dojo/dom-construct,mycore/util/DOJOUtil,dijit/form/TextBox,dijit/form/Select,dijit/form/Textarea,mycore/common/EventDelegator".split(","),function(f,g,e,b){return f("mycore.dijit.I18nRow",[g,e],{templateString:b,widgetsInTemplate:!0,eventDelegator:null,baseClass:"i18nRow",constructor:function(a){f.safeMixin(this,a)},create:function(a){this.inherited(arguments);
this._setLanguages(a.languages?a.languages:["de"]);this.set("value",a.initialValue);this.eventDelegator=new mycore.common.EventDelegator({source:this,delegate:!1,getEventObject:dojo.hitch(this,function(){return{row:this,value:this.get("value")}})});this.eventDelegator.register("lang",this.lang);this.eventDelegator.register("text",this.text);this.eventDelegator.register("description",this.description);setTimeout(dojo.hitch(this,function(){this.eventDelegator.startDelegation()}),1)},_setValueAttr:function(a,
b){null!=a&&(a=this._normalize(a),this.equals(a)||(null!=this.eventDelegator&&(this.lang.get("value")!=a.lang&&this.eventDelegator.block("lang"),this.text.get("value")!=a.text&&this.eventDelegator.block("text"),this.description.get("value")!=a.description&&this.eventDelegator.block("description"),(b||void 0===b)&&this.eventDelegator.fireAfterLastBlock()),this.containsLanguage(a.lang)||this.lang.addOption({value:a.lang,label:a.lang}),this.lang.set("value",a.lang),this.text.set("value",a.text),this.description.set("value",
a.description)))},_getValueAttr:function(){return{lang:this.lang.get("value"),text:this.text.get("value"),description:this.description.get("value")}},_setDisabledAttr:function(a){this.lang.set("disabled",a);this.text.set("disabled",a);this.description.set("disabled",a);this.inherited(arguments)},_setLanguages:function(a){for(var b=this.lang.get("value"),d=this.lang.getOptions(),e=[],f=0;f<a.length;f++)e.push({value:a[f],label:a[f]});null!=this.eventDelegator&&this.eventDelegator.block("lang");this.lang.removeOption(d);
this.lang.addOption(e);""!=b&&!this.containsLanguage(b)&&this.lang.addOption({value:b,label:b});this.lang.set("value",b)},receive:function(a){a.id&&"resetLang"==a.id&&a.languages&&this._setLanguages(a.languages)},_normalize:function(a){return{lang:a.lang?a.lang:null,text:a.text?a.text:"",description:a.description?a.description:""}},equals:function(a){a=this._normalize(a);return this.lang.get("value")==a.lang&&this.text.get("value")==a.text&&this.description.get("value")==a.description},containsLanguage:function(a){for(var b=
this.lang.getOptions(),d=0;d<b.length;d++)if(b[d].value==a)return!0;return!1}})})},"mycore/dijit/RepeaterRow":function(){require({cache:{"url:mycore/dijit/templates/RepeaterRow.html":'<tr class="${baseClass}">\n\t<\!-- add other table columns here --\>\n\t<td class="control remove" data-dojo-attach-point="control">\n\t\t<button data-dojo-attach-point="removeRow" data-dojo-type="mycore.dijit.PlainButton"\n\t\t\t\tdata-dojo-props="showLabel: false, iconClass:\'icon-cancel\'"></button>\n\t</td>\n</tr>\n'}});
define("mycore/dijit/RepeaterRow","dojo/_base/declare,dijit/_Widget,dijit/_Templated,dojo/Evented,dojo/text!./templates/RepeaterRow.html,dojo/on,dojo/_base/lang,dojo/dom-construct,dojo/dom-class,dojo/dom-style,dojo/json,dijit/form/Button".split(","),function(f,g,e,b,a,c,d,h,j,m){return f("mycore.dijit.RepeaterRow",[g,e,b],{templateString:a,widgetsInTemplate:!0,disabled:!1,baseClass:"mycoreRepeaterRow",_repeater:null,initialValue:null,removeable:!0,constructor:function(a){null==a._repeater?console.error("No repeater set for this row. You should call addRow() in your repeater to create a row."):
f.safeMixin(this,a)},create:function(){this.inherited(arguments);c(this.removeRow,"click",d.hitch(this,this._onRemove))},addColumn:function(a){var b=h.create("td");h.place(a,b);h.place(b,this.control,"before");return b},getRepeater:function(){return this._repeater},_onRemove:function(){c.emit(this,"remove",{row:this})},_setDisabledAttr:function(a){this.disabled=a;this.removeRow.set("disabled",a);c.emit(this,"disable",{row:this,disabled:a})},_setRemovableAttr:function(a){this.removable=a;m.set(this.removeRow.domNode,
"display",a?"block":"none")},_setValueAttr:function(){},_getValueAttr:function(){},equals:function(){return!1},receive:function(){}})})},"dijit/_Templated":function(){define("dijit/_Templated","./_WidgetBase,./_TemplatedMixin,./_WidgetsInTemplateMixin,dojo/_base/array,dojo/_base/declare,dojo/_base/lang,dojo/_base/kernel".split(","),function(f,g,e,b,a,c,d){c.extend(f,{waiRole:"",waiState:""});return a("dijit._Templated",[g,e],{widgetsInTemplate:!1,constructor:function(){d.deprecated(this.declaredClass+
": dijit._Templated deprecated, use dijit._TemplatedMixin and if necessary dijit._WidgetsInTemplateMixin","","2.0")},_attachTemplateNodes:function(a,d){this.inherited(arguments);for(var e=c.isArray(a)?a:a.all||a.getElementsByTagName("*"),i=c.isArray(a)?0:-1;i<e.length;i++){var f=-1==i?a:e[i],g=d(f,"waiRole");g&&f.setAttribute("role",g);(g=d(f,"waiState"))&&b.forEach(g.split(/\s*,\s*/),function(a){-1!=a.indexOf("-")&&(a=a.split("-"),f.setAttribute("aria-"+a[0],a[1]))})}}})})},"url:mycore/dijit/templates/RepeaterRow.html":'<tr class="${baseClass}">\n\t<\!-- add other table columns here --\>\n\t<td class="control remove" data-dojo-attach-point="control">\n\t\t<button data-dojo-attach-point="removeRow" data-dojo-type="mycore.dijit.PlainButton"\n\t\t\t\tdata-dojo-props="showLabel: false, iconClass:\'icon-cancel\'"></button>\n\t</td>\n</tr>\n',
"url:mycore/dijit/templates/I18nRow.html":'<tr class="${baseClass}">\n\t<td class="content lang">\n\t\t<select data-dojo-attach-point="lang" data-dojo-type="dijit.form.Select"></select>\n\t</td>\n\t<td class="content text">\n\t\t<input data-dojo-attach-point="text" data-dojo-type="dijit.form.TextBox" data-dojo-props="intermediateChanges: true"/>\n\t</td>\n\t<td class="content description">\n\t\t<textarea data-dojo-attach-point="description" data-dojo-type="dijit.form.Textarea" data-dojo-props="intermediateChanges: true"></textarea>\n\t</td>\n\t<\!-- add other table columns here --\>\n\t<td class="control remove" data-dojo-attach-point="control">\n\t\t<button data-dojo-attach-point="removeRow" data-dojo-type="mycore.dijit.PlainButton"\n\t\t\t\tdata-dojo-props="showLabel: false, iconClass:\'icon-cancel\'"></button>\n\t</td>\n</tr>\n',
"mycore/dijit/Preloader":function(){require({cache:{"url:mycore/dijit/templates/Preloader.html":'<div class="${baseClass}" role="progressbar" tabindex="-1">\n\t<div data-dojo-attach-point="progressBar" data-dojo-type="dijit.ProgressBar"></div>\n\t<div data-dojo-attach-point="text" class="text">${text}</div>\n    <div data-dojo-attach-point="containerNode"></div>\n</div>'}});define("mycore/dijit/Preloader","dojo/_base/declare,dijit/_Widget,dijit/_Templated,dojo/text!./templates/Preloader.html,dojo/_base/lang,dojo/on,dijit/ProgressBar".split(","),
function(f,g,e,b,a,c){return f("mycore.dijit.Preloader",[g,e],{templateString:b,widgetsInTemplate:!0,baseClass:"mycorePreloader",preloader:null,showText:!1,text:"",constructor:function(a){a.preloader||console.error("No preloader defined. e.g. new mycore.dijit.Preloader({preloader: new mycore.common.Preloader()})");f.safeMixin(this,a)},create:function(){this.inherited(arguments);this.updateText();c(this.preloader,"preloadObjectFinished",a.hitch(this,function(a){this.progressBar.update({maximum:100,
progress:a.progress});this.updateText()}))},updateText:function(){if(this.showText){for(var a="",b=this.preloader.list,c=0;c<b.length;c++)a+=b[c].getPreloadName(),a+=c+1!=b.length?", ":"";this.text.innerHTML=0==a.length?"":"["+a+"]"}}})})},"url:mycore/dijit/templates/Preloader.html":'<div class="${baseClass}" role="progressbar" tabindex="-1">\n\t<div data-dojo-attach-point="progressBar" data-dojo-type="dijit.ProgressBar"></div>\n\t<div data-dojo-attach-point="text" class="text">${text}</div>\n    <div data-dojo-attach-point="containerNode"></div>\n</div>',
"mycore/dijit/Repeater":function(){require({cache:{"url:mycore/dijit/templates/Repeater.html":'<div class="${baseClass}" role="" tabindex="-1">\n\t<table class="contentNode">\n\t\t<tbody data-dojo-attach-point="tableBody">\n\t\t\t<tr data-dojo-attach-point="addNode">\n\t\t\t\t<td class="control add">\n\t\t\t\t\t<button\tdata-dojo-attach-point="addRowButton" data-dojo-type="mycore.dijit.PlainButton"\n\t\t\t\t\t\t\tdata-dojo-props="showLabel: false, iconClass:\'icon-plus\'"></button>\n\t\t\t\t</td>\n\t\t\t</tr>\n\t\t</tbody>\n\t</table>\n    <div data-dojo-attach-point="containerNode"></div>\n</div>\n'}});
define("mycore/dijit/Repeater","dojo/_base/declare,dijit/_Widget,dijit/_Templated,dojo/Evented,dojo/text!./templates/Repeater.html,dojo/on,dojo/_base/lang,dojo/dom-construct,dojo/dom-class,mycore/util/DOJOUtil,mycore/dijit/RepeaterRow,mycore/dijit/PlainButton".split(","),function(f,g,e,b,a,c,d,h,j,m){return f("mycore.dijit.Repeater",[g,e,b],{templateString:a,widgetsInTemplate:!0,baseClass:"mycoreRepeater",row:null,disabled:!1,_rows:null,minOccurs:0,head:null,constructor:function(a){!a.row||!a.row.className?
console.error("No row class is given. Create e.g. with {row: {class: 'my.sample.className'}}"):(this._rows=[],f.safeMixin(this,a))},create:function(){this.inherited(arguments);for(var a=0;a<this.minOccurs;a++)this._addRow({disabled:this.disabled});c(this.addRowButton,"click",d.hitch(this,this._onAdd))},_setValueAttr:function(a,b){for(var c=!1,d=0,e=0;e<a.length;e++){if(d<this._rows.length){var f=this._rows[d];f.equals(a[e])||(f.set("value",a[e],!1),c=!0)}else c=!0,this._addRow({initialValue:a[e],
disabled:this.disabled});d++}for(;this._rows.length>d;)c=!0,this._removeRow(this._rows[this._rows.length-1]);c&&(b||void 0===b)&&setTimeout(dojo.hitch(this,function(){this._onChange()}),1)},_getValueAttr:function(){for(var a=[],b=0;b<this._rows.length;b++)a.push(this._rows[b].get("value"));return a},addRow:function(a){a=this._addRow(a);this._onChange();return a},_addRow:function(a){var b=this.row.args?d.clone(this.row.args):{};d.mixin(b,a);if(null==b.removable)b.removable=this._rows.length>=this.minOccurs;
b._repeater=this;a=m.instantiate(this.row.className,[b]);c(a,"remove",d.hitch(this,this._onRemove));c(a,"change",d.hitch(this,this._onChange));h.place(a.domNode,this.addNode,"before");this._rows.push(a);return a},removeRow:function(a){this._removeRow(a);this._onChange()},_removeRow:function(a){var b=this.indexOf(a);this._rows.splice(b,1);a.destroy();return b},_setHeadAttr:function(a){null!=this.head&&h.destroy(this.head);this.head=a;h.place(this.head,this.tableBody,"first")},_onAdd:function(){this.addRow()},
_onRemove:function(a){this.removeRow(a.row)},_onChange:function(){c.emit(this,"change",{source:this})},_setDisabledAttr:function(a){this.disabled=a;for(var b=0;b<this._rows.length;b++)this._rows[b].set("disabled",a);this.addRowButton.set("disabled",a);c.emit(this,"disable",{disabled:a})},indexOf:function(a){return this._rows.indexOf(a)},broadcast:function(a){for(var b=0;b<this._rows.length;b++)this._rows[b].receive(a)}})})},"url:mycore/dijit/templates/Repeater.html":'<div class="${baseClass}" role="" tabindex="-1">\n\t<table class="contentNode">\n\t\t<tbody data-dojo-attach-point="tableBody">\n\t\t\t<tr data-dojo-attach-point="addNode">\n\t\t\t\t<td class="control add">\n\t\t\t\t\t<button\tdata-dojo-attach-point="addRowButton" data-dojo-type="mycore.dijit.PlainButton"\n\t\t\t\t\t\t\tdata-dojo-props="showLabel: false, iconClass:\'icon-plus\'"></button>\n\t\t\t\t</td>\n\t\t\t</tr>\n\t\t</tbody>\n\t</table>\n    <div data-dojo-attach-point="containerNode"></div>\n</div>\n',
"mycore/dijit/TextRow":function(){define("mycore/dijit/TextRow","dojo/_base/declare,mycore/dijit/RepeaterRow,dojo/on,dojo/_base/lang,dojo/dom-construct,dijit/form/TextBox".split(","),function(f,g){return f("mycore.dijit.TextRow",[g],{baseClass:"textRow",textBox:null,eventDelegator:null,constructor:function(e){this.inherited(arguments);this.textBox=new dijit.form.TextBox({value:this.initialValue,style:"width: 100%",intermediateChanges:!0});this.eventDelegator=new mycore.common.EventDelegator({source:this,
delegate:!1,getEventObject:dojo.hitch(this,function(){return{row:this,value:this.get("value")}})});this.eventDelegator.register("text",this.textBox);setTimeout(dojo.hitch(this,function(){this.eventDelegator.startDelegation()}),1)},create:function(){this.inherited(arguments);this.addColumn(this.textBox.domNode)},_setValueAttr:function(e,b){b||void 0===b||this.eventDelegator.block("text");this.textBox.set("value",e)},_getValueAttr:function(){return this.textBox.get("value")},_setDisabledAttr:function(e){this.textBox.set("disabled",
e);this.inherited(arguments)},equals:function(e){return this.textBox.get("value")==e}})})},"mycore/util/util-all":function(){define("mycore/util/util-all",["./DOJOUtil","./DOMUtil"],function(){console.warn("util-all may include much more code than your application actually requires. We strongly recommend that you investigate a custom build or the web build tool");return{}})}}});define("mycore/mycore-dojo",[],1);