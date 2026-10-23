scoreboard objectives add aj.i dummy
scoreboard objectives add aj.id dummy
scoreboard objectives add aj.tween_time dummy
scoreboard objectives add aj.anim_time dummy
scoreboard objectives add aj.life_time dummy
scoreboard objectives add aj.lol.structure.tower.export_version dummy
scoreboard objectives add aj.lol.structure.tower.rig_loaded dummy
scoreboard objectives add aj.lol.structure.tower.animation.spawn.local_anim_time dummy
scoreboard objectives add aj.lol.structure.tower.animation.idle.local_anim_time dummy
scoreboard objectives add aj.lol.structure.tower.animation.destroy.local_anim_time dummy
scoreboard objectives add aj.lol.structure.tower.animation.spawn.loop_mode dummy
scoreboard objectives add aj.lol.structure.tower.animation.idle.loop_mode dummy
scoreboard objectives add aj.lol.structure.tower.animation.destroy.loop_mode dummy
scoreboard players set $aj.lol.structure.tower.animation.spawn aj.id 0
scoreboard players set $aj.lol.structure.tower.animation.idle aj.id 1
scoreboard players set $aj.lol.structure.tower.animation.destroy aj.id 2
scoreboard players set $aj.lol.structure.tower.variant.blue aj.id 0
scoreboard players set $aj.lol.structure.tower.variant.red aj.id 1
scoreboard players add .aj.last_id aj.id 0
scoreboard players set $aj.loop_mode.loop aj.i 0
scoreboard players set $aj.loop_mode.once aj.i 1
scoreboard players set $aj.loop_mode.hold aj.i 2
scoreboard players set aj.lol.structure.tower.export_version aj.i 1103619383
scoreboard players reset * aj.lol.structure.tower.rig_loaded
execute as @e[type=minecraft:item_display,tag=aj.lol.structure.tower.root] run function animated_java:lol.structure.tower/zzzzzzzz/on_load