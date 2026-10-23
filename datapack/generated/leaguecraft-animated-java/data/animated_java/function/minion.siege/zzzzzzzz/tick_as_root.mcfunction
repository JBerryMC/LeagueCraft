execute unless score @s aj.minion.siege.rig_loaded = @s aj.minion.siege.rig_loaded run function animated_java:minion.siege/zzzzzzzz/on_load
scoreboard players add @s aj.life_time 1
execute at @s on passengers run tp @s ~ ~ ~ ~ ~
function animated_java:minion.siege/zzzzzzzz/animations/tick
function #animated_java:minion.siege/on_tick/as_root