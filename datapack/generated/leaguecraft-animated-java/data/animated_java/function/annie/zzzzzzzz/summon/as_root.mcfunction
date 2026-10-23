execute unless score #frame aj.i = #frame aj.i run scoreboard players set #frame aj.i 0
execute unless score #variant aj.i = #variant aj.i run scoreboard players set #variant aj.i 0
execute unless score #animation aj.i = #animation aj.i run scoreboard players set #animation aj.i -1
scoreboard players set @s aj.anim_time 0
scoreboard players set @s aj.annie.rig_loaded 1
scoreboard players operation @s aj.annie.export_version = aj.annie.export_version aj.i
execute store result score @s aj.id run scoreboard players add .aj.last_id aj.id 1
tp @s ~ ~ ~ ~ ~
execute at @s on passengers run function animated_java:annie/zzzzzzzz/summon/as_rig_entities
execute if score #variant aj.i = $aj.annie.variant.default aj.id run function animated_java:annie/zzzzzzzz/apply_variant/default/as_root
execute if score #animation aj.i matches 0.. run scoreboard players operation @s aj.anim_time = #frame aj.i
execute if score #animation aj.i = $aj.annie.animation.idle aj.id run function animated_java:annie/zzzzzzzz/animations/idle/apply_frame_as_root
execute if score #animation aj.i = $aj.annie.animation.idle aj.id run scoreboard players operation @s aj.annie.animation.idle.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.annie.animation.death aj.id run function animated_java:annie/zzzzzzzz/animations/death/apply_frame_as_root
execute if score #animation aj.i = $aj.annie.animation.death aj.id run scoreboard players operation @s aj.annie.animation.death.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.annie.animation.walk aj.id run function animated_java:annie/zzzzzzzz/animations/walk/apply_frame_as_root
execute if score #animation aj.i = $aj.annie.animation.walk aj.id run scoreboard players operation @s aj.annie.animation.walk.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.annie.animation.aa aj.id run function animated_java:annie/zzzzzzzz/animations/aa/apply_frame_as_root
execute if score #animation aj.i = $aj.annie.animation.aa aj.id run scoreboard players operation @s aj.annie.animation.aa.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.annie.animation.rage.aa aj.id run function animated_java:annie/zzzzzzzz/animations/rage.aa/apply_frame_as_root
execute if score #animation aj.i = $aj.annie.animation.rage.aa aj.id run scoreboard players operation @s aj.annie.animation.rage.aa.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.annie.animation.skill aj.id run function animated_java:annie/zzzzzzzz/animations/skill/apply_frame_as_root
execute if score #animation aj.i = $aj.annie.animation.skill aj.id run scoreboard players operation @s aj.annie.animation.skill.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.annie.animation.aa2 aj.id run function animated_java:annie/zzzzzzzz/animations/aa2/apply_frame_as_root
execute if score #animation aj.i = $aj.annie.animation.aa2 aj.id run scoreboard players operation @s aj.annie.animation.aa2.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.annie.animation.rage.aa2 aj.id run function animated_java:annie/zzzzzzzz/animations/rage.aa2/apply_frame_as_root
execute if score #animation aj.i = $aj.annie.animation.rage.aa2 aj.id run scoreboard players operation @s aj.annie.animation.rage.aa2.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.annie.animation.spawn aj.id run function animated_java:annie/zzzzzzzz/animations/spawn/apply_frame_as_root
execute if score #animation aj.i = $aj.annie.animation.spawn aj.id run scoreboard players operation @s aj.annie.animation.spawn.local_anim_time = #frame aj.i
execute at @s run function #animated_java:annie/on_summon/as_root
tag @s remove aj.new
scoreboard players reset #frame aj.i
scoreboard players reset #variant aj.i
scoreboard players reset #animation aj.i