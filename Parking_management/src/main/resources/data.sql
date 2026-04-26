-- Insert parking levels
INSERT INTO parking_level (level_name, total_slots, current_occupancy) VALUES
                                                                           ('Level 1', 300, 120),
                                                                           ('Level 2', 300, 200),
                                                                           ('Level 3', 300, 50),
                                                                           ('Level 4', 300, 280);

-- Insert sample events
INSERT INTO event (title, description, start_date, end_date) VALUES
                                                                 ('Concert at Stadium', 'Large concert expected to cause high traffic', '2025-06-15 18:00:00', '2025-06-15 23:00:00'),
                                                                 ('City Marathon', 'Road closures and increased parking demand', '2025-07-10 07:00:00', '2025-07-10 14:00:00'),
                                                                 ('Tech Conference', 'Business event near the parking', '2025-08-05 09:00:00', '2025-08-07 18:00:00');