# codeartifact-maven-wagon

Maven `http` wagon implementation for [AWS CodeArtifact](https://aws.amazon.com/codeartifact/) that automatically
retrieves the authorization token before uploading/downloading artifacts. Java 8 or greater is required.

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

In addtion to the use of the `DefaultCredentialsProvider` to obtain the proper aws credentials from the environment, 
static credentials can be provided in an a `server` entry in `maven-settings.xml`

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