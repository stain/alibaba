// JettyServer.js
const NSINTERFACE = Components.interfaces.nsILifeCycle;
const CID = "62df17fe-7c39-11dc-8314-0800200c9a66";
const NAME = "The Jetty Server XPCOM Component of the alibaba plugin";
const ID = "@alibaba.openrdf.org/jettyServer;1";
const EXT_NAME = "alibaba";
const BOOTSTRAP_JAR = "components/lib/alibaba-firefox.jar";
const LIB_PATH = "components/lib/";
const PREF_ID = "@mozilla.org/preferences-service;1";
const nsIPrefBranch = Components.interfaces.nsIPrefBranch;
 
/*
 *  ATTENTION
 *
 *  Firefox is very very unforgiving to syntax errors in XPCOM Javascript
 *  files. If this file does not parse properly once, it might not be
 *  loaded again even if you restart Firefox. Watch the JavaScript
 *  Console for any error message (only logged the first time the file
 *  fails to parse).
 *
 *  You might have to uninstall the whole extension, restart Firefox, 
 *  reinstall the extension (with fixes), and then restart Firefox.
 */
 
function _printToJSConsole(msg) {
    Components.classes["@mozilla.org/consoleservice;1"]
        .getService(Components.interfaces.nsIConsoleService)
            .logStringMessage(msg);
}

/*----------------------------------------------------------------------
 * The Module (instance factory)
 *----------------------------------------------------------------------
 */

var JettyModule = {
    /*
     *  VERY IMPORTANT: Modify these first 3 fields to make them unique
     *  to your components. Note that these fields have nothing to
     *  do with the extension ID, nor the IDL interface IDs that the
     *  component implements. A component can implement several interfaces.
     */
    _myComponentID : Components.ID(CID),
    _myName :        NAME,
    _myContractID :  ID,
    
    /*
     *  This flag specifies whether this factory will create only a
     *  single instance of the component.
     */
    _singleton :     true,
    _myFactory : {
        createInstance : function(outer, iid) {
            if (outer != null) {
                throw Components.results.NS_ERROR_NO_AGGREGATION;
            }
            
            var instance = null;
            
            if (this._singleton) {
                instance = this.theInstance;
            }
            
            if (!(instance)) {
                instance = new JettyServer(); // JettyServer is declared below
            }
            
            if (this._singleton) {
                this.theInstance = instance;
            }

            return instance.QueryInterface(iid);
        }
    },
    
    registerSelf : function(compMgr, fileSpec, location, type) {
        compMgr = compMgr.QueryInterface(Components.interfaces.nsIComponentRegistrar);
        compMgr.registerFactoryLocation(
            this._myComponentID,
            this._myName,
            this._myContractID,
            fileSpec,
            location,
            type
        );
        dump(this._myName + "\n");
    },

    unregisterSelf : function(compMgr, fileSpec, location) {
        var server = this._myFactory.theInstance;
        if (server && server.isRunning()) {
            server.stop();
        }
        compMgr = compMgr.QueryInterface(Components.interfaces.nsIComponentRegistrar);
        compMgr.unregisterFactoryLocation(this._myComponentID, fileSpec);
    },

    getClassObject : function(compMgr, cid, iid) {
        if (cid.equals(this._myComponentID)) {
            return this._myFactory;
        } else if (!iid.equals(Components.interfaces.nsIFactory)) {
            throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
        }

        throw Components.results.NS_ERROR_NO_INTERFACE;
    },

    canUnload : function(compMgr) {
        /*
         *  Do any unloading task you want here
         */
        return true;
    }
}

/*
 *  This function NSGetModule will be called by Firefox to retrieve the
 *  module object. This function has to have that name and it has to be
 *  specified for every single .JS file in the components directory of
 *  your extension.
 */
function NSGetModule(compMgr, fileSpec) {
    return JettyModule;
}




/*----------------------------------------------------------------------
 * The Jettyt Server, implemented as a Javascript function.
 *----------------------------------------------------------------------
 */

function JettyServer() {
    /*
     *  This is a XPCOM-in-Javascript trick: Clients using an XPCOM
     *  implemented in Javascript can access its wrappedJSObject field
     *  and then from there, access its Javascript methods that are
     *  not declared in any of the IDL interfaces that it implements.
     *
     *  Being able to call directly the methods of a Javascript-based
     *  XPCOM allows clients to pass to it and receive from it
     *  objects of types not supported by IDL.
     */
    this.wrappedJSObject = this;
    
    this._initialized = false;
    this._packages = null;
    this.server = null;
    this.java = null;
}

/*
 *  nsISupports.QueryInterface
 */
JettyServer.prototype.QueryInterface = function(iid) {
    /*
     *  This code specifies that the component supports 2 interfaces:
     *  nsIHelloWorld and nsISupports.
     */
    if (!iid.equals(NSINTERFACE) &&
        !iid.equals(Components.interfaces.nsISupports)) {
        throw Components.results.NS_ERROR_NO_INTERFACE;
    }
    return this;
};

