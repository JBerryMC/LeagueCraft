scoreboard players set @s aj.lol.structure.inhib.animation.destroy.local_anim_time 0
tag @s remove aj.lol.structure.inhib.animation.destroy
execute on passengers run data modify entity @s interpolation_duration set value 0
tag @s add aj.lol.structure.inhib.disable_command_keyframes
function animated_java:lol.structure.inhib/zzzzzzzz/animations/destroy/tree/leaf_0
tag @s remove aj.lol.structure.inhib.disable_command_keyframes