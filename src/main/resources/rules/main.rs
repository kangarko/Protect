# -----------------------------------------------------------------------------------------------------
# Welcome to a rules file. It contains precreated rules for known hacked and crash items as well as
# examples to show you how Protect works.
#
# You can create as many .rs files as you need to organize your rules and delete this one.
#
# For documentation and examples, see https://docs.mineacademy.org/protect/rules
# For variables you can use, see https://docs.mineacademy.org/protect/variables
# -----------------------------------------------------------------------------------------------------

# -----------------------------------------------------------------------------------------------------
# A sample rule. 
# -----------------------------------------------------------------------------------------------------
# Each rule starts with "match" followed by a material name. See https://mineacademy.org/material
# for compatible material names. Use * to match everything or take advantage of our high performance
# regex fork described here: https://docs.mineacademy.org/protect/rules.html#must-have-operators
#
# This rule blocks unnaturally stacked items, i.e. 64x of Diamond Sword which naturally can only be 1x.

# Match every item
match *

# Every rule must have a unique name, i.e. unnatural-stack
name unnatural-stack

# This is a predefined operator that will call Bukkit API to check if the stack is over the max limit
check stack size

# If it is, notify admins with "protect.notify.unnaturalstack" permissions about this confiscation.
then notify protect.notify.unnaturalstack {player} has their {item_type} confiscated for it was unnaturally stacked ({item_amount}x)!

# Take the entire item. Protect scans each slot in the inventory individually so that the entire
# slot will be wiped.
then confiscate

# -----------------------------------------------------------------------------------------------------
# Another rule: Block items over 64 stacks, theoretically possible with modded clients.
# -----------------------------------------------------------------------------------------------------
match *
name over-64
require amount 65
then confiscate

# -----------------------------------------------------------------------------------------------------
# Confiscate the following items if found outside of creative gamemode.
# -----------------------------------------------------------------------------------------------------
match "SOIL"|"BEDROCK"|"COMMAND"|"BARRIER"|"STRUCTURE_BLOCK"|"COMMAND_MINECART"|"ENDER_PORTAL"|"ENDER_PORTAL_FRAME"|"PORTAL"
name survival-only
ignore gamemode creative
then confiscate

# -----------------------------------------------------------------------------------------------------
# Restrict diamond blocks to max 64 in a player's inventory.
# We take the excessive blocks. I.e. if player has 128 diamond blocks
# after this check he will only have 64.
# -----------------------------------------------------------------------------------------------------
match "DIAMOND_BLOCK"|"EMERALD_BLOCK"
name valuable-block
ignore inventory amount 64
ignore gamemode creative
then confiscate excess

# -----------------------------------------------------------------------------------------------------
# The three rules below limit items for newcoming players. Typically, new players should not have
# too many luxury items and this can reveal xrayers, bad staff or dupe exploits.
# This will check the amount of inventory items (i.e. in a chest + player inventory).
#
# See group.rs for explainer on how groups work.
# -----------------------------------------------------------------------------------------------------
match "DIAMOND_BLOCK"|"EMERALD_BLOCK"
name valuable-block-beginner
ignore inventory amount 1
group beginner

match "DIAMOND"|"EMERALD"
name valuable-beginner
ignore inventory amount 32
group beginner

match "DRAGON_EGG"|"BEACON"
name impossible-beginner
group beginner

# -----------------------------------------------------------------------------------------------------
# Restrict diamonds and emeralds to 256 stacks per container for all players.
# -----------------------------------------------------------------------------------------------------
match "DIAMOND"|"EMERALD"
name valuable
ignore inventory amount 256
ignore gamemode creative
then confiscate excess

# -----------------------------------------------------------------------------------------------------
# Restrict iron block to 3000 stacks per container for all players (one inv row can hold 576 stacks).
# -----------------------------------------------------------------------------------------------------
match "IRON_BLOCK"
name iron-block
ignore inventory amount 3000
ignore gamemode creative
then confiscate excess