/*
 *  Initializes this component, including loading JARs.
 */
JettyServer.prototype.initialize = function (trace) {
    if (this._initialized) {
        return true;
    }
    
    this._traceFlag = (trace);
    
    this._trace("JettyServer.initialize {");
    try {
        var java = this.java;

        var extensionPath = this._getExtensionPath(EXT_NAME);
        
        /*
         *  Bootstrap our class loading mechanism
         */
        this._bootstrapClassLoader(java, extensionPath);
        
        /*
         *  Enumerate URLs to our JARs and class directories
         */
        var libPath = extensionPath + LIB_PATH;
        var jarFilenames = java.io.File(java.net.URL(libPath).getFile()).list();
        
        var jarFilepaths = [];
        for (var i = 0; i < jarFilenames.length; i++) {
            jarFilepaths.push(libPath + jarFilenames[i]);
        }
        var prefManager = Components.classes[PREF_ID].getService(nsIPrefBranch);
        var classpath = prefManager.getCharPref("extensions.alibaba.classpath").split(';');
        for (var i = 0; i < classpath.length; i++) {
            if (classpath[i]) {
                jarFilepaths.push("file://" + classpath[i]);
            }
        }
        
        /*
         *  Load them up!
         */
        this._packages = this._loadJava(java, jarFilepaths);
        
        /*
         *  Create Jetty Server object
         */
        var factory = this._packages.getClass("org.openrdf.alibaba.firefox.JettyServerFactory").n();
        this.server = factory.createServer();
         
        this._initialized = true;
    } catch (e) {
        this._fail(e);
        this._trace(this.error);
        var msg;
        if (e.getMessage) {
            msg = e + ": " + e.getMessage() + "\n";
            var ex = e;
            while (ex.getCause() != null) {
                ex = ex.getCause();
                msg += "caused by " + ex + ": " + ex.getMessage() + "\n";
            }
        } else {
            msg = e;
        }
       dump(msg + "\n");
       throw e;
    }
    this._trace("} JettyServer.initialize");
    
    return this._initialized;
};

/*
 * LifeCycle interface
 */
JettyServer.prototype.isFailed = function () {
    if (this.server)
      return this.server.isFailed();
};
JettyServer.prototype.isRunning = function () {
    if (this.server)
      return this.server.isRunning();
};
JettyServer.prototype.isStarted = function () {
    if (this.server)
      return this.server.isStarted();
};
JettyServer.prototype.isStarting = function () {
    if (this.server)
      return this.server.isStarting();
};
JettyServer.prototype.isStopped = function () {
    if (this.server)
      return this.server.isStopped();
};
JettyServer.prototype.isStopping = function () {
    if (this.server)
      return this.server.isStopping();
};
JettyServer.prototype.start = function () {
    this.initialize(false);
    dump("Starting Jetty...");
    this.server.start();
    dump("Jetty Started.\n");
};
JettyServer.prototype.stop = function () {
    if (this.server) {
      dump("Stopping Jetty...");
      this.server.stop();
      dump("Jetty Stopped.\n");
    }
};

/*
 *  Returns the packages of all the JARs that this component has loaded.
 */
JettyServer.prototype.getPackages = function() {
    this._trace("JettyServer.getPackages");
    return this._packages;
};

/*
 *  Returns the Test object instantiated by default.
 */
JettyServer.prototype.getTest = function() {
    this._trace("JettyServer.getTest");
    return this._test;
};



JettyServer.prototype._fail = function(e) {
    if (e.getMessage) {
        this.error = e + ": " + e.getMessage() + "\n";
        while (e.getCause() != null) {
            e = e.getCause();
            this.error += "caused by " + e + ": " + e.getMessage() + "\n";
        }
    } else {
        this.error = e;
    }
};

JettyServer.prototype._bootstrapClassLoader = function(java, extensionPath) {
    if (this._bootstraped) {
        return;
    }
    
    this._trace("JettyServer._bootstrapClassLoader {");
    
    var firefoxClassLoaderURL = 
        new java.net.URL(extensionPath + BOOTSTRAP_JAR);
    
    /*
     *  Step 1. Load the bootstraping firefoxClassLoader jar.
     */
    var bootstrapClassLoader = java.net.URLClassLoader.newInstance([ firefoxClassLoaderURL ]);
    
    /*
     *  Step 2. Instantiate a Policy object from firefoxClassLoader jar.
     */
    var policyClass = java.lang.Class.forName(
        "org.openrdf.alibaba.firefox.LocalTrustPolicy",
        true,
        bootstrapClassLoader
    );
    var policy = policyClass.newInstance();
    
    /*
     *  Step 3. Now, the trick: We wrap our own Policy around
     *  the current security policy of the JVM security manager. This
     *  allows us to give our own Java code whatever permission we
     *  want, even though Firefox doesn't give us any permission.
     */
    java.security.Policy.setPolicy(policy);
        
    /*
     *  That's pretty much it for the security bootstraping hack. But we want to
     *  do a little more. We want our own class loader for subsequent JARs that
     *  we load.
     */
     
    /*
     *  Step 4. Reload firefoxClassLoader jar and so we can make use of
     *  TracingClassLoader. We need to reload it because when it was
     *  loaded previously, we had not yet set the policy to give it
     *  enough permission for loading classes.
     */
    
    var firefoxClassLoaderPackages = new WrappedPackages(
        java,
        java.net.URLClassLoader.newInstance([ firefoxClassLoaderURL ])
    );
    var tracingClassLoaderClass = 
        firefoxClassLoaderPackages.getClass("edu.mit.simile.firefoxClassLoader.TracingClassLoader");

    /*
     *  That's it. These are the only 3 things we
     *  need for loading more code.
     */
    this._firefoxClassLoaderURL = firefoxClassLoaderURL;
    this._policy = policy;
    this._tracingClassLoaderClass = tracingClassLoaderClass;
    
    this._bootstraped = true;
    
    this._trace("} JettyServer._bootstrapClassLoader");
};

