-- Seed reference data for modules. Idempotent so it's safe to re-run.
INSERT INTO modules (code, name, sort_order) VALUES
    ('safety',  'Безпека',   1),
    ('math',    'Математика', 2),
    ('logic',   'Логіка',    3),
    ('reading', 'Читання',   4),
    ('writing', 'Письмо',    5)
ON CONFLICT (code) DO NOTHING;