# -----------------------------------------------------------------------------------------------------
# Restrict gold block to 1500 stacks per container for all players.
# -----------------------------------------------------------------------------------------------------
match "GOLD_BLOCK"
name gold-block
ignore inventory amount 1500
ignore gamemode creative
then confiscate excess

# -----------------------------------------------------------------------------------------------------
# Restrict redstone block to 2000 stacks per container for all players.
# -----------------------------------------------------------------------------------------------------
match "REDSTONE_BLOCK"
name redstone-block
ignore inventory amount 2000
ignore gamemode creative
then confiscate excess

# -----------------------------------------------------------------------------------------------------
# Restrict lapis block to 2000 stacks per container for all players.
# -----------------------------------------------------------------------------------------------------
match LAPIS_BLOCK
name lapis-block
ignore inventory amount 2000
ignore gamemode creative
then confiscate excess

# -----------------------------------------------------------------------------------------------------
# Restrict dragon egg to 1 for all players. Suitable for vanilla survival servers where it is very
# unlikely someone has more than one egg in a chest.
# -----------------------------------------------------------------------------------------------------
match DRAGON_EGG
name dragon-egg
ignore inventory amount 1
ignore gamemode creative
then confiscate excess

# -----------------------------------------------------------------------------------------------------
# Restrict beacons to 5 stacks per container for all players.
# -----------------------------------------------------------------------------------------------------
match BEACON
name beacon
ignore inventory amount 5
ignore gamemode creative
then confiscate excess

# -----------------------------------------------------------------------------------------------------
# Restrict enchanted gold apple to 5 stacks per container for all players.
# -----------------------------------------------------------------------------------------------------
match ENCHANTED_GOLDEN_APPLE
name enchanted-apple
ignore inventory amount 16
ignore gamemode creative
then confiscate excess

# -----------------------------------------------------------------------------------------------------
# Block too long item names that crash the client (i.e. Wurst's crash nametag).
# -----------------------------------------------------------------------------------------------------
match *
name name-too-long
require name length 64
then confiscate

# -----------------------------------------------------------------------------------------------------
# Block too long nbt item tags (use /protect iteminfo to inspect) that crash the client and the server.
# This blocks Wurst's crash chest
# -----------------------------------------------------------------------------------------------------
match *
name nbt-too-long
require tag length 2048
ignore material PLAYER_HEAD|PLAYER_WALL_HEAD
then notify protect.notify.nbttoolong {player} has {item_amount}x {item_type} confiscated for it had very long nbt tag! (Use /protect logs to view)
then confiscate

# -----------------------------------------------------------------------------------------------------
# Naturally potion bottle only has one potion, prevent all hacked potions.
# You need to edit this rule if you have custom potion plugins.
# -----------------------------------------------------------------------------------------------------
match POTION
name too-many-potions
require potion amount 2
then confiscate

# -----------------------------------------------------------------------------------------------------
# Limit max potion level to 5 (base level is 1 + amplifier at 4 equals level 5).
# You need to edit this rule if you have custom potion plugins.
# -----------------------------------------------------------------------------------------------------
match POTION
name level-too-high
require potion amplifier 4
then confiscate

# -----------------------------------------------------------------------------------------------------
# Block enchants unapplicable to the given item.
# Use "ignore enchant <enchant>" to ignore enchants from custom plugins.
# See https://mineacademy.org/enchants for enchant names (works on ALL Minecraft versions).
# -----------------------------------------------------------------------------------------------------
match *
name enchant-not-applicable
# Ignore Boss eggs from the Boss plugin (mineacademy.org/boss)
ignore tag Boss_V4
check enchant not-applicable
then confiscate

# -----------------------------------------------------------------------------------------------------
# Block unnaturally high enchants.
# Use "ignore enchant <enchant>" to ignore enchants from custom plugins.
# Use "ignore enchantlevel <level>" to specify max level for enchants if you support higher than
# vanilla levels.
# See https://mineacademy.org/enchants for enchant names (works on ALL Minecraft versions).
# -----------------------------------------------------------------------------------------------------
match *
name enchant-too-high
check enchant too-high
then confiscate