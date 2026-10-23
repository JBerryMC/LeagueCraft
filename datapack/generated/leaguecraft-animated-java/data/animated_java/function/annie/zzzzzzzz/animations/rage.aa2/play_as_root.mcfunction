scoreboard players set @s aj.anim_time 0
scoreboard players set @s aj.annie.animation.rage.aa2.local_anim_time 0
scoreboard players set @s aj.annie.animation.rage.aa2.loop_mode 1
execute on passengers run data modify entity @s interpolation_duration set value 0
function animated_java:annie/zzzzzzzz/animations/rage.aa2/tree/leaf_0
execute on passengers run data modify entity @s interpolation_duration set value 1
tag @s add aj.annie.animation.rage.aa2