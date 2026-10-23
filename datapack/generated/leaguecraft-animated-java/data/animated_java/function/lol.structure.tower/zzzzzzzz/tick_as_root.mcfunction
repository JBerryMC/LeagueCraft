execute unless score @s aj.lol.structure.tower.rig_loaded = @s aj.lol.structure.tower.rig_loaded run function animated_java:lol.structure.tower/zzzzzzzz/on_load
scoreboard players add @s aj.life_time 1
execute at @s on passengers run tp @s ~ ~ ~ ~ ~
function animated_java:lol.structure.tower/zzzzzzzz/animations/tick
function #animated_java:lol.structure.tower/on_tick/as_root