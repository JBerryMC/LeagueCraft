scoreboard players set @s aj.lol.structure.inhib.animation.spawn.local_anim_time 0
tag @s remove aj.lol.structure.inhib.animation.spawn
execute on passengers run data modify entity @s interpolation_duration set value 0
tag @s add aj.lol.structure.inhib.disable_command_keyframes
function animated_java:lol.structure.inhib/zzzzzzzz/animations/spawn/tree/leaf_0
tag @s remove aj.lol.structure.inhib.disable_command_keyframes