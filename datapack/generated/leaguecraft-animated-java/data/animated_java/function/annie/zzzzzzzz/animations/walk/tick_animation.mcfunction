scoreboard players add @s aj.annie.animation.walk.local_anim_time 1
scoreboard players operation @s aj.anim_time = @s aj.annie.animation.walk.local_anim_time
function animated_java:annie/zzzzzzzz/animations/walk/apply_frame_as_root
execute if score @s aj.annie.animation.walk.local_anim_time matches 20.. run function animated_java:annie/zzzzzzzz/animations/walk/end