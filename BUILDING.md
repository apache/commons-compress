# Building Apache Commons Compress

In order to build Commons Compress a JDK implementation 1.7 or higher
and Apache Maven 3.x are required.

To install the jars into your local Maven repository simply run

    mvn clean install

which will also run the unit tests.

Some tests are only run when specific profiles are enabled, these
tests require a lot of disk space as they test behavior for very large
archives.

    mvn test -Prun-tarit

runs tests for tar archives and requires more than 8GiB of disk space.

    mvn test -Prun-zipit

runs tests for zip archives that require up to 20 GiB of disk
space. In addition the tests will run for a long time (more then ten
minutes, maybe even longer depending on your hardware) and heavily
load the CPU at times.

## Building the Site

The site build produces license release audit (aka RAT) reports as
well as PMD and findbugs reports. Clirr didn't work for us anymore so
we switched to japicmp, the same is true for Cobertura which we had to
replace with jacoco.

japicmp requires the jar to be present when the site is built,
therefore the package goal must be executed before creating the site.

    mvn package site -Pjacoco

builds the site.
