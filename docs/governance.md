# Governance

[Maxim Rusanov (@rusanoph)](https://github.com/rusanoph) is the maintainer and release owner. Use GitHub Issues for support, bug reports and roadmap proposals. Report security issues through the private channel. [Security policy](../SECURITY.md)

The maintainer reviews pull requests for correctness, compatibility and evidence. A change that breaks an API or changes ZooKeeper paths or stored state requires a decision record and release notes with the expected upgrade impact. Record the affected versions and a migration or rollback procedure where applicable. [ADR template](adr-template.md)

M0 covers the experimental server in this repository. M1 starts the separate lab and addresses safety issues including session failure and ownership. Later milestones address fencing, remote ownership and independently tested providers. [Baseline](baseline.md)
