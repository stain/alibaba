/* ---------------------------------------------------------------------- */
/* Component specific code. */
const RDF = "rdf";
const PROTOCOL_NAME = "Alibaba RDF Protocol";
const RDF_PROTOCOL = "@mozilla.org/network/protocol;1?name=" + RDF;
const PROTOCOL_CID = Components.ID("042d156d-2ebd-4dda-8096-d0f7fae7b029");
const RDF_NAME = "Simple URI that acts like a standard URL";
const RDF_URL = "@mozilla.org/network/standard-url;1?name=" + RDF;
const URL_CID = Components.ID("042d156d-2ebd-4dda-8096-d0f7fae7b028");
const nsILifeCycle = Components.interfaces.nsILifeCycle;
const SERVER_ID = "@alibaba.openrdf.org/jettyServer;1";
// Mozilla defined
const SIMPLE_URI = "@mozilla.org/network/simple-uri;1";
const STANDARD_URL = "@mozilla.org/network/standard-url;1";
const HTTP_PROTOCOL = "@mozilla.org/network/protocol;1?name=http";
const URLTYPE_STANDARD = 1;
const nsISupports = Components.interfaces.nsISupports;
const nsIHttpProtocolHandler = Components.interfaces.nsIHttpProtocolHandler;
const nsIProtocolHandler = Components.interfaces.nsIProtocolHandler;
const nsIURI = Components.interfaces.nsIURI;
const nsIStandardURL = Components.interfaces.nsIStandardURL;
const nsIClassInfo = Components.interfaces.nsIClassInfo;
const nsIURL = Components.interfaces.nsIURL;
const nsISerializable = Components.interfaces.nsISerializable;
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
      if (!spec)
         return baseURI;
      var uri = Components.classes[RDF_URL].createInstance(nsIStandardURL);
      uri.init(1, -1, spec, charset, baseURI);
      uri = uri.QueryInterface(nsIURI);
      return uri;
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
      uri = uri.replace("rdf://http//", "rdf:http://");
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
      var prefManager = Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefBranch);
      if (prefManager.getBoolPref("extensions.alibaba.embed")) {
         var server = Components.classes[SERVER_ID].getService(nsILifeCycle);
         if (!server.isRunning() && !server.isFailed()) {
            server.start();
         }
      }
      return channel
   }
}
function RdfUrl() {
   // constructor
}
RdfUrl.prototype = {
   QueryInterface : function(iid) {
      if (iid.equals(nsISupports))
         return this;
      if (iid.equals(nsIURL))
         return this;
      if (iid.equals(nsIURI))
         return this;
      if (iid.equals(nsIClassInfo))
         return this;
      if (iid.equals(nsIStandardURL))
         return this;
      if (iid.equals(nsISerializable))
         return this;
      throw Components.results.NS_ERROR_NO_INTERFACE;
   },
   getInterfaces : function(aCount) {
      var array = [nsISupports, nsIURL, nsIURI, nsIClassInfo, nsIStandardURL, nsISerializable];
      aCount.value = array.length;
      return array;
   },
   getHelperForLanguage : function(aLanguage) {
      return null;
   },
   asciiHost : 'localhost',
   assciiSpec : null,
   directory : null,
   fileBaseName : null,
   fileExtension : null,
   fileName : null,
   filePath : null,
   host : 'localhost',
   hostPort : -1,
   mutable : false,
   originCharset : null,
   param : null,
   password : null,
   path : null,
   port : -1,
   prePath : null,
   query : null,
   ref : null,
   scheme : RDF,
   spec : null,
   username : null,
   userPass : null,
   init : function(urlType, defaultPort, spec, originCharset, baseURI) {
      this.spec = spec;
      this.assciiSpec = spec;
      this.originCharset = originCharset;
   },
   clone : function() {
     var copy = RdfUrlFactory.createInstance(nsIStandardURL);
     copy.init(1, -1, spec, originCharset, null);
     copy = copy.QueryInterface(nsIURI);
     return copy;
   },
   equals : function(other) {
     this.spec == other.spec;
   },
   getCommonBaseSpec : function(URIToCompare) {
   },
   getRelativeSpec : function(URIToCompare) {
   },
   resolve : function(relativePath) {
   },
   schemeIs : function(scheme) {
      return this.scheme == scheme;
   },
   read : function(inputStream) {
      var spec = inputStream.readString();
      init(1, -1, spec, null, null);
   },
   write : function(outputStream) {
      outputStream.writeStringZ(this.spec);
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
var RdfUrlFactory = {
   createInstance : function (outer, iid) {
      if (outer != null)
         throw Components.results.NS_ERROR_NO_AGGREGATION;
      if (iid.equals(nsISupports) || iid.equals(nsIURL) || iid.equals(nsIURI) ||
            iid.equals(nsISerializable) || iid.equals(nsIClassInfo) || iid.equals(nsIStandardURL))
         return new RdfUrl();
      throw Components.results.NS_ERROR_NO_INTERFACE;
   }
};
var Module = {
   registerSelf : function (compMgr, fileSpec, location, type) {
      compMgr = compMgr.QueryInterface(Components.interfaces.nsIComponentRegistrar);
      compMgr.registerFactoryLocation(PROTOCOL_CID, PROTOCOL_NAME, RDF_PROTOCOL, fileSpec, location, type);
      dump(PROTOCOL_NAME + "\n");
      compMgr.registerFactoryLocation(URL_CID, RDF_NAME, RDF_URL, fileSpec, location, type);
      dump(RDF_NAME + "\n");
   },
   unregisterSelf: function(compMgr, location, type) {
      compMgr = compMgr.QueryInterface(Components.interfaces.nsIComponentRegistrar);
      compMgr.unregisterFactoryLocation(PROTOCOL_CID, location);
      compMgr.unregisterFactoryLocation(URL_CID, location);
   },
   getClassObject : function (compMgr, cid, iid) {
      if (!iid.equals(Components.interfaces.nsIFactory))
         throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
      if (cid.equals(PROTOCOL_CID))
         return ProtocolFactory;
      if (cid.equals(URL_CID))
         return RdfUrlFactory;
      throw Components.results.NS_ERROR_NO_INTERFACE;
   },
   canUnload : function (compMgr) {
      return true;
   }
};
function NSGetModule(aCompMgr, aFileSpec) {
   return Module;
}