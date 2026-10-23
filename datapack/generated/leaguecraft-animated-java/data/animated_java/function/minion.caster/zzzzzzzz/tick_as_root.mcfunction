execute unless score @s aj.minion.caster.rig_loaded = @s aj.minion.caster.rig_loaded run function animated_java:minion.caster/zzzzzzzz/on_load
scoreboard players add @s aj.life_time 1
execute at @s on passengers run tp @s ~ ~ ~ ~ ~
function animated_java:minion.caster/zzzzzzzz/animations/tick
function #animated_java:minion.caster/on_tick/as_root