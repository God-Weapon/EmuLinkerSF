# EmuLinkerSF
EmuLinkerSF is a modified version of EmuLinker Kaillera network server created by Moosehead with new features by Suprafast.
Original EmuLinker can be found here: https://github.com/monospacesoftware/emulinker
******************
This is unofficially updated version of EmulinkeSF, based on original latest source v72.3 (09-20-2009).
This version includes bug fixes and other improvements.
******************
## Development

From the emulinker/ directory:

| Command              | Description                                                                 |
| -------------------- | --------------------------------------------------------------------------- |
| `mvn compile`        | Compile the code. Running this command also patches ErrorProne suggestions. |
| `mvn spotless:apply` | Run the formatter.                                                          |
| `mvn spotless:check` | Run the linter.                                                             |
| `mvn test`           | Run tests.                                                                  |
| `mvn package`        | Build the jar.                                                              |
| `mvn exec:java`      | Run the server locally.                                                     |

Note: If you use non-ASCII characters in the `conf/language.properties` file, you need to run with at least Java 9 for the characters to appear correctly.