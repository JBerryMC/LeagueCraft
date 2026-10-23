scoreboard players add @s aj.minion.siege.animation.death.local_anim_time 1
scoreboard players operation @s aj.anim_time = @s aj.minion.siege.animation.death.local_anim_time
function animated_java:minion.siege/zzzzzzzz/animations/death/apply_frame_as_root
execute if score @s aj.minion.siege.animation.death.local_anim_time matches 15.. run function animated_java:minion.siege/zzzzzzzz/animations/death/end