scoreboard players add @s aj.lol.structure.tower.animation.idle.local_anim_time 1
scoreboard players operation @s aj.anim_time = @s aj.lol.structure.tower.animation.idle.local_anim_time
function animated_java:lol.structure.tower/zzzzzzzz/animations/idle/apply_frame_as_root
execute if score @s aj.lol.structure.tower.animation.idle.local_anim_time matches 80.. run function animated_java:lol.structure.tower/zzzzzzzz/animations/idle/end