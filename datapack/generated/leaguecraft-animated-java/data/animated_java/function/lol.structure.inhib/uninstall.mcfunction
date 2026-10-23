scoreboard objectives remove aj.i
scoreboard objectives remove aj.id
scoreboard objectives remove aj.tween_time
scoreboard objectives remove aj.anim_time
scoreboard objectives remove aj.life_time
scoreboard objectives remove aj.lol.structure.inhib.export_version
scoreboard objectives remove aj.lol.structure.inhib.rig_loaded
scoreboard objectives remove aj.lol.structure.inhib.animation.spawn.local_anim_time
scoreboard objectives remove aj.lol.structure.inhib.animation.idle.local_anim_time
scoreboard objectives remove aj.lol.structure.inhib.animation.destroy.local_anim_time
scoreboard objectives remove aj.lol.structure.inhib.animation.spawn.loop_mode
scoreboard objectives remove aj.lol.structure.inhib.animation.idle.loop_mode
scoreboard objectives remove aj.lol.structure.inhib.animation.destroy.loop_mode
tellraw @a ["",{text:"["},{text:"Animated Java",color:"aqua"},{text:"] "},[{text:"INFO ℹ",color:"green"},{text:" > ",color:"gray"},{text:"The lol.structure.inhib Rig has been uninstalled successfully.\n"},{text:"Please remove the Rig's functions from the datapack before reloading.",color:"gray"}]]