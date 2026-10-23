scoreboard objectives add aj.i dummy
scoreboard objectives add aj.id dummy
scoreboard objectives add aj.tween_time dummy
scoreboard objectives add aj.anim_time dummy
scoreboard objectives add aj.life_time dummy
scoreboard objectives add aj.minion.siege.export_version dummy
scoreboard objectives add aj.minion.siege.rig_loaded dummy
scoreboard objectives add aj.minion.siege.animation.aa.local_anim_time dummy
scoreboard objectives add aj.minion.siege.animation.idle.local_anim_time dummy
scoreboard objectives add aj.minion.siege.animation.death.local_anim_time dummy
scoreboard objectives add aj.minion.siege.animation.aa.loop_mode dummy
scoreboard objectives add aj.minion.siege.animation.idle.loop_mode dummy
scoreboard objectives add aj.minion.siege.animation.death.loop_mode dummy
scoreboard players set $aj.minion.siege.animation.aa aj.id 0
scoreboard players set $aj.minion.siege.animation.idle aj.id 1
scoreboard players set $aj.minion.siege.animation.death aj.id 2
scoreboard players set $aj.minion.siege.variant.blue aj.id 0
scoreboard players set $aj.minion.siege.variant.red aj.id 1
scoreboard players add .aj.last_id aj.id 0
scoreboard players set $aj.loop_mode.loop aj.i 0
scoreboard players set $aj.loop_mode.once aj.i 1
scoreboard players set $aj.loop_mode.hold aj.i 2
scoreboard players set aj.minion.siege.export_version aj.i -1724407556
scoreboard players reset * aj.minion.siege.rig_loaded
execute as @e[type=minecraft:item_display,tag=aj.minion.siege.root] run function animated_java:minion.siege/zzzzzzzz/on_load