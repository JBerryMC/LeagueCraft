scoreboard objectives add aj.i dummy
scoreboard objectives add aj.id dummy
scoreboard objectives add aj.tween_time dummy
scoreboard objectives add aj.anim_time dummy
scoreboard objectives add aj.life_time dummy
scoreboard objectives add aj.aatrox.export_version dummy
scoreboard objectives add aj.aatrox.rig_loaded dummy
scoreboard objectives add aj.aatrox.animation.r.cast.local_anim_time dummy
scoreboard objectives add aj.aatrox.animation.r.idle.local_anim_time dummy
scoreboard objectives add aj.aatrox.animation.q1.local_anim_time dummy
scoreboard objectives add aj.aatrox.animation.q2.local_anim_time dummy
scoreboard objectives add aj.aatrox.animation.q3.local_anim_time dummy
scoreboard objectives add aj.aatrox.animation.aa.local_anim_time dummy
scoreboard objectives add aj.aatrox.animation.passive.local_anim_time dummy
scoreboard objectives add aj.aatrox.animation.w.local_anim_time dummy
scoreboard objectives add aj.aatrox.animation.r.cast.loop_mode dummy
scoreboard objectives add aj.aatrox.animation.r.idle.loop_mode dummy
scoreboard objectives add aj.aatrox.animation.q1.loop_mode dummy
scoreboard objectives add aj.aatrox.animation.q2.loop_mode dummy
scoreboard objectives add aj.aatrox.animation.q3.loop_mode dummy
scoreboard objectives add aj.aatrox.animation.aa.loop_mode dummy
scoreboard objectives add aj.aatrox.animation.passive.loop_mode dummy
scoreboard objectives add aj.aatrox.animation.w.loop_mode dummy
scoreboard players set $aj.aatrox.animation.r.cast aj.id 0
scoreboard players set $aj.aatrox.animation.r.idle aj.id 1
scoreboard players set $aj.aatrox.animation.q1 aj.id 2
scoreboard players set $aj.aatrox.animation.q2 aj.id 3
scoreboard players set $aj.aatrox.animation.q3 aj.id 4
scoreboard players set $aj.aatrox.animation.aa aj.id 5
scoreboard players set $aj.aatrox.animation.passive aj.id 6
scoreboard players set $aj.aatrox.animation.w aj.id 7
scoreboard players set $aj.aatrox.variant.default aj.id 0
scoreboard players add .aj.last_id aj.id 0
scoreboard players set $aj.loop_mode.loop aj.i 0
scoreboard players set $aj.loop_mode.once aj.i 1
scoreboard players set $aj.loop_mode.hold aj.i 2
scoreboard players set aj.aatrox.export_version aj.i 708775211
scoreboard players reset * aj.aatrox.rig_loaded
execute as @e[type=minecraft:item_display,tag=aj.aatrox.root] run function animated_java:aatrox/zzzzzzzz/on_load