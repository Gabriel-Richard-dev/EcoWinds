-- Seed de feriados estaduais (CE) e municipais (Maracanaú) para os
-- anos correntes. Nacionais sao sincronizados via BrasilAPI no startup
-- e em job diario, portanto nao entram aqui.
--
-- ON CONFLICT garante idempotencia caso a migration seja re-aplicada
-- ou caso o admin ja tenha cadastrado manualmente.

INSERT INTO holidays (holiday_date, name, scope, source, type) VALUES
  -- Data Magna do Ceara (Abolicao da Escravidao no Ceara) -- 25/03
  ('2025-03-25', 'Data Magna do Ceará', 'STATE', 'SEED', 'civil'),
  ('2026-03-25', 'Data Magna do Ceará', 'STATE', 'SEED', 'civil'),
  ('2027-03-25', 'Data Magna do Ceará', 'STATE', 'SEED', 'civil'),

  -- Aniversario de Maracanau -- 04/03 (emancipado em 1983)
  ('2025-03-04', 'Aniversário de Maracanaú', 'MUNICIPAL', 'SEED', 'civil'),
  ('2026-03-04', 'Aniversário de Maracanaú', 'MUNICIPAL', 'SEED', 'civil'),
  ('2027-03-04', 'Aniversário de Maracanaú', 'MUNICIPAL', 'SEED', 'civil')
ON CONFLICT (holiday_date, scope) DO NOTHING;
