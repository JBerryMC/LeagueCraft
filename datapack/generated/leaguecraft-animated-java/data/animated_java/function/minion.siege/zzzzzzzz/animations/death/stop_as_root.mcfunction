scoreboard players set @s aj.minion.siege.animation.death.local_anim_time 0
tag @s remove aj.minion.siege.animation.death
execute on passengers run data modify entity @s interpolation_duration set value 0
tag @s add aj.minion.siege.disable_command_keyframes
function animated_java:minion.siege/zzzzzzzz/animations/death/tree/leaf_0
tag @s remove aj.minion.siege.disable_command_keyframes