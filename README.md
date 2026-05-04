# codeartifact-maven-extension

![maven](https://img.shields.io/maven-central/v/io.dangernoodle/codeartifact-maven-extension)
[![maven-build](https://github.com/dangernoodle-io/codeartifact-maven-extension/actions/workflows/maven-build.yml/badge.svg)](https://github.com/dangernoodle-io/codeartifact-maven-extension/actions/workflows/maven-build.yml)
[![maven-release](https://github.com/dangernoodle-io/codeartifact-maven-extension/actions/workflows/maven-release.yml/badge.svg)](https://github.com/dangernoodle-io/codeartifact-maven-extension/actions/workflows/maven-release.yml)
[![Coverage Status](https://coveralls.io/repos/github/dangernoodle-io/codeartifact-maven-extension/badge.svg?branch=main)](https://coveralls.io/github/dangernoodle-io/codeartifact-maven-extension?branch=main)

Maven `build` extension that provides a [resolver](https://maven.apache.org/resolver/index.html) and 
[wagon](https://maven.apache.org/wagon/index.html) implementation capable of authenticating against 
[AWS CodeArtifact](https://aws.amazon.com/codeartifact/) before uploading/downloading artifacts.

Java 8 or greater is required. The appropriate implementation will automatically be selected based upon your maven
version. See [here](https://maven.apache.org/guides/mini/guide-resolver-transport.html) for additional details.

## Usage

The easiest way to configure the plugin is to add an entry to the `.mvn/extensions.xml` file (see [here](https://maven.apache.org/configure.html)
for more details) like so:

```xml
<extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0 http://maven.apache.org/xsd/core-extensions-1.0.0.xsd">
  <extension>
     <groupId>io.dangernoodle</groupId>
     <artifactId>codeartifact-maven-extension</artifactId>
     <version>${codeartifact-maven-extension.version}</version>
  </extension>
</extensions>
```

This is the recommended approach if you use a parent pom that is also stored within `codeartifact`. If you have a
standalone project and don't wish to use the `.mvn` directory, you can add the following to the build section of 
your project's pom:

```xml

<build>
  <extensions>
    <extension>
      <groupId>io.dangernoodle</groupId>
      <artifactId>codeartifact-maven-extension</artifactId>
      <version>${codeartifact-maven-extension.version}</version>
    </extension>
  </extensions>
  ...
</build>
```

### Credentials

The extension automatically uses the [DefaultCredentialsProvider](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/auth/credentials/DefaultCredentialsProvider.html) to find AWS credentials. Be sure you have 
configured your environment accordingly.

Separately, static credentials can be provided in an a `server` entry in `maven-settings.xml`

```xml

<servers>
  <server>
    <id>codeartifact</id>
    <username>AWS_ACCESS_KEY_ID</username>
    <password>AWS_SECRET_ACCESS_KEY</password>
  </server>
</servers>
```

Make sure you are using appropriate security precautions if you are using static credentials.

### AWS profile

If you don't want to rely on the `DefaultCredentialsProvider` and prefer to authenticate using a named profile from
your AWS shared credentials/config file, you can tell the extension which profile to use. The
[ProfileCredentialsProvider](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/auth/credentials/ProfileCredentialsProvider.html)
is then used instead of the default chain.

The profile can be set in any of the following ways. They are checked in the order listed and the first non-null
value wins:

1. **System property** passed on the command line: `-Ddangernoodle.codeartifact.aws.profile=<profile>`
2. **Environment variable**: `DANGERNOODLE_CODEARTIFACT_AWS_PROFILE=<profile>`
3. **Maven project property** `codeartifact.aws.profile`. This is read from the current Maven project's
   effective properties, so it can be set in the project's `pom.xml`, or in an active profile in your
   user's `settings.xml`. For example:

   ```xml
   <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                                 https://maven.apache.org/xsd/settings-1.0.0.xsd">
       <profiles>
           <profile>
               <id>local</id>
               <properties>
                   <codeartifact.aws.profile>AWSPowerUserAccess</codeartifact.aws.profile>
               </properties>
           </profile>
       </profiles>
       <activeProfiles>
           <activeProfile>local</activeProfile>
       </activeProfiles>
   </settings>
   ```

Static credentials configured via a `<server>` entry (see above) take precedence over any profile resolution. If
none of the profile sources is set and no static credentials are configured, the extension falls back to the
`DefaultCredentialsProvider`.

> AWS profile resolution is only available with the resolver implementation (Maven >= 3.9.0). The wagon
> implementation (Maven < 3.9.0) only supports static credentials via a `<server>` entry.
