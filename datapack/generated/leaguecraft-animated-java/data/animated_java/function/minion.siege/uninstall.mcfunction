scoreboard objectives remove aj.i
scoreboard objectives remove aj.id
scoreboard objectives remove aj.tween_time
scoreboard objectives remove aj.anim_time
scoreboard objectives remove aj.life_time
scoreboard objectives remove aj.minion.siege.export_version
scoreboard objectives remove aj.minion.siege.rig_loaded
scoreboard objectives remove aj.minion.siege.animation.aa.local_anim_time
scoreboard objectives remove aj.minion.siege.animation.idle.local_anim_time
scoreboard objectives remove aj.minion.siege.animation.death.local_anim_time
scoreboard objectives remove aj.minion.siege.animation.aa.loop_mode
scoreboard objectives remove aj.minion.siege.animation.idle.loop_mode
scoreboard objectives remove aj.minion.siege.animation.death.loop_mode
tellraw @a ["",{text:"["},{text:"Animated Java",color:"aqua"},{text:"] "},[{text:"INFO ℹ",color:"green"},{text:" > ",color:"gray"},{text:"The minion.siege Rig has been uninstalled successfully.\n"},{text:"Please remove the Rig's functions from the datapack before reloading.",color:"gray"}]]