scoreboard players add @s aj.aatrox.animation.r.cast.local_anim_time 1
scoreboard players operation @s aj.anim_time = @s aj.aatrox.animation.r.cast.local_anim_time
function animated_java:aatrox/zzzzzzzz/animations/r.cast/apply_frame_as_root
execute if score @s aj.aatrox.animation.r.cast.local_anim_time matches 10.. run function animated_java:aatrox/zzzzzzzz/animations/r.cast/end