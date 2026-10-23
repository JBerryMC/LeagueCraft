scoreboard players add @s aj.aatrox.animation.q1.local_anim_time 1
scoreboard players operation @s aj.anim_time = @s aj.aatrox.animation.q1.local_anim_time
function animated_java:aatrox/zzzzzzzz/animations/q1/apply_frame_as_root
execute if score @s aj.aatrox.animation.q1.local_anim_time matches 20.. run function animated_java:aatrox/zzzzzzzz/animations/q1/end