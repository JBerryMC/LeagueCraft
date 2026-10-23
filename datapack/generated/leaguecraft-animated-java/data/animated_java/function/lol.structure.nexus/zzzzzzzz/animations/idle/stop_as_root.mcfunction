scoreboard players set @s aj.lol.structure.nexus.animation.idle.local_anim_time 0
tag @s remove aj.lol.structure.nexus.animation.idle
execute on passengers run data modify entity @s interpolation_duration set value 0
tag @s add aj.lol.structure.nexus.disable_command_keyframes
function animated_java:lol.structure.nexus/zzzzzzzz/animations/idle/tree/leaf_0
tag @s remove aj.lol.structure.nexus.disable_command_keyframes