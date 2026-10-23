scoreboard players set @s aj.aatrox.animation.q1.local_anim_time 0
tag @s remove aj.aatrox.animation.q1
execute on passengers run data modify entity @s interpolation_duration set value 0
tag @s add aj.aatrox.disable_command_keyframes
function animated_java:aatrox/zzzzzzzz/animations/q1/tree/leaf_0
tag @s remove aj.aatrox.disable_command_keyframes