execute unless score @s aj.annie.rig_loaded = @s aj.annie.rig_loaded run function animated_java:annie/zzzzzzzz/on_load
scoreboard players add @s aj.life_time 1
execute at @s on passengers run tp @s ~ ~ ~ ~ ~
function animated_java:annie/zzzzzzzz/animations/tick
function #animated_java:annie/on_tick/as_root