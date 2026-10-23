scoreboard players set @s aj.aatrox.animation.aa.local_anim_time 0
tag @s remove aj.aatrox.animation.aa
execute on passengers run data modify entity @s interpolation_duration set value 0
tag @s add aj.aatrox.disable_command_keyframes
function animated_java:aatrox/zzzzzzzz/animations/aa/tree/leaf_0
tag @s remove aj.aatrox.disable_command_keyframes