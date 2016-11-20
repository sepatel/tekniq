# tekniq
A framework designed around Kotlin. Modules include

## [tekniq-core](https://github.com/sepatel/tekniq/tree/master/tekniq-core)
A suite of tools that have no dependencies on other libraries providing
capabilities such as loading cache, configurations, tracking tools, and
more.

**Gradle**
```
compile "io.tekniq:tekniq-core:0.3.1"
```

**Maven Dependency**
```
<dependency>
    <groupId>io.tekniq</groupId>
    <artifactId>tekniq-core</artifactId>
    <version>0.3</version>
</dependency>
```

## [tekniq-jdbc](https://github.com/sepatel/tekniq/tree/master/tekniq-jdbc)
Provides extensions to the DataSource and Connection objects allowing
one to more cleanly and easily work with the JDBC APIs with the kotlin
idiom supported. Does not require overhead of object mappings or such.

**Gradle**
```
compile "io.tekniq:tekniq-jdbc:0.3.1"
```

**Maven Dependency**
```
<dependency>
    <groupId>io.tekniq</groupId>
    <artifactId>tekniq-jdbc</artifactId>
    <version>0.3</version>
</dependency>
```

## [tekniq-rest](https://github.com/sepatel/tekniq/tree/master/tekniq-rest)
A tool utilizing jackson-mapper for making RESTful calls to web services.

**Gradle**
```
compile "io.tekniq:tekniq-rest:0.3.1"
```

**Maven Dependency**
```
<dependency>
    <groupId>io.tekniq</groupId>
    <artifactId>tekniq-rest</artifactId>
    <version>0.3</version>
</dependency>
```

## [tekniq-sparklin](https://github.com/sepatel/tekniq/tree/master/tekniq-sparklin)
A Kotlin HTTP Framework built on top of Spark with a DSL as easy to use
as NodeJS's Express or Ruby's Sinatra. Also provides a nice validation
framework, easy to use authorization management, auto-transformation of
body to complex object types, and more.

**Gradle**
```
compile "io.tekniq:tekniq-sparklin:0.3.1"
```

**Maven Dependency**
```
<dependency>
    <groupId>io.tekniq</groupId>
    <artifactId>tekniq-sparklin</artifactId>
    <version>0.3</version>
</dependency>
```

