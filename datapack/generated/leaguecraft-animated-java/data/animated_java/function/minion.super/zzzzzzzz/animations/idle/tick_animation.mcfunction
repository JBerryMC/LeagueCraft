scoreboard players add @s aj.minion.super.animation.idle.local_anim_time 1
scoreboard players operation @s aj.anim_time = @s aj.minion.super.animation.idle.local_anim_time
function animated_java:minion.super/zzzzzzzz/animations/idle/apply_frame_as_root
execute if score @s aj.minion.super.animation.idle.local_anim_time matches 30.. run function animated_java:minion.super/zzzzzzzz/animations/idle/end