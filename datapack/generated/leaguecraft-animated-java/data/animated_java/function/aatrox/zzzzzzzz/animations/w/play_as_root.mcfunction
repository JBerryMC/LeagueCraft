scoreboard players set @s aj.anim_time 0
scoreboard players set @s aj.aatrox.animation.w.local_anim_time 0
scoreboard players set @s aj.aatrox.animation.w.loop_mode 1
execute on passengers run data modify entity @s interpolation_duration set value 0
function animated_java:aatrox/zzzzzzzz/animations/w/tree/leaf_0
execute on passengers run data modify entity @s interpolation_duration set value 1
tag @s add aj.aatrox.animation.w