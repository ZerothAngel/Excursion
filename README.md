# Excursion &mdash; A teleport plugin for Bukkit #

Excursion is a teleport plugin for Bukkit which remembers each player's previous location in each world.

**NOTE:** This plugin only uses Superperms (aka Bukkit permissions) for permissions.

## Features ##

*   When leaving a world (via the `/visit` command), the player's location within that world is saved. When the player revisits that world, they are teleported to their saved location.

*   Worlds can be blacklisted/excluded from this behavior, forcing players to always teleport to the world's spawn point.

*   Worlds can be grouped! When a player leaves a grouped world, in addition to the player's location, the player's world is also saved. When the player revisits that group, they are teleported to the exact world and location they left from.

    To make things clearer, suppose there are 3 worlds: WorldA, WorldB, and WorldC. They are put in a group with WorldA as the "primary world." When a player visits this group for the first time (by using `/visit` on any of the member worlds), the player will teleport to WorldA's spawn point.

    Let's now say the player is in WorldB and visits some other world (that's not in the group). When the player returns to the group, they will teleport back to their saved location in WorldB.

    **NOTE:** Due to the nature of grouping, some other method must be used to allow players to move between worlds of a group &mdash; for example, nether gates and portals.

*   Worlds (and groups) can have aliases.

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
*   `excursion.visit.<world>` - Allows visiting `<world>`. You must use the exact name of the world directory in your server directory!
*   `excursion.visit.*` - Allows visiting all worlds.
*   `excursion.reload` - Allows use of the `/excursion reload` command

Operators have all permissions by default.
