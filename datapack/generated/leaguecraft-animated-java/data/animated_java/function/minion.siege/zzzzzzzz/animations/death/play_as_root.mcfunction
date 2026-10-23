scoreboard players set @s aj.anim_time 0
scoreboard players set @s aj.minion.siege.animation.death.local_anim_time 0
scoreboard players set @s aj.minion.siege.animation.death.loop_mode 2
execute on passengers run data modify entity @s interpolation_duration set value 0
function animated_java:minion.siege/zzzzzzzz/animations/death/tree/leaf_0
execute on passengers run data modify entity @s interpolation_duration set value 1
tag @s add aj.minion.siege.animation.death