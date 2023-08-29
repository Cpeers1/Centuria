[![Build](https://github.com/Cpeers1/Centuria/actions/workflows/gradle.yml/badge.svg)](https://github.com/Cpeers1/Centuria/actions) [![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-blue.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)

# Centuria
Centuria is a work-in-progress server emulator for the now-defunct MMORPG Fer.al. Developed by a group of developers from the Fer.ever project, Centuria is a fan-run server designed to bring back the MMO game that recently shut down.

# Building Centuria
To build Centuria you will need to have Java 17 JDK installed. Centuria is build using gradle. 

## Building on Windows
On windows, run the following commands in cmd or powershell::

Download dependencies:
```powershell
mkdir deps
git clone https://github.com/SkySwimmer/connective-http deps/connective-http
```

Set up a development environment (optional):
```powershell
.\gradlew eclipse
```

Build the project:
```powershell
.\gradlew build
```

## Building on Linux and OSX
On linux, in bash or your favorite shell, run the following commands: (note that this requires bash to be installed on OSX, most linux distros have bash pre-installed)

Configure permissions:
```bash
chmod +x gradlew
```

Download dependencies:
```bash
mkdir deps
git clone https://github.com/SkySwimmer/connective-http deps/connective-http
```

Set up a development environment (optional):
```bash
./gradlew eclipse createEclipseLaunches
```

Build the project:
```bash
./gradlew build
```

## After building
After building, you can find the compiled server in `build/Installations`, you can run the server by starting `centuria` (either the shell or batch script depending on your OS)

<br/>

## Note about self-hosted servers
Due to the lack of a easy-to-use launcher, hosting a server yourself may prove difficult. You will need to edit your client's `Fer.al_Data/sharedassets1.assets` and swap out the server endpoints, however this can be tricky, you may run into string length issues. (a guide will be made in the near-future)

<br/>


# Built-in Client mods
Centuria comes with a few client mods in its main repository. 

## Module org.asf.centuria.client.plugin
This module is designed by Owen to swap out the game asset URLs, its still a bit untested here and there but seems to work.

### Building this Client Mod:
(written by Owen)
You will need a unhollowed version of the fer.al game assembly (Assembly-CSharp.dll) to build / run the client mod.
The hollowed version of the fer.al assembly is generated by the BepinEX client when first ran, in "client\build\BepInEx\unhollowed".
Place it into the "clientmods\org.asf.centuria.client.plugin\lib\feral" in this repository to be able to build the client mod.

I have plans to write the plugin to be able to self-retrieve the assembly when ran.
But in order to build it, you MUST manually obtain and move the fer.al game assembly.

### Installing this Client Mod
After building the client mod, you should find the dll in `bin/Debug/netstandard2.1`, copy the dll to a folder named `org.asf.centuria.client.plugin` in the bepinex plugins folder and it should load.
