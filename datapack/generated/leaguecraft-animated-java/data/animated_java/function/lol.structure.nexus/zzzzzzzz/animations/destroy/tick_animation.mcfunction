scoreboard players add @s aj.lol.structure.nexus.animation.destroy.local_anim_time 1
scoreboard players operation @s aj.anim_time = @s aj.lol.structure.nexus.animation.destroy.local_anim_time
function animated_java:lol.structure.nexus/zzzzzzzz/animations/destroy/apply_frame_as_root
execute if score @s aj.lol.structure.nexus.animation.destroy.local_anim_time matches 105.. run function animated_java:lol.structure.nexus/zzzzzzzz/animations/destroy/end