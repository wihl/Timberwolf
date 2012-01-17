h1. Welcome to Timberwolf

Timberwolf is an open-source, command-line service to aid in importing emails from Microsoft Exchange into Hadoop hbase. It will locate all emails within an exchange instance which it has access to and upload them into hbase. It also allows for periodic recynching between Exchange and hbase through periodic executions of the tool.

h2. Documentation

We don't have any documentation so there you go.

h2. Contact us

Not yet.

h2. Bug and Issue tracker.

None.

h2. Compiling Timberwolf

Compiling Timberwolf requires the following tools:

* Java JDK 1.6
* Apache Maven

To compile Timberwolf, run `mvn compile`.
To build a distribution, run `mvn package`.

The final Flume distribution artifacts will be in $project/target/.

h2. Running Timberwolf

h3. Setting up an authorization configuration

Timberwolf uses JAAS to perform its authentication. This means you will be required to supply a login configuration entry labeled "Timberwolf".

We supply a sample_jaas.config file which contains a very basic kerberos authentication configuration which may be suitable in your case. You can specify it either through the following java commandline option:

-Djava.security.auth.login.config==sample_jaas.config

(note, this is a command line option to java, not to the Timberwolf jar)

or you can copy the Timberwolf entry in that file into ~/.java.login.config where java will detect it automatically.

Information about where java configuration files can be placed can be found:
http://docs.oracle.com/javase/6/docs/jre/api/security/jaas/spec/com/sun/security/auth/login/ConfigFile.html

Information about configuration options are available at:
http://docs.oracle.com/javase/1.4.2/docs/guide/security/jaas/spec/com/sun/security/auth/module/Krb5LoginModule.html

If you do not set up a "Timberwolf" authorization configuration, you'll get the error, "Authentication failed: No LoginModules configured for Timberwolf", when you try to run Timberwolf.

h3. Ensuring your realm/domain is properly set up

If you run into errors such as "Cannot get kdc for realm...", it's possible that your domain is not properly configured. Ensure that your /etc/krb5.conf file and /etc/hosts file correctly describe your realm.

h3. Executing the JAR

java -jar ${project}/target/timberwolf-SNAPSHOT-jar-with-dependencies.jar \
    --exchange-url ${yourExchangeUrl}/ews/exchange.asmx \
    --domain ${yourDomain}

will run Timberwolf at a very basic level and print all located emails to the console.

MORE STUFF WILL NEED TO GO HERE. LIKE ALL THE HBASE STUFF.
