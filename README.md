# Welcome to Timberwolf

Timberwolf is an open-source, command-line program for importing emails from
Microsoft Exchange into Apache HBase. It will locate all emails within an
Exchange instance which it has access to and import them into HBase. By
keeping track of when it was last run, Timberwolf can incrementally update
HBase with new messages.

## Compiling Timberwolf

Compiling Timberwolf requires the following tools:

* Java JDK 1.6
* Apache Maven

To compile Timberwolf, run `mvn compile`.
To build a distribution, run `mvn package`.

The final Timberwolf distribution artifacts will be in $project/target/.

To build a .deb package, run `mvn package -P deb`.  To build a .rpm package,
run `mvn package -P rpm`.  Note that building an RPM package on Ubuntu requires
installing rpm: `sudo apt-get install rpm`.

## Contact us

For questions or comments, you can write and/or subscribe to the 
[Timberwolf mailing list](http://www.ripariandata.com/mailing-list/).

If you would prefer, you can contact us privately at contact@ripariandata.com

## Bug and Issue tracker

All bugs and features are reported and tracked at:
<https://ripariandata.atlassian.net>

## Running Timberwolf

### Setting up an authorization configuration

Timberwolf uses JAAS to perform its authentication against Exchange. This
means you will be required to supply a login configuration entry labeled
"Timberwolf".

We supply a sample_jaas.config file which contains a very basic Kerberos
authentication configuration which may be suitable in your case. You can
specify it either through the following Java command-line option:

    -Djava.security.auth.login.config=sample_jaas.config

(note, this is a command line option to Java, not to the Timberwolf JAR)

...or you can copy the Timberwolf entry in that file into ~/.java.login.config
where Java will detect it automatically.

Information about where Java configuration files can be placed can be found:
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

This will run Timberwolf at and print all located emails to the console. In
order to import the emails into an HBase instance, you must specify the
required HBase arguments:

    java -jar ${project}/target/timberwolf-SNAPSHOT-jar-with-dependencies.jar \
        --exchange-url ${yourExchangeUrl}/ews/exchange.asmx \
        --domain ${yourDomain}
        --hbase-clientport ${portYourHbaseListensOn} \
        --hbase-quorum ${yourHbaseUrl} \
        --hbase-table ${resultingTable}

## Contributing

Submissions are welcome. To submit your changes, fork this repository and do
your work in a topic branch. Once you're done, you can submit a pull request.

### Coding Conventions

The Timberwolf code works off of the Sun Java coding conventions with a few
minor exceptions. Curly braces should always be on their own line and imports
should be ordered alphabetically and broken up by groups.

You can run `mvn checkstyle:checkstyle` in order to confirm whether your
additions conform to the coding convention.

### Running the tests

Before submitting a pull request for approval, you should run the tests with `mvn test`.

There are a number of integrated tests that you can also run, if you'd like,
but they are ignored by default. In order to run the integrated tests, you
must have a working instance of HBase to test against.
There is an additional integrated test which also requires a working instance
of Microsoft Exchange.
The integration tests require a set of properties that must be set in a
`testing.properties` file, see `testing.properties.example` or [the online
documentation](https://github.com/RiparianData/Timberwolf/wiki/Running-the-tests)
for more information.
