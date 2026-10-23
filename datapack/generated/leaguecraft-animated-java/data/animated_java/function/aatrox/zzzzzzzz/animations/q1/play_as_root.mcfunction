scoreboard players set @s aj.anim_time 0
scoreboard players set @s aj.aatrox.animation.q1.local_anim_time 0
scoreboard players set @s aj.aatrox.animation.q1.loop_mode 2
execute on passengers run data modify entity @s interpolation_duration set value 0
function animated_java:aatrox/zzzzzzzz/animations/q1/tree/leaf_0
execute on passengers run data modify entity @s interpolation_duration set value 1
tag @s add aj.aatrox.animation.q1