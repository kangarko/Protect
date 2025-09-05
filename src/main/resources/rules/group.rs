# -----------------------------------------------------------------------------------------------------
# Welcome to the group file. 
#
# You can create groups of rule operators to avoid writing the same operators twice.
# You can empty, but do not delete this file.
# 
# For documentation and examples, see https://docs.mineacademy.org/protect/rules#groups
# -----------------------------------------------------------------------------------------------------

# An example group
#
# This group restrict players who are new to the server to have a maximum of 1 stack 
# diamond/emerald blocks. New players usually don't have high amount of luxury items, so this might
# reveal xrayers, duping operations or staff abusing their rights.
#
# The playtime is pulled from your main world/playerdata/ folder from the player's .dat file.

# Define the group "beginner"
group beginner

# Do not execute the group if player spent more than 2 hours on the server
ignore playtime 2 hours

# Do not execute for players having "protect.bypass.beginner" permission
ignore perm protect.bypass.beginner

# Ignore creative players
ignore gamemode creative

# Confiscate the items over the limit (specified in your rules using the "ignore inventory amount"
# operator. For example, if "ignore inventory amount" is set to "32", and player has 48 stacks of
# diamond, we take the 16 stacks over the limit so that player will be left having 32 stacks.
then confiscate excess
