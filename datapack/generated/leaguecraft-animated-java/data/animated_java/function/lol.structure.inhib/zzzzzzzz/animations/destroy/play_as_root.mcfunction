scoreboard players set @s aj.anim_time 0
scoreboard players set @s aj.lol.structure.inhib.animation.destroy.local_anim_time 0
scoreboard players set @s aj.lol.structure.inhib.animation.destroy.loop_mode 2
execute on passengers run data modify entity @s interpolation_duration set value 0
function animated_java:lol.structure.inhib/zzzzzzzz/animations/destroy/tree/leaf_0
execute on passengers run data modify entity @s interpolation_duration set value 1
tag @s add aj.lol.structure.inhib.animation.destroy