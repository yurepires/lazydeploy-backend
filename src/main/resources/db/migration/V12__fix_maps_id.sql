-- The original seed contains provider identifiers that differ from the IDs
-- returned by the Battlelog Keeper. Keep the corrections in one place.
CREATE TEMPORARY TABLE map_id_corrections (
    old_id VARCHAR(128) PRIMARY KEY,
    new_id VARCHAR(128) NOT NULL UNIQUE
) ON COMMIT DROP;

INSERT INTO map_id_corrections (old_id, new_id)
VALUES
    ('XP3_004', 'XP3_UrbanGdn'),
    ('XP3_001', 'XP3_WtrFront'),
    ('XP3_002', 'XP3_Prpganda'),
    ('XP0_Caspi', 'XP0_Caspian'),
    ('XP4_WtrTret', 'XP4_WlkrFtry'),
    ('XP4_WlkrFtry', 'XP4_Titan'),
    ('MP_Damage', 'MP_Tremors'),
    ('XP6_CMP', 'XP7_Valley'),
    ('MP_Hainan', 'MP_Resort'),
    ('MP_Tremors', 'MP_Damage'),
    ('XP5_001', 'XP6_CMP'),
    ('XP6_Night', 'XP5_Night_01');

-- Move all source IDs to temporary values first. This makes swaps and chains
-- safe even when a destination is another source ID.
UPDATE battlefield_map AS map
SET id = '__V12_MAP__' || map.id,
    updated_at = CURRENT_TIMESTAMP
FROM map_id_corrections AS correction
WHERE map.id = correction.old_id;

UPDATE battlefield_map AS map
SET id = correction.new_id,
    updated_at = CURRENT_TIMESTAMP
FROM map_id_corrections AS correction
WHERE map.id = '__V12_MAP__' || correction.old_id;

-- XP0_Firestorm is the identifier of Operation Firestorm 2014 and is already
-- present in the seed. MP_Abandoned is a different map: Zavod 311.
UPDATE battlefield_map
SET display_name = 'Zavod 311',
    expansion = 'Base Game',
    enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'MP_Abandoned';

-- Rogue Transmission was missing from the original seed.
INSERT INTO battlefield_map (
    id,
    display_name,
    enabled,
    expansion,
    metadata,
    created_at,
    updated_at
)
VALUES (
    'MP_TheDish',
    'Rogue Transmission',
    TRUE,
    'Base Game',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE
SET display_name = EXCLUDED.display_name,
    enabled = EXCLUDED.enabled,
    expansion = EXCLUDED.expansion,
    updated_at = CURRENT_TIMESTAMP;

-- Rules created with the old catalog interpreted MP_Abandoned as Firestorm.
-- Keep that compatibility correction separate from the catalog corrections:
-- MP_Abandoned itself now remains the identifier of Zavod 311.
CREATE TEMPORARY TABLE rule_map_id_corrections (
    old_id VARCHAR(128) PRIMARY KEY,
    new_id VARCHAR(128) NOT NULL
) ON COMMIT DROP;

INSERT INTO rule_map_id_corrections (old_id, new_id)
SELECT old_id, new_id
FROM map_id_corrections
UNION ALL
SELECT 'MP_Abandoned', 'XP0_Firestorm';

-- Update MAP_IN rules that were saved with the old identifiers. Temporary
-- values prevent a correction chain from being applied more than once.
DO $$
DECLARE
    correction RECORD;
BEGIN
    FOR correction IN
        SELECT old_id, new_id
        FROM rule_map_id_corrections
    LOOP
        UPDATE notification_rule_parameter AS rule_parameter
        SET parameter_value = REPLACE(
                rule_parameter.parameter_value,
                correction.old_id,
                '__V12_RULE__' || correction.old_id
            )
        FROM notification_rule AS rule
        WHERE rule.id = rule_parameter.rule_id
          AND rule.type = 'MAP_IN'
          AND rule_parameter.parameter_key = 'values'
          AND strpos(rule_parameter.parameter_value, correction.old_id) > 0;
    END LOOP;

    FOR correction IN
        SELECT old_id, new_id
        FROM rule_map_id_corrections
    LOOP
        UPDATE notification_rule_parameter AS rule_parameter
        SET parameter_value = REPLACE(
                rule_parameter.parameter_value,
                '__V12_RULE__' || correction.old_id,
                correction.new_id
            )
        FROM notification_rule AS rule
        WHERE rule.id = rule_parameter.rule_id
          AND rule.type = 'MAP_IN'
          AND rule_parameter.parameter_key = 'values'
          AND strpos(rule_parameter.parameter_value, '__V12_RULE__') > 0;
    END LOOP;
END $$;
