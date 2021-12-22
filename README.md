# DDTJ: It kills bugs

[![Build](https://github.com/ddtj/ddtj/actions/workflows/build.yml/badge.svg)](https://github.com/ddtj/ddtj/actions/workflows/build.yml) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ddtj_ddtj&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ddtj_ddtj)  


DDT is the flip side of TDD (Test-driven development). It stands for "Development Driven Tests". Notice that it doesn’t contradict or replace test-driven development, in fact, it’s a perfect complement to it. But let’s start by defining DDT and how it improves production code and code quality.

DDT lets us generate test source code from a running application. 

It’s often very difficult to create a unit test for a complex bug with complex mocking. The goal of DDT is: if you can reproduce it you can unit test it. It provides you with the full set of tools to generate a fully isolated unit test. This, in-turn enables high-quality code, reducing the programming feedback loop.

Run the app, it will auto-generate mock objects and unit test for the method of your choice!

## DDT vs. DDTJ
DDT is the name of the approach (Development Driven Tests) whereas DDTJ is the specific implementation. Notice that there's no detailed specification of DDT, just the separation of terms.

## What DDT Is Not
It isn’t a:
* Replacement for test-driven development - TDD is a software development process. DDT is a tool that produces tests. These tests can join the existing tests in providing greater coverage for test-driven development
* Replacement for integration tests/functional tests/acceptance tests/smoke tests  - Integration tests are essential for a healthy project. The feedback loop with unit tests is faster, so we normally prefer them first. But they are not a replacement!
* It isn’t a “test recorder” - well, technically it is. But not in the classical sense, since it generates unit tests and runs constantly
* A tool for production - this tool affects performance and RAM usage. That isn’t a big deal since we’re running on a development machine. Don’t use it in production code!

There are front end test recording tools that effectively “auto-generate” integration tests. They’re great tools for user interfaces, but they aren’t always ideal for backend developers. 

If you still run into issues when running manual tests, then this might be an interesting option to explore.

## Status
**Currently, DDTJ is under initial development and still isn't functional. Even when it will be functional, this isn't production code yet!**

The current target is Java and Spring Boot. However, the code/design is generic enough to enable support for other languages/platforms. I made all design decisions with portability in mind. If you would like to use this in a different language/platform, please star the project, check the issue tracker and file an RFE though the project issue tracker if one doesn’t exist. 

We’ll try to prioritize support based on GitHub voting and support.

## Usage
**NOTE:** Currently DDTJ requires Java SE 11 or newer.

Download and unzip the DDTJ binary distribution from here (this isn't available yet). In it you will find:
- `backend.jar`
- `ddtj.jar`
 
Run:

```bash
java -jar backend.jar
```

Then run your application with the `ddtj.jar` as such:

```bash
java -jar ddtj.jar -run [-javahome:<path-to-java-home] [-whitelist:regex-whitelist classes] [-arg=<app argument>...] mainClass
```

Once you have enough usage of your app worthy of testing, you can start using the CLI to generate tests:

```bash
java -jar ddtj.jar -list-classes
```

Will print out the list of classes for which there are available unit tests. Sample output looks like:

```
Class Name               Methods                Total Tests
com.mycompany.MyClass     20      1000
…
```

```bash
java -jar ddtj.jar -list-methods <class-name>
```

Will print out the list of methods in the class for which there are available unit tests. Sample output looks like:

```
Method                Total Tests
methodSignature          1000
…
```

```bash
java -jar ddtj.jar -list-tests <class-name> <method-signature>
```

Will print out the list of methods in the class for which there are available unit tests. Sample output looks like:

```
Sampling Time                             Test Id
Dec 12, 2021, 10:00:00.00          37866-3333-333-3333
…
```

```bash
java -jar ddtj.jar -generate 37866-3333-333-3333 -type junit -package com.mycompany.myapp.tests -classname MyMethodTest -methodName testMethod
```

Will print out a unit test class with the junit processor:

```java
package com.mycompany.myapp.tests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest; 
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyMethodTest {
  @Mock
  private UserRepository userRepository;

  @Autowired 
  @InjectMocks
  private MyClass myClass;

  private static final List<User> listOfEntities = new ArrayList();

  @Test
  void testMethod() {
      Mockito.when(userRepository.findAll()).thenReturn(listOfEntities);
      myClass.methodSignature(5, "other string");
  }
}
```

Notice that the mock objects are simplistic in this piece of code and already they encapsulate most of the method.

## Working with the Sources
You can follow the process in the actions script for building the project. Effectively we have three projects:
* Common - common code, mostly data objects
* CLI - command line tool
* Backend - spring boot backend server

To compile everything do:

```bash
cd Common
mvn install
cd ../CLI
mvn package
cd ../Backend
mvn package
```

You can read about the process of building DDTJ in the following blog series: https://dev.to/codenameone/series/15971

## Future Enhancements
An interesting use case is automatic test generation to increase code coverage. When running an application, we can detect code paths that weren't tested and generate unit tests to cover the writing tests gap. This would require integration with testing tools to detect current coverage information.

Currently, I designed DDTJ for use by development teams, but a feature like this might make it suitable for wider audiences.
There's also a plan to create a web interface besides the CLI. 

Future versions of the tools would hopefully be compiled to native code with GraalVM to reduce RAM usage and startup time. A focus of the project is on simple design to enable native compilation.

I already mentioned support for additional languages/platforms above. But we need to support additional unit testing frameworks as well. I designed the system with templating and extensible code to facilitate future growth.

Currently, the design does nothing about code duplication in newly generated tests. This is intentional, as the opposite would include a far more complex public interface to the tool. However, this is something we should reconsider, as duplication of code is a major code smell. 

There's currently no support for programming teams and shared workflow. I'm not sure if there's room for something like that, but it would be interesting. Since DDT collects a lot of data about the system, it can reveal code patterns that can improve the team’s software development practice. This is vague, but possibly an even bigger feature awaits within...
