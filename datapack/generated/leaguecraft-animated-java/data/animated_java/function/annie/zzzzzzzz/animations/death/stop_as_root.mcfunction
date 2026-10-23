scoreboard players set @s aj.annie.animation.death.local_anim_time 0
tag @s remove aj.annie.animation.death
execute on passengers run data modify entity @s interpolation_duration set value 0
tag @s add aj.annie.disable_command_keyframes
function animated_java:annie/zzzzzzzz/animations/death/tree/leaf_0
tag @s remove aj.annie.disable_command_keyframes