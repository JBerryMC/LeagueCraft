scoreboard objectives add aj.i dummy
scoreboard objectives add aj.id dummy
scoreboard objectives add aj.tween_time dummy
scoreboard objectives add aj.anim_time dummy
scoreboard objectives add aj.life_time dummy
scoreboard objectives add aj.minion.super.export_version dummy
scoreboard objectives add aj.minion.super.rig_loaded dummy
scoreboard objectives add aj.minion.super.animation.aa.local_anim_time dummy
scoreboard objectives add aj.minion.super.animation.idle.local_anim_time dummy
scoreboard objectives add aj.minion.super.animation.death.local_anim_time dummy
scoreboard objectives add aj.minion.super.animation.aa.loop_mode dummy
scoreboard objectives add aj.minion.super.animation.idle.loop_mode dummy
scoreboard objectives add aj.minion.super.animation.death.loop_mode dummy
scoreboard players set $aj.minion.super.animation.aa aj.id 0
scoreboard players set $aj.minion.super.animation.idle aj.id 1
scoreboard players set $aj.minion.super.animation.death aj.id 2
scoreboard players set $aj.minion.super.variant.blue aj.id 0
scoreboard players set $aj.minion.super.variant.red aj.id 1
scoreboard players add .aj.last_id aj.id 0
scoreboard players set $aj.loop_mode.loop aj.i 0
scoreboard players set $aj.loop_mode.once aj.i 1
scoreboard players set $aj.loop_mode.hold aj.i 2
scoreboard players set aj.minion.super.export_version aj.i 1484896018
scoreboard players reset * aj.minion.super.rig_loaded
execute as @e[type=minecraft:item_display,tag=aj.minion.super.root] run function animated_java:minion.super/zzzzzzzz/on_load