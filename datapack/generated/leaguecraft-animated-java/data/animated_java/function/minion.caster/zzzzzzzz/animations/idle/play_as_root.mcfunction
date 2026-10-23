scoreboard players set @s aj.anim_time 0
scoreboard players set @s aj.minion.caster.animation.idle.local_anim_time 0
scoreboard players set @s aj.minion.caster.animation.idle.loop_mode 0
execute on passengers run data modify entity @s interpolation_duration set value 0
function animated_java:minion.caster/zzzzzzzz/animations/idle/tree/leaf_0
execute on passengers run data modify entity @s interpolation_duration set value 1
tag @s add aj.minion.caster.animation.idle