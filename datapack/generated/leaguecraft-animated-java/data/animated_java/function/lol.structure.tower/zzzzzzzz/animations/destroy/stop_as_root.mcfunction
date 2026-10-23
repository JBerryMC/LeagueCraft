scoreboard players set @s aj.lol.structure.tower.animation.destroy.local_anim_time 0
tag @s remove aj.lol.structure.tower.animation.destroy
execute on passengers run data modify entity @s interpolation_duration set value 0
tag @s add aj.lol.structure.tower.disable_command_keyframes
function animated_java:lol.structure.tower/zzzzzzzz/animations/destroy/tree/leaf_0
tag @s remove aj.lol.structure.tower.disable_command_keyframes