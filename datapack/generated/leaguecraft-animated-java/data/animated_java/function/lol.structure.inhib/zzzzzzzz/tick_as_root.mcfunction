execute unless score @s aj.lol.structure.inhib.rig_loaded = @s aj.lol.structure.inhib.rig_loaded run function animated_java:lol.structure.inhib/zzzzzzzz/on_load
scoreboard players add @s aj.life_time 1
execute at @s on passengers run tp @s ~ ~ ~ ~ ~
function animated_java:lol.structure.inhib/zzzzzzzz/animations/tick
function #animated_java:lol.structure.inhib/on_tick/as_root