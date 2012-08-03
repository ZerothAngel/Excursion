# Excursion &mdash; A teleport plugin for Bukkit #

Excursion is a teleport plugin for Bukkit which remembers each player's previous location in each world.

Please post bugs and/or feature requests as [dev.bukkit.org tickets](http://dev.bukkit.org/server-mods/excursion/tickets/).

## Features ##

*   When leaving a world (via the `/visit` command), the player's location within that world is saved. When the player revisits that world, they are teleported to their saved location.

*   Worlds can be blacklisted/excluded from this behavior, forcing players to always teleport to the world's spawn point.

*   Worlds can be grouped! When a player leaves a grouped world, in addition to the player's location, the player's world is also saved. When the player revisits that group, they are teleported to the exact world and location they left from.

    To make things clearer, suppose there are 3 worlds: WorldA, WorldB, and
WorldC. WorldA and WorldB are put in a group with WorldA as the "primary world."
When a player visits this group for the first time (by using `/visit` on either
WorldA or WorldB), the player will teleport to WorldA's spawn point.

    Let's now say the player is in WorldB and visits WorldC. When the player returns to the group, they will teleport back to their saved location in WorldB.

    **NOTE:** Due to the nature of grouping, some other method must be used to allow players to move between worlds of a group &mdash; for example, nether gates and portals.

*   Worlds (and groups) can have aliases.

*   A delay may be configured per world (or group). When a player leaves that
    world with the `/visit` command, they will be forced to wait the configured
    number of seconds before actually being teleported away. With a delay
    enabled, you may optionally configure that the teleport be cancelled
    if the player attacks or receives damage.

## Installation & Configuration ##

Simply drop Excursion.jar in your Bukkit server's `plugins` directory. Start up your server. This will create an `Excursion` directory and default `config.yml` within the `plugins` directory. See that file for configuration details.

## Commands & Permissions ##

The basic command is:

> `/visit <world>`

Where `<world>` is the name of a world or a group.

> `/excursion reload`

Re-reads config.yml.

Permission nodes are:

*   `excursion.visit` - Allows use of the `/visit` command.
*   `excursion.access.<world>` - Allows visiting `<world>`. You must use the exact name of the world directory in your server directory!
*   `excursion.access.*` - Allows visiting all worlds.
*   `excursion.reload` - Allows use of the `/excursion reload` command

Operators have all permissions by default.

## License & Source ##

Excursion is released under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Sources may be found on GitHub:

*   [Excursion](https://github.com/ZerothAngel/Excursion)
*   [ToHPluginUtils](https://github.com/ZerothAngel/ToHPluginUtils)

Development builds may be found on my continuous integration site:

*   [Excursion](http://ci.tyrannyofheaven.org/job/Excursion/lastSuccessfulBuild/org.tyrannyofheaven.bukkit$Excursion/) (Requires ToHPluginUtils.jar)
*   [Excursion-standlone](http://ci.tyrannyofheaven.org/job/Excursion-standalone/lastSuccessfulBuild/org.tyrannyofheaven.bukkit$Excursion/) (includes ToHPluginUtils, like the version distributed on dev.bukkit.org)

## To Do ##

*   Automated or manual cleanup of stale saved locations.
*   Perhaps simple import/export to allow for easier database schema upgrades.
