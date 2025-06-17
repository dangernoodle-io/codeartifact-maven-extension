# Changelog

- **0.4.0** (2025.06.17)
  - Allow access using assumed roles
- **0.3.1** (2025.05.13)
  - Fixed dependencies
- **0.3.0** (2025.05.13)
  - Use IAM identity center SSO authentication to obtain codeartifact auth tokens
- **0.2.4** (2024.09.17)
  - Added support for codeartifact domains that contain hyphens
  - Added 'maven-resolver-transport-http' implementation for http 
  - Simplified wagon use, dropped `codeartifact://` prefix
  - Renamed artifact to `codeartifact-maven-extension`
  - Note: versions `0.2.0`-`0.2.3` had release pipeline failures
- **0.1.1** (2023.03.09)
  - Improved test coverage 
  - Added Javadoc
- **0.1.0** (2023.03.08)
  - Initial release as `codeartifact-maven-wagon`