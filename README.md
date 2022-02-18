# EmuLinkerSF
EmuLinkerSF is a modified version of EmuLinker Kaillera network server created by Moosehead with new features by Suprafast.
Original EmuLinker can be found here: https://github.com/monospacesoftware/emulinker
******************
This is unofficially updated version of EmulinkeSF, based on original latest source v72.3 (09-20-2009).
This version includes bug fixes and other improvements.
******************

## Development

To build the jar using Ant (tested at version 1.10.7) run:

```shell
$ ant build
```

To run the jar with Bash:

```shell
$ java -Xms64m -Xmx128m -cp ./conf:./build/emulinker.jar:./lib/commons-collections-3.1.jar:./lib/commons-configuration-1.1.jar:./lib/commons-el.jar:./lib/commons-lang-2.1.jar:./lib/commons-logging.jar:./lib/commons-pool-1.2.jar:./lib/log4j-1.2.12.jar:./lib/nanocontainer-1.0-beta-3.jar:./lib/picocontainer-1.1.jar:./lib/xstream-1.1.2.jar:./lib/commons-codec-1.3.jar:./lib/commons-httpclient-3.0-rc3.jar org.emulinker.kaillera.pico.PicoStarter
```
