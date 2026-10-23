scoreboard players set @s aj.anim_time 0
scoreboard players set @s aj.aatrox.animation.r.cast.local_anim_time 0
scoreboard players set @s aj.aatrox.animation.r.cast.loop_mode 2
execute on passengers run data modify entity @s interpolation_duration set value 0
function animated_java:aatrox/zzzzzzzz/animations/r.cast/tree/leaf_0
execute on passengers run data modify entity @s interpolation_duration set value 1
tag @s add aj.aatrox.animation.r.cast