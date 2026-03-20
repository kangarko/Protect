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
# Confiscate the following items if found outside of creative game mode.
# -----------------------------------------------------------------------------------------------------
match "SOIL"|"BEDROCK"|"COMMAND"|"BARRIER"|"STRUCTURE_BLOCK"|"COMMAND_MINECART"|"ENDER_PORTAL"|"ENDER_PORTAL_FRAME"|"PORTAL"
name survival-only
ignore gamemode creative
then confiscate

# -----------------------------------------------------------------------------------------------------
# Restrict diamond blocks to max 64 in a player's inventory.
# We take the excessive blocks, i.e., if a player has 128 diamond blocks
# after this check he will only have 64.
# -----------------------------------------------------------------------------------------------------
match "DIAMOND_BLOCK"|"EMERALD_BLOCK"
name valuable-block
ignore inventory amount 64
ignore gamemode creative
then confiscate excess

# -----------------------------------------------------------------------------------------------------
# The three rules below limit items for new players. Typically, new players should not have
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

# -----------------------------------------------------------------------------------------------------
# Block conflicting enchantments on the same item (e.g., Sharpness + Smite).
# Naturally, Minecraft prevents this from happening, so this catches hacked items.
# WARNING: Many custom item plugins intentionally add conflicting enchantments.
# Uncomment the lines below to enable this rule.
# TIP: Use #enchantable/durability instead of * to only scan enchantable items (faster).
# See https://minecraft.wiki/w/Tag for all available tags.
# -----------------------------------------------------------------------------------------------------
#match #enchantable/durability
#name enchant-conflicting
#check enchant conflicting
#then confiscate

# -----------------------------------------------------------------------------------------------------
# Block items with the unbreakable flag set outside of creative mode.
# Unbreakable items cannot be obtained in survival without hacking.
# WARNING: Many custom item plugins create unbreakable items as part of gameplay.
# Uncomment the lines below to enable this rule.
# -----------------------------------------------------------------------------------------------------
#match *
#name unbreakable
#check unbreakable
#ignore gamemode creative
#then strip-nbt

# -----------------------------------------------------------------------------------------------------
# Block items with custom attribute modifiers (e.g., overpowered damage/speed).
# Use "require attribute value <amount>" to only match modifiers above a certain threshold.
# WARNING: RPG and custom item plugins often set high attribute values intentionally.
# Uncomment the lines below to enable this rule.
# -----------------------------------------------------------------------------------------------------
#match *
#name attribute-modified
#check attribute modified
#require attribute value 100
#then strip-attributes

# -----------------------------------------------------------------------------------------------------
# Block items with hidden tooltips. Hacked clients hide item details to disguise illegal items.
# Requires 1.21+. Uncomment the lines below to enable this rule.
# -----------------------------------------------------------------------------------------------------
#match *
#name hidden-tooltip
#check hide-tooltip
#then strip-hide-tooltip

# -----------------------------------------------------------------------------------------------------
# Catch-all: Strip ALL non-vanilla components from items at once.
# This catches hacked food, consumable, equippable, rarity, death protection, tool, damage
# resistance, and any other component that doesn't belong on the item's vanilla version.
# Requires 1.21+. Uncomment the lines below to enable this rule.
# WARNING: Custom item plugins may add components intentionally. Use ignore operators to whitelist.
# Use "ignore component <name>" to preserve specific components (e.g. custom_name, lore).
# Multiple components can be separated with |, e.g. "ignore component custom_name|lore"
# See https://minecraft.wiki/w/Data_component_format for all component names.
# -----------------------------------------------------------------------------------------------------
#match *
#name illegal-components
#check illegal components
#ignore component custom_name|lore|enchantments|custom_data|custom_model_data|damage|repair_cost|attribute_modifiers|stored_enchantments
#then strip-components

# -----------------------------------------------------------------------------------------------------
# Individual component rules (alternative to the catch-all above for granular control).
# Each rule targets a specific hacked component. Requires 1.21+.
# Uncomment the ones you need.
# -----------------------------------------------------------------------------------------------------
#match *
#name illegal-food
#check illegal food
#then strip-food

#match *
#name illegal-consumable
#check illegal consumable
#then strip-consumable

#match *
#name illegal-equippable
#check illegal equippable
#then strip-equippable

#match *
#name illegal-rarity
#check illegal rarity
#then strip-rarity

#match *
#name illegal-death-protection
#check illegal death-protection
#then strip-death-protection

#match *
#name illegal-damage-resistant
#check illegal damage-resistant
#then strip-damage-resistant

#match *
#name illegal-tool
#check illegal tool
#then strip-tool

#match *
#name enchantment-glint-override
#check enchantment-glint-override
#then strip-enchantment-glint

# -----------------------------------------------------------------------------------------------------
# Dynamic component rules: You can check and strip ANY component by name without
# needing a specific built-in rule. Use "check illegal <component>" and "then strip-<component>"
# where <component> is any Minecraft component name (use underscores, e.g. attack_range).
# See https://minecraft.wiki/w/Data_component_format for all component names.
# This works with current and future Minecraft versions automatically.
# -----------------------------------------------------------------------------------------------------
#match *
#name illegal-attack-range
#check illegal attack_range
#then strip-attack_range

#match *
#name illegal-blocks-attacks
#check illegal blocks_attacks
#then strip-blocks_attacks

#match *
#name illegal-glider
#check illegal glider
#then strip-glider

# -----------------------------------------------------------------------------------------------------
# Per-rule ignore operators for items with custom display names, lore, or model data.
# Use these to whitelist specific items from being scanned by a rule instead of relying
# on the global Ignore settings in settings.yml.
#
# Supports wildcard matching (* for any characters): "ignore lore *Quest*"
# Lore lines are joined with | and colors are stripped before matching.
#
# Example: Skip items from a custom plugin by their display name or lore
# ignore displayname *Custom Weapon*
# ignore lore *Quest Item*
# ignore modeldata 12345
# -----------------------------------------------------------------------------------------------------