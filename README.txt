# Welcome to Timberwolf

Timberwolf is an open-source, command-line service to aid in importing emails
from Microsoft Exchange into Hadoop hbase. It will locate all emails within an
exchange instance which it has access to and upload them into hbase. It also
allows for periodic recynching between Exchange and hbase through periodic
executions of the tool.

## Compiling Timberwolf

Compiling Timberwolf requires the following tools:

* Java JDK 1.6
* Apache Maven

To compile Timberwolf, run `mvn compile`.
To build a distribution, run `mvn package`.

The final Flume distribution artifacts will be in $project/target/.

## Documentation

We don't currently publish documentation.

## Contact us

_Not yet._

## Bug and Issue tracker.

_None._

## Running Timberwolf

### Setting up an authorization configuration

Timberwolf uses JAAS to perform its authentication. This means you will be
required to supply a login configuration entry labeled "Timberwolf".

We supply a sample_jaas.config file which contains a very basic kerberos
authentication configuration which may be suitable in your case. You can
specify it either through the following java commandline option:

    -Djava.security.auth.login.config==sample_jaas.config

(note, this is a command line option to java, not to the Timberwolf jar)

...or you can copy the Timberwolf entry in that file into ~/.java.login.config
where java will detect it automatically.

Information about where java configuration files can be placed can be found:
<http://docs.oracle.com/javase/6/docs/jre/api/security/jaas/spec/com/sun/security/auth/login/ConfigFile.html>

Information about configuration options are available at:
<http://docs.oracle.com/javase/1.4.2/docs/guide/security/jaas/spec/com/sun/security/auth/module/Krb5LoginModule.html>

If you do not set up a "Timberwolf" authorization configuration, you'll get
the error, "Authentication failed: No LoginModules configured for Timberwolf",
when you try to run Timberwolf.

### Ensuring your realm/domain is properly set up

If you run into errors such as "Cannot get kdc for realm...", it's possible
that your domain is not properly configured. Ensure that your /etc/krb5.conf
file and /etc/hosts file correctly describe your realm.

### Executing the JAR

    java -jar ${project}/target/timberwolf-SNAPSHOT-jar-with-dependencies.jar \
        --exchange-url ${yourExchangeUrl}/ews/exchange.asmx \
        --domain ${yourDomain}

This will run Timberwolf at a very basic level and print all located emails to
the console. In order to import the emails into an hbase instance, you must
specify the required hbase arguments:

    java -jar ${project}/target/timberwolf-SNAPSHOT-jar-with-dependencies.jar \
        --exchange-url ${yourExchangeUrl}/ews/exchange.asmx \
        --domain ${yourDomain}
        --hbase-clientport ${portYourHbaseListensOn} \
        --hbase-quorum ${yourHbaseUrl} \
        --hbase-table ${resultingTable}

## Contributing

Timberwolf is an open-source project. Anyone can contribute to Timberwolf by
submitting a patch (MORE DATA NEEDED HERE)

### Coding Conventions

The Timberwolf code works off of the java sun coding conventions with a few
minor exceptions. Curly braces should always be on their own line and imports
should be ordered alphabetically and broken up by groups.

You can run "mvn checkstyle:checkstyle" in order to confirm whether your
additions conform to the coding convention.

### Running the tests

Before submitting a patch for approval, you should run the tests with:
    mvn test

There are a number of integrated tests that you can also run if you'd like,
but they are ignored by default. In order to run the integrated tests, you
must have a working instance of hbase and exchange to test against. And then
you must create the file {project}/testing.properties that specifies the
following properties:

* *ZooKeeperQuorum*: This must specify the machine hosting your zookeeper
  instance.
* *ZooKeeperClientPort*= This specifies the port your zookeeper instance is
  listening on.
* *ExchangeURI* = This specifies the exchange web service you intend to hit.
  It will most like end with ".../ews/exchange.asmx"
* *LdapDomain* = This specifies the domain you're testing on.
* *LdapConfigEntry* = This specifies your jaas login configuration entry you
  intend to use for testing. Likely, you can use the same configuration you
  use for runtime, which is "Timberwolf".

"testing.properties.example" is a sample file you can use to get you started.
Just rename the properties appropriately and rename the file to
"testing.properties". All of these properties directly correspond to command
line arguments that Timberwolf accepts.

Although the integrated tests run against exchange and hbase, they will not
effect any existing data nor will they leave any testing data in hbase.
