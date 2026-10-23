scoreboard players add @s aj.minion.melee.animation.aa.local_anim_time 1
scoreboard players operation @s aj.anim_time = @s aj.minion.melee.animation.aa.local_anim_time
function animated_java:minion.melee/zzzzzzzz/animations/aa/apply_frame_as_root
execute if score @s aj.minion.melee.animation.aa.local_anim_time matches 20.. run function animated_java:minion.melee/zzzzzzzz/animations/aa/end