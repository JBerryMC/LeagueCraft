execute unless score #frame aj.i = #frame aj.i run scoreboard players set #frame aj.i 0
execute unless score #variant aj.i = #variant aj.i run scoreboard players set #variant aj.i 0
execute unless score #animation aj.i = #animation aj.i run scoreboard players set #animation aj.i -1
scoreboard players set @s aj.anim_time 0
scoreboard players set @s aj.minion.melee.rig_loaded 1
scoreboard players operation @s aj.minion.melee.export_version = aj.minion.melee.export_version aj.i
execute store result score @s aj.id run scoreboard players add .aj.last_id aj.id 1
tp @s ~ ~ ~ ~ ~
execute at @s on passengers run function animated_java:minion.melee/zzzzzzzz/summon/as_rig_entities
execute if score #variant aj.i = $aj.minion.melee.variant.blue aj.id run function animated_java:minion.melee/zzzzzzzz/apply_variant/blue/as_root
execute if score #variant aj.i = $aj.minion.melee.variant.red aj.id run function animated_java:minion.melee/zzzzzzzz/apply_variant/red/as_root
execute if score #animation aj.i matches 0.. run scoreboard players operation @s aj.anim_time = #frame aj.i
execute if score #animation aj.i = $aj.minion.melee.animation.aa aj.id run function animated_java:minion.melee/zzzzzzzz/animations/aa/apply_frame_as_root
execute if score #animation aj.i = $aj.minion.melee.animation.aa aj.id run scoreboard players operation @s aj.minion.melee.animation.aa.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.minion.melee.animation.idle aj.id run function animated_java:minion.melee/zzzzzzzz/animations/idle/apply_frame_as_root
execute if score #animation aj.i = $aj.minion.melee.animation.idle aj.id run scoreboard players operation @s aj.minion.melee.animation.idle.local_anim_time = #frame aj.i
execute if score #animation aj.i = $aj.minion.melee.animation.death aj.id run function animated_java:minion.melee/zzzzzzzz/animations/death/apply_frame_as_root
execute if score #animation aj.i = $aj.minion.melee.animation.death aj.id run scoreboard players operation @s aj.minion.melee.animation.death.local_anim_time = #frame aj.i
execute at @s run function #animated_java:minion.melee/on_summon/as_root
tag @s remove aj.new
scoreboard players reset #frame aj.i
scoreboard players reset #variant aj.i
scoreboard players reset #animation aj.i