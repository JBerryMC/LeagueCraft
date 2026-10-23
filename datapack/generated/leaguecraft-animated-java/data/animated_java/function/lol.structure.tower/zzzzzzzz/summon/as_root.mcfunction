execute unless score #frame aj.i = #frame aj.i run scoreboard players set #frame aj.i 0
execute unless score #variant aj.i = #variant aj.i run scoreboard players set #variant aj.i 0
execute unless score #animation aj.i = #animation aj.i run scoreboard players set #animation aj.i -1
scoreboard players set @s aj.anim_time 0
scoreboard players set @s aj.lol.structure.tower.rig_loaded 1
scoreboard players operation @s aj.lol.structure.tower.export_version = aj.lol.structure.tower.export_version aj.i
execute store result score @s aj.id run scoreboard players add .aj.last_id aj.id 1
tp @s ~ ~ ~ ~ ~
execute at @s on passengers run function animated_java:lol.structure.tower/zzzzzzzz/summon/as_rig_entities
execute if score #variant aj.i = $aj.lol.structure.tower.variant.blue aj.id run function animated_java:lol.structure.tower/zzzzzzzz/apply_variant/blue/as_root
execute if score #variant aj.i = $aj.lol.structure.tower.variant.red aj.id run function animated_java:lol.structure.tower/zzzzzzzz/apply_variant/red/as_root
execute if score #animation aj.i matches 0.. run scoreboard players operation @s aj.anim_time = #frame aj.i
execute if score #animation aj.i = $aj.lol.structure.tower.animation.spawn aj.id run function animated_java:lol.structure.tower/zzzzzzzz/animations/spawn/apply_frame_as_root
execute if score #animation aj.i = $aj.lol.structure.tower.animation.spawn aj.id run scoreboard players operation @s aj.lol.structure.tower.animation.spawn.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.lol.structure.tower.animation.idle aj.id run function animated_java:lol.structure.tower/zzzzzzzz/animations/idle/apply_frame_as_root
execute if score #animation aj.i = $aj.lol.structure.tower.animation.idle aj.id run scoreboard players operation @s aj.lol.structure.tower.animation.idle.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.lol.structure.tower.animation.destroy aj.id run function animated_java:lol.structure.tower/zzzzzzzz/animations/destroy/apply_frame_as_root
execute if score #animation aj.i = $aj.lol.structure.tower.animation.destroy aj.id run scoreboard players operation @s aj.lol.structure.tower.animation.destroy.local_anim_time = #frame aj.i
execute at @s run function #animated_java:lol.structure.tower/on_summon/as_root
tag @s remove aj.new
scoreboard players reset #frame aj.i
scoreboard players reset #variant aj.i
scoreboard players reset #animation aj.i