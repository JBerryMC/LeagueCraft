scoreboard players operation @s aj.id = .aj.last_id aj.id
tag @s remove aj.new
function #animated_java:minion.caster/on_summon/as_rig_entities
execute if entity @s[tag=aj.minion.caster.bone] run function #animated_java:minion.caster/zzzzzzzz/on_summon/as_bones

