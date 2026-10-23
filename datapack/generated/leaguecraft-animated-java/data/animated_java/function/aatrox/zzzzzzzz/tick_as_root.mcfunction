execute unless score @s aj.aatrox.rig_loaded = @s aj.aatrox.rig_loaded run function animated_java:aatrox/zzzzzzzz/on_load
scoreboard players add @s aj.life_time 1
execute at @s on passengers run tp @s ~ ~ ~ ~ ~
function animated_java:aatrox/zzzzzzzz/animations/tick
function #animated_java:aatrox/on_tick/as_root