TimeWarp [![Build Status](https://travis-ci.org/InspireNXE/TimeWarp.svg?branch=master)](https://travis-ci.org/InspireNXE/TimeWarp)
=========
TimeWarp is a small plugin that opens up the ability to change the length of day in Minecraft. It is licensed under the [MIT License].

* [Download]
* [Commands]
* [Configuration]
* [Issues]
* [Wiki]
* [Source]
* [Donate]

### Features
* Ability to set daypart length to custom tick value  
  **Note**: The value set in the configuration file for each daypart is scaled against the original value that Minecraft would use. This means that if the custom value is around or lower than the original value that time will not appear to flow smoothly due to how the client handles manually setting time.
* Per-world settings

### Building
**Note:** If you do not have [Gradle] installed then use `./gradlew` for Unix systems or Git Bash and `gradlew.bat` for Windows systems in place of any `gradle` command.

To build Enquiry, simply run `gradle`. The compiled jar is located in `./libs/`.

[Commands]: https://github.com/InspireNXE/TimeWarp/wiki/Commands
[Configuration]: https://github.com/InspireNXE/TimeWarp/wiki/Configuration
[Donate]: https://www.patreon.com/Grinch
[Download]: https://github.com/InspireNXE/TimeWarp/releases/latest
[Gradle]: http://www.gradle.org
[Issues]: https://github.com/InspireNXE/TimeWarp/issues
[Java]: http://www.java.com
[MIT License]: http://www.tldrlegal.com/license/mit-license
[Source]: https://github.com/InspireNXE/TimeWarp/
[Wiki]: https://github.com/InspireNXE/TimeWarp/wiki