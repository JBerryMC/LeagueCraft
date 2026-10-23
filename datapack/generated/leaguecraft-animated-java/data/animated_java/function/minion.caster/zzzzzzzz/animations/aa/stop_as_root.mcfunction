scoreboard players set @s aj.minion.caster.animation.aa.local_anim_time 0
tag @s remove aj.minion.caster.animation.aa
execute on passengers run data modify entity @s interpolation_duration set value 0
tag @s add aj.minion.caster.disable_command_keyframes
function animated_java:minion.caster/zzzzzzzz/animations/aa/tree/leaf_0
tag @s remove aj.minion.caster.disable_command_keyframes