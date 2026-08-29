ALTER TABLE notification_delivery_attempt
    ADD COLUMN recipient_snapshot VARCHAR(320);

DELETE FROM notification_channel_parameter
WHERE LOWER(parameter_key) = 'recipient'
  AND channel_configuration_id IN (
      SELECT id
      FROM notification_channel_configuration
      WHERE UPPER(type) = 'EMAIL'
  );
