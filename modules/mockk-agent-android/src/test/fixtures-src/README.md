# HostWithTrap test fixture

`HostWithTrap` references `MissingType` at compile time. Only `poison-host-fixtures.jar` (containing
`HostWithTrap.class`, not `MissingType`) is checked in under `src/test/resources/` so unit tests reproduce issue #1518:
`Class.declaredMethods` fails while `getDeclaredMethod("safe")` does not.

Regenerate after changing the sources:

```bash
cd modules/mockk-agent-android/src/test/fixtures-src
rm -rf /tmp/poison-missing /tmp/poison-host
mkdir -p /tmp/poison-missing /tmp/poison-host
javac -d /tmp/poison-missing MissingType.java
javac -cp /tmp/poison-missing -d /tmp/poison-host HostWithTrap.java
jar cf ../resources/poison-host-fixtures.jar -C /tmp/poison-host .
```
