scoreboard objectives remove aj.i
scoreboard objectives remove aj.id
scoreboard objectives remove aj.tween_time
scoreboard objectives remove aj.anim_time
scoreboard objectives remove aj.life_time
scoreboard objectives remove aj.minion.caster.export_version
scoreboard objectives remove aj.minion.caster.rig_loaded
scoreboard objectives remove aj.minion.caster.animation.aa.local_anim_time
scoreboard objectives remove aj.minion.caster.animation.idle.local_anim_time
scoreboard objectives remove aj.minion.caster.animation.death.local_anim_time
scoreboard objectives remove aj.minion.caster.animation.aa.loop_mode
scoreboard objectives remove aj.minion.caster.animation.idle.loop_mode
scoreboard objectives remove aj.minion.caster.animation.death.loop_mode
tellraw @a ["",{text:"["},{text:"Animated Java",color:"aqua"},{text:"] "},[{text:"INFO ℹ",color:"green"},{text:" > ",color:"gray"},{text:"The minion.caster Rig has been uninstalled successfully.\n"},{text:"Please remove the Rig's functions from the datapack before reloading.",color:"gray"}]]