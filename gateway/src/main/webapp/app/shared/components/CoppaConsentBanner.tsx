import React, { useState } from 'react';
import { Box, Button, Typography, Paper, Checkbox, FormControlLabel, Link, Collapse } from '@mui/material';
import ChildCareIcon from '@mui/icons-material/ChildCare';
import useLocalStorage from 'app/shared/hooks/useLocalStorage';

const STORAGE_KEY = 'coppa-consent-given';

export const CoppaConsentBanner = () => {
  const [consentGiven, setConsentGiven] = useLocalStorage<boolean>(STORAGE_KEY, false);
  const [checked, setChecked] = useState(false);
  const [expanded, setExpanded] = useState(false);

  if (consentGiven) return null;

  return (
    <Box
      sx={{
        position: 'fixed',
        bottom: 0,
        left: 0,
        right: 0,
        zIndex: 9999,
        p: { xs: 1, sm: 2 },
        backgroundColor: 'rgba(0,0,0,0.85)',
        backdropFilter: 'blur(8px)',
      }}
    >
      <Paper
        elevation={0}
        sx={{
          maxWidth: 860,
          mx: 'auto',
          p: { xs: 2, sm: 3 },
          backgroundColor: 'transparent',
          color: '#fff',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2 }}>
          <ChildCareIcon sx={{ color: '#7c4dff', fontSize: 32, mt: 0.3, flexShrink: 0 }} />
          <Box sx={{ flex: 1 }}>
            <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 0.5 }}>
              Захист персональних даних дітей (COPPA)
            </Typography>
            <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.75)', mb: 1 }}>
              Цей сервіс призначений для дітей від 4 до 10 років. Ми дотримуємось COPPA (Children&apos;s Online Privacy Protection Act) та
              GDPR-K. Продовжуючи, ви підтверджуєте, що є батьком / опікуном або що отримали дозвіл батьків.
            </Typography>

            <Button
              size="small"
              onClick={() => setExpanded(!expanded)}
              sx={{ color: '#7c4dff', p: 0, minWidth: 0, mb: 1, textTransform: 'none', fontSize: '0.8rem' }}
            >
              {expanded ? '▲ Сховати деталі' : '▼ Детальніше про збір даних'}
            </Button>

            <Collapse in={expanded}>
              <Box
                sx={{
                  mb: 2,
                  p: 1.5,
                  borderRadius: '8px',
                  backgroundColor: 'rgba(255,255,255,0.05)',
                  border: '1px solid rgba(255,255,255,0.1)',
                }}
              >
                <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.7)', mb: 1 }}>
                  <strong>Що ми збираємо:</strong>
                </Typography>
                <Typography component="ul" variant="body2" sx={{ color: 'rgba(255,255,255,0.65)', pl: 2, m: 0 }}>
                  <li>Ім&apos;я користувача та email (лише для батьків/вчителів)</li>
                  <li>Прогрес навчання: пройдені уроки та результати</li>
                  <li>Технічні дані: IP-адреса, браузер (для безпеки)</li>
                </Typography>
                <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.7)', mt: 1, mb: 0.5 }}>
                  <strong>Що ми НЕ збираємо:</strong> геолокацію, фото, поведінкову рекламу.
                </Typography>
                <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.65)' }}>
                  Ви можете{' '}
                  <Link href="/account/data-deletion" sx={{ color: '#7c4dff' }}>
                    запросити видалення даних
                  </Link>{' '}
                  у будь-який час.
                </Typography>
              </Box>
            </Collapse>

            <FormControlLabel
              control={
                <Checkbox
                  checked={checked}
                  onChange={e => setChecked(e.target.checked)}
                  sx={{ color: '#7c4dff', '&.Mui-checked': { color: '#7c4dff' } }}
                  size="small"
                />
              }
              label={
                <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.85)' }}>
                  Я підтверджую, що є батьком / опікуном дитини або що отримав(ла) згоду батьків на використання сервісу
                </Typography>
              }
            />
          </Box>

          <Button
            variant="contained"
            disabled={!checked}
            onClick={() => setConsentGiven(true)}
            sx={{
              flexShrink: 0,
              backgroundColor: '#7c4dff',
              color: '#fff',
              borderRadius: '10px',
              fontWeight: 'bold',
              px: 3,
              alignSelf: 'center',
              '&:hover': { backgroundColor: '#6200ea' },
              '&:disabled': { backgroundColor: 'rgba(124,77,255,0.3)', color: 'rgba(255,255,255,0.4)' },
            }}
          >
            Погоджуюсь
          </Button>
        </Box>
      </Paper>
    </Box>
  );
};

export default CoppaConsentBanner;
