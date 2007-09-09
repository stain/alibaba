/* ---------------------------------------------------------------------- */
/* Component specific code. */
const RDF = "rdf";
const kPROTOCOL_NAME = "Alibaba RDF Protocol";
const RDF_PROTOCOL = "@mozilla.org/network/protocol;1?name=" + RDF;
const CID = Components.ID("042d156d-2ebd-4dda-8096-d0f7fae7b029");
// Mozilla defined
const SIMPLE_URI = "@mozilla.org/network/simple-uri;1";
const HTTP_PROTOCOL = "@mozilla.org/network/protocol;1?name=http";
const nsISupports = Components.interfaces.nsISupports;
const nsIHttpProtocolHandler = Components.interfaces.nsIHttpProtocolHandler;
const nsIProtocolHandler = Components.interfaces.nsIProtocolHandler;
const nsIURI = Components.interfaces.nsIURI;
/* ---------------------------------------------------------------------- */
function Protocol() {
   // constructor
}
Protocol.prototype = {
   QueryInterface : function(iid) {
      if (!iid.equals(nsIProtocolHandler) && !iid.equals(nsISupports))
         throw Components.results.NS_ERROR_NO_INTERFACE;
      return this;
   },
   scheme : RDF,
   defaultPort : - 1,
   protocolFlags : nsIProtocolHandler.URI_NORELATIVE | nsIProtocolHandler.URI_NOAUTH,
   allowPort : function(port, scheme) {
      return false;
   },
   newURI : function(spec, charset, baseURI) {
      if (RDF == spec.substring(0, spec.indexOf(':'))) {
         var uri = Components.classes[SIMPLE_URI].createInstance(nsIURI);
         uri.spec = spec;
         return uri;
      }
      else {
         var finalURL = this.decode(baseURI);
         /* create dummy nsIURI and nsIChannel instances */
         var http = Components.classes[HTTP_PROTOCOL] .getService(nsIHttpProtocolHandler);
         return http.newURI(spec, charset, http.newURI(finalURL, charset, null));
      }
   },
   decode : function(input_uri) {
      var url = [];
      var prefManager = Components.classes["@mozilla.org/preferences-service;1"] .getService(Components.interfaces.nsIPrefBranch);
      var hostUrl = prefManager.getCharPref("extensions.alibaba.host-url");
      dump("[RdfProtocol] hostUrl=" + hostUrl + "\n");
      url.push(hostUrl);
      // input_uri is a nsIUri, so get a string from it using .spec
      var uri = input_uri.spec;
      // strip away the rdf: part
      uri = uri.substring(uri.indexOf(":") + 1, uri.length);
      dump("[RdfProtocol] uri=" + uri + "\n");
      url.push(encodeURIComponent(uri));
      var format = prefManager.getCharPref("extensions.alibaba.format");
      if (format) {
         dump("[RdfProtocol] accept=" + format + "\n");
         url.push('&accept=');
         url.push(encodeURIComponent(format));
      }
      var intent = prefManager.getCharPref("extensions.alibaba.intent");
      if (intent) {
         dump("[RdfProtocol] intent=" + intent + "\n");
         url.push('&intent=');
         url.push(encodeURIComponent(intent));
      }
      var finalURL = url.join('');
      dump("[RdfProtocol] finalURL=" + finalURL + "\n");
      return finalURL;
   },
   newChannel : function(input_uri) {
      var finalURL = this.decode(input_uri);
      /* create dummy nsIURI and nsIChannel instances */
      var http = Components.classes[HTTP_PROTOCOL].getService(nsIHttpProtocolHandler);
      var channel = http.newChannel(http.newURI(finalURL, null, null), null, null);
      var httpChannel = channel.QueryInterface(Components.interfaces.nsIHttpChannel);
      httpChannel.setRequestHeader("X-RdfProtocol", "true", false);
      return channel
   }
}
var ProtocolFactory = {
   createInstance : function (outer, iid) {
      if (outer != null)
         throw Components.results.NS_ERROR_NO_AGGREGATION;
      if (!iid.equals(nsIProtocolHandler) && !iid.equals(nsISupports))
         throw Components.results.NS_ERROR_NO_INTERFACE;
      return new Protocol();
   }
};
var Module = {
   registerSelf : function (compMgr, fileSpec, location, type) {
      compMgr = compMgr.QueryInterface(Components.interfaces.nsIComponentRegistrar);
      compMgr.registerFactoryLocation(CID, kPROTOCOL_NAME, RDF_PROTOCOL, fileSpec, location, type);
   },
   unregisterSelf: function(compMgr, location, type) {
      compMgr = compMgr.QueryInterface(Components.interfaces.nsIComponentRegistrar);
      compMgr.unregisterFactoryLocation(CID, location);
   },
   getClassObject : function (compMgr, cid, iid) {
      if (!cid.equals(CID))
         throw Components.results.NS_ERROR_NO_INTERFACE;
      if (!iid.equals(Components.interfaces.nsIFactory))
         throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
      return ProtocolFactory;
   },
   canUnload : function (compMgr) {
      return true;
   }
};
function NSGetModule(aCompMgr, aFileSpec) {
   return Module;
}