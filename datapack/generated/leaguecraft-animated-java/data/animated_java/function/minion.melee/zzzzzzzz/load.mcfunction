scoreboard objectives add aj.i dummy
scoreboard objectives add aj.id dummy
scoreboard objectives add aj.tween_time dummy
scoreboard objectives add aj.anim_time dummy
scoreboard objectives add aj.life_time dummy
scoreboard objectives add aj.minion.melee.export_version dummy
scoreboard objectives add aj.minion.melee.rig_loaded dummy
scoreboard objectives add aj.minion.melee.animation.aa.local_anim_time dummy
scoreboard objectives add aj.minion.melee.animation.idle.local_anim_time dummy
scoreboard objectives add aj.minion.melee.animation.death.local_anim_time dummy
scoreboard objectives add aj.minion.melee.animation.aa.loop_mode dummy
scoreboard objectives add aj.minion.melee.animation.idle.loop_mode dummy
scoreboard objectives add aj.minion.melee.animation.death.loop_mode dummy
scoreboard players set $aj.minion.melee.animation.aa aj.id 0
scoreboard players set $aj.minion.melee.animation.idle aj.id 1
scoreboard players set $aj.minion.melee.animation.death aj.id 2
scoreboard players set $aj.minion.melee.variant.blue aj.id 0
scoreboard players set $aj.minion.melee.variant.red aj.id 1
scoreboard players add .aj.last_id aj.id 0
scoreboard players set $aj.loop_mode.loop aj.i 0
scoreboard players set $aj.loop_mode.once aj.i 1
scoreboard players set $aj.loop_mode.hold aj.i 2
scoreboard players set aj.minion.melee.export_version aj.i -905087695
scoreboard players reset * aj.minion.melee.rig_loaded
execute as @e[type=minecraft:item_display,tag=aj.minion.melee.root] run function animated_java:minion.melee/zzzzzzzz/on_load