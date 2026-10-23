scoreboard players add @s aj.lol.structure.inhib.animation.idle.local_anim_time 1
scoreboard players operation @s aj.anim_time = @s aj.lol.structure.inhib.animation.idle.local_anim_time
function animated_java:lol.structure.inhib/zzzzzzzz/animations/idle/apply_frame_as_root
execute if score @s aj.lol.structure.inhib.animation.idle.local_anim_time matches 320.. run function animated_java:lol.structure.inhib/zzzzzzzz/animations/idle/end