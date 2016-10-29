# tekniq
A framework designed around Kotlin. Modules include

## tekniq-core
A suite of tools that have no dependencies on any other libraries making
it clean and easy to use without any bloat. It provides such features as

* **Loading Cache** is a cache that supports concurrency, dynamic loading
of content, access/write expirations, and more. It is based around the
original oc4j and guava design concepts but with the kotlin idiom and
advantages of Java 8 technology behind it keeping it very powerful and
efficient.
* **Tracking Carrier Detection** is some implementation logic that
supports determining if a string is a USPS, UPS, FEDex, or other type of
carriers or just a randomly made up string. And provides a link to the
carriers website to pull up full details on the tracking information as
well.
* **Configuration** is yet another configuration concept but this one can
also provide transformations of data for both basic and complex object
types as well in many cases. Also provides a way to merge multiple
sources of configurations into a single interface. Such as merging the
Environmental settings with a database configuration table with a local
code based configuration into a single object which is passed around.

## tekniq-jdbc
Provides a dynamic data source which can be created and utilized at
runtime configuration inputs yet still safely be injected into other
components that need a data source at boot time. For example, the config
settings might be stored in a MongoDB for the various SQL databases that
you are doing ETLs against. You can inject various DataSource to the
code even though it will not provide a connection until it obtains the
information from a central MongoDB server. And if the content on the
server changes, it will update the dynamic data source without a restart
of the application. If a configuration is not provided, the datasource
will not provide connections to the requestor but the ETL can suspend
its particular efforts while other ETL efforts take place.

Also provides other tools for working with SQL databases derived from
https://github.com/andrewoma/kwery

## tekniq-rest
A tool utilizing jackson-mapper for making JSON based RESTful calls to
web services. _(TODO: Place a small example)_

## tekniq-sparklin
A Kotlin HTTP Framework built on top of Spark with a DSL as easy to use
as NodeJS's Express or Ruby's Sinatra. Also provides a nice validation
framework, easy to use authorization management, auto-transformation of
body to complex object types, and more.

_(TODO: Pull in the sparklin project example before deprecating the
sparklin project)_