JettyServer.prototype._loadJava = function(java, jarURLStrings) {
    this._trace("JettyServer._loadJava {");

    var jarURLs = [];
    
    /*
     *  We include the firefoxClassLoader jar again whenever we
     *  load more JARs so that we can use various reflection
     *  utility classes in firefoxClassLoader jar on these
     *  JARs.
     */
     jarURLs.push(this._firefoxClassLoaderURL);
    
    /*
     *  Now we add the rest of the JARs.
     */
    for (var i = 0; i < jarURLStrings.length; i++) {
        var jarURL = new java.net.URL(jarURLStrings[i]);
        jarURLs.push(jarURL);
    }
        
    /*
     *  Create a new TracingClassLoader
     */
    var classLoader = this._tracingClassLoaderClass.m("newInstance")(this._traceFlag);
    
    /*
     *  Give it the JARS
     */
    for (var i = 0; i < jarURLs.length; i++) {
        classLoader.add(jarURLs[i]);
    }
    java.lang.Thread.currentThread().setContextClassLoader(classLoader);
    
    /*
     *  Wrap up the class loader and return
     */
    var packages = new WrappedPackages(java, classLoader);
    
    this._trace("} JettyServer._loadJava");
    
    return packages;
}

JettyServer.prototype._trace = function (msg) {
    if (this._traceFlag) {
        _printToJSConsole(msg);
    }
}

/*
 *  Get the file path to the installation directory of this 
 *  extension.
 */
JettyServer.prototype._getExtensionPath = function(extensionName) {
    var chromeRegistry =
        Components.classes["@mozilla.org/chrome/chrome-registry;1"]
            .getService(Components.interfaces.nsIChromeRegistry);
            
    var uri =
        Components.classes["@mozilla.org/network/standard-url;1"]
            .createInstance(Components.interfaces.nsIURI);
    
    uri.spec = "chrome://" + extensionName + "/content/";
    
    var path = chromeRegistry.convertChromeURL(uri);
    if (typeof(path) == "object") {
        path = path.spec;
    }
    
    if (path.indexOf("/content/") > 0) {
        path = path.substring(0, path.indexOf("/content/") + 1);
    }
    if (path.indexOf("/chrome/") > 0) {
        path = path.substring(0, path.indexOf("/chrome/") + 1);
    }
    
    return path;
};
    
/*
 *  Retrieve the file path to the user's profile directory.
 *  We don't really use it here but it might come in handy
 *  for you.
 */
JettyServer.prototype._getProfilePath = function() {
    var fileLocator =
        Components.classes["@mozilla.org/file/directory_service;1"]
            .getService(Components.interfaces.nsIProperties);
    
    var path = escape(fileLocator.get("ProfD", Components.interfaces.nsIFile).path.replace(/\\/g, "/")) + "/";
    if (path.indexOf("/") == 0) {
        path = 'file://' + path;
    } else {
        path = 'file:///' + path;
    }
    
    return path;
};


/*
 *  Wraps a class loader and allows easy access to the classes that it loads.
 */
function WrappedPackages(java, classLoader) {
    var packages = java.lang.Class.forName(
        "edu.mit.simile.firefoxClassLoader.Packages",
        true,
        classLoader
    ).newInstance();
    
    var argumentsToArray = function(args) {
        var a = [];
        for (var i = 0; i < args.length; i++) {
            a[i] = args[i];
        }
        return a;
    }

    this.getClass = function(className) {
        var classWrapper = packages.getClass(className);
        if (classWrapper) {
            return {
                n : function() {
                    return classWrapper.callConstructor(argumentsToArray(arguments));
                },
                f : function(fieldName) {
                    return classWrapper.getField(fieldName);
                },
                m : function(methodName) {
                    return function() {
                        return classWrapper.callMethod(methodName, argumentsToArray(arguments));
                    };
                }
            };
        } else {
            return null;
        }
    };
    
    this.setTracing = function(enable) {
        classLoader.setTracing((enable) ? true : false);
    };
}
