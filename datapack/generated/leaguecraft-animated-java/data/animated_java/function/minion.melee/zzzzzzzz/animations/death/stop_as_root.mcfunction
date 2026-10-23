scoreboard players set @s aj.minion.melee.animation.death.local_anim_time 0
tag @s remove aj.minion.melee.animation.death
execute on passengers run data modify entity @s interpolation_duration set value 0
tag @s add aj.minion.melee.disable_command_keyframes
function animated_java:minion.melee/zzzzzzzz/animations/death/tree/leaf_0
tag @s remove aj.minion.melee.disable_command_keyframes