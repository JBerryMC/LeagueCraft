scoreboard players operation @s aj.id = .aj.last_id aj.id
tag @s remove aj.new
function #animated_java:minion.siege/on_summon/as_rig_entities
execute if entity @s[tag=aj.minion.siege.bone] run function #animated_java:minion.siege/zzzzzzzz/on_summon/as_bones

