scoreboard players set @s aj.lol.structure.tower.animation.spawn.local_anim_time 0
tag @s remove aj.lol.structure.tower.animation.spawn
execute on passengers run data modify entity @s interpolation_duration set value 0
tag @s add aj.lol.structure.tower.disable_command_keyframes
function animated_java:lol.structure.tower/zzzzzzzz/animations/spawn/tree/leaf_0
tag @s remove aj.lol.structure.tower.disable_command_keyframes