scoreboard objectives add aj.i dummy
scoreboard objectives add aj.id dummy
scoreboard objectives add aj.tween_time dummy
scoreboard objectives add aj.anim_time dummy
scoreboard objectives add aj.life_time dummy
scoreboard objectives add aj.annie.export_version dummy
scoreboard objectives add aj.annie.rig_loaded dummy
scoreboard objectives add aj.annie.animation.idle.local_anim_time dummy
scoreboard objectives add aj.annie.animation.death.local_anim_time dummy
scoreboard objectives add aj.annie.animation.walk.local_anim_time dummy
scoreboard objectives add aj.annie.animation.aa.local_anim_time dummy
scoreboard objectives add aj.annie.animation.rage.aa.local_anim_time dummy
scoreboard objectives add aj.annie.animation.skill.local_anim_time dummy
scoreboard objectives add aj.annie.animation.aa2.local_anim_time dummy
scoreboard objectives add aj.annie.animation.rage.aa2.local_anim_time dummy
scoreboard objectives add aj.annie.animation.spawn.local_anim_time dummy
scoreboard objectives add aj.annie.animation.idle.loop_mode dummy
scoreboard objectives add aj.annie.animation.death.loop_mode dummy
scoreboard objectives add aj.annie.animation.walk.loop_mode dummy
scoreboard objectives add aj.annie.animation.aa.loop_mode dummy
scoreboard objectives add aj.annie.animation.rage.aa.loop_mode dummy
scoreboard objectives add aj.annie.animation.skill.loop_mode dummy
scoreboard objectives add aj.annie.animation.aa2.loop_mode dummy
scoreboard objectives add aj.annie.animation.rage.aa2.loop_mode dummy
scoreboard objectives add aj.annie.animation.spawn.loop_mode dummy
scoreboard players set $aj.annie.animation.idle aj.id 0
scoreboard players set $aj.annie.animation.death aj.id 1
scoreboard players set $aj.annie.animation.walk aj.id 2
scoreboard players set $aj.annie.animation.aa aj.id 3
scoreboard players set $aj.annie.animation.rage.aa aj.id 4
scoreboard players set $aj.annie.animation.skill aj.id 5
scoreboard players set $aj.annie.animation.aa2 aj.id 6
scoreboard players set $aj.annie.animation.rage.aa2 aj.id 7
scoreboard players set $aj.annie.animation.spawn aj.id 8
scoreboard players set $aj.annie.variant.default aj.id 0
scoreboard players add .aj.last_id aj.id 0
scoreboard players set $aj.loop_mode.loop aj.i 0
scoreboard players set $aj.loop_mode.once aj.i 1
scoreboard players set $aj.loop_mode.hold aj.i 2
scoreboard players set aj.annie.export_version aj.i -98649140
scoreboard players reset * aj.annie.rig_loaded
execute as @e[type=minecraft:item_display,tag=aj.annie.root] run function animated_java:annie/zzzzzzzz/on_load