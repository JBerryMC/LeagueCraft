execute unless score #frame aj.i = #frame aj.i run scoreboard players set #frame aj.i 0
execute unless score #variant aj.i = #variant aj.i run scoreboard players set #variant aj.i 0
execute unless score #animation aj.i = #animation aj.i run scoreboard players set #animation aj.i -1
scoreboard players set @s aj.anim_time 0
scoreboard players set @s aj.aatrox.rig_loaded 1
scoreboard players operation @s aj.aatrox.export_version = aj.aatrox.export_version aj.i
execute store result score @s aj.id run scoreboard players add .aj.last_id aj.id 1
tp @s ~ ~ ~ ~ ~
execute at @s on passengers run function animated_java:aatrox/zzzzzzzz/summon/as_rig_entities
execute if score #variant aj.i = $aj.aatrox.variant.default aj.id run function animated_java:aatrox/zzzzzzzz/apply_variant/default/as_root
execute if score #animation aj.i matches 0.. run scoreboard players operation @s aj.anim_time = #frame aj.i
execute if score #animation aj.i = $aj.aatrox.animation.r.cast aj.id run function animated_java:aatrox/zzzzzzzz/animations/r.cast/apply_frame_as_root
execute if score #animation aj.i = $aj.aatrox.animation.r.cast aj.id run scoreboard players operation @s aj.aatrox.animation.r.cast.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.aatrox.animation.r.idle aj.id run function animated_java:aatrox/zzzzzzzz/animations/r.idle/apply_frame_as_root
execute if score #animation aj.i = $aj.aatrox.animation.r.idle aj.id run scoreboard players operation @s aj.aatrox.animation.r.idle.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.aatrox.animation.q1 aj.id run function animated_java:aatrox/zzzzzzzz/animations/q1/apply_frame_as_root
execute if score #animation aj.i = $aj.aatrox.animation.q1 aj.id run scoreboard players operation @s aj.aatrox.animation.q1.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.aatrox.animation.q2 aj.id run function animated_java:aatrox/zzzzzzzz/animations/q2/apply_frame_as_root
execute if score #animation aj.i = $aj.aatrox.animation.q2 aj.id run scoreboard players operation @s aj.aatrox.animation.q2.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.aatrox.animation.q3 aj.id run function animated_java:aatrox/zzzzzzzz/animations/q3/apply_frame_as_root
execute if score #animation aj.i = $aj.aatrox.animation.q3 aj.id run scoreboard players operation @s aj.aatrox.animation.q3.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.aatrox.animation.aa aj.id run function animated_java:aatrox/zzzzzzzz/animations/aa/apply_frame_as_root
execute if score #animation aj.i = $aj.aatrox.animation.aa aj.id run scoreboard players operation @s aj.aatrox.animation.aa.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.aatrox.animation.passive aj.id run function animated_java:aatrox/zzzzzzzz/animations/passive/apply_frame_as_root
execute if score #animation aj.i = $aj.aatrox.animation.passive aj.id run scoreboard players operation @s aj.aatrox.animation.passive.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.aatrox.animation.w aj.id run function animated_java:aatrox/zzzzzzzz/animations/w/apply_frame_as_root
execute if score #animation aj.i = $aj.aatrox.animation.w aj.id run scoreboard players operation @s aj.aatrox.animation.w.local_anim_time = #frame aj.i
execute at @s run function #animated_java:aatrox/on_summon/as_root
tag @s remove aj.new
scoreboard players reset #frame aj.i
scoreboard players reset #variant aj.i
scoreboard players reset #animation aj.i