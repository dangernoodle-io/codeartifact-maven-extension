# codeartifact-maven-wagon

![maven](https://img.shields.io/maven-central/v/io.dangernoodle/codeartifact-maven-wagon)
![pull-request](https://github.com/dangernoodle-io/codeartifact-maven-wagon/actions/workflows/pull-request.yml/badge.svg)
![coverage](https://coveralls.io/repos/github/dangernoodle-io/codeartifact-maven-wagon/badge.svg?branch=main)

Maven wagon implementation for [AWS CodeArtifact](https://aws.amazon.com/codeartifact/) that automatically
retrieves the authorization token before uploading/downloading artifacts. 

Java 8 or greater is required.

## Usage

Add the following to the build section of your project's pom:

```xml
<build>
  <extensions>
    <extension>
      <groupId>io.dangernoodle</groupId>
      <artifactId>codeartifact-maven-wagon</artifactId>
      <version>${codeartifact-maven-wagon.version}</version>
    </extension>
  </extensions>
  ...
</build>
```

Replace `https` with `codeartifact` in your `repositories` and `distributionMangement` sections to activate the wagon:

```xml
<repositories>
  <repository>
    <id>codeartifact</id>
    <url>codeartifact://domain-account.d.codeartifact.region.amazonaws.com/maven/repository</url>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>
```
```xml
<distributionManagement>   
  <repository>
    <id>codeartifact</id>
    <url>codeartifact://domain-account.d.codeartifact.region.amazonaws.com/maven/repository</url>
  </repository>
  <snapshotRepository>
    <id>codeartifact</id>
    <url>codeartifact://domain-account.d.codeartifact.region.amazonaws.com/maven/repository</url>
  </snapshotRepository>
</distributionManagement>
```

### Credentials

The wagon automatically uses the [DefaultCredentialsProvider](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/auth/credentials/DefaultCredentialsProvider.html)
to find AWS credentials. Be sure you have  configured your environment accordingly.

Separately,static credentials can be provided in an a `server` entry in `maven-settings.xml`

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