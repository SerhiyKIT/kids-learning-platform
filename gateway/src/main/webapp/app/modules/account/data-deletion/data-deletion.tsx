import React, { useState } from 'react';
import { Box, Typography, Card, CardContent, TextField, Button, Alert, CircularProgress, Stepper, Step, StepLabel } from '@mui/material';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import useLocalStorage from 'app/shared/hooks/useLocalStorage';
import { getThemeStyles } from 'app/modules/home/home.styles';
import { useAppSelector } from 'app/config/store';

const STEPS = ['Підтвердження особи', 'Вибір даних', 'Остаточне видалення'];

export const DataDeletionPage = () => {
  const [isDark] = useLocalStorage('app-theme-dark', true);
  const styles = getThemeStyles(isDark);
  const account = useAppSelector(state => state.authentication.account);

  const [activeStep, setActiveStep] = useState(0);
  const [reason, setReason] = useState('');
  const [confirmEmail, setConfirmEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleNext = () => setActiveStep(prev => prev + 1);
  const handleBack = () => setActiveStep(prev => prev - 1);

  const handleSubmit = async () => {
    if (confirmEmail.toLowerCase() !== (account?.email ?? '').toLowerCase()) {
      setError('Email не збігається з вашим акаунтом.');
      return;
    }
    setLoading(true);
    setError('');
    // In production: POST /api/account/data-deletion-request
    await new Promise(resolve => setTimeout(resolve, 1200));
    setLoading(false);
    setSubmitted(true);
  };

  if (submitted) {
    return (
      <Box sx={{ maxWidth: 600, mx: 'auto', mt: 6, px: 2, textAlign: 'center' }}>
        <Typography variant="h2" sx={{ mb: 2 }}>
          ✅
        </Typography>
        <Typography variant="h5" sx={{ fontWeight: 'bold', color: styles.text, mb: 2 }}>
          Запит на видалення отримано
        </Typography>
        <Typography variant="body1" sx={{ color: styles.textSecondary }}>
          Ми обробимо ваш запит протягом 30 днів відповідно до GDPR / COPPA. Підтвердження буде надіслано на{' '}
          <strong>{account?.email}</strong>.
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: styles.bg, color: styles.text, py: 6, px: 2 }}>
      <Box sx={{ maxWidth: 660, mx: 'auto' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 4 }}>
          <DeleteForeverIcon sx={{ color: '#f44336', fontSize: 36 }} />
          <Typography variant="h4" sx={{ fontWeight: 'bold', color: styles.text }}>
            Запит на видалення даних
          </Typography>
        </Box>

        <Alert severity="warning" sx={{ borderRadius: '12px', mb: 4 }}>
          Видалення даних є незворотнім. Весь прогрес навчання, налаштування та зв&apos;язки будуть видалені.
        </Alert>

        <Stepper activeStep={activeStep} sx={{ mb: 4, '& .MuiStepLabel-label': { color: styles.textSecondary } }}>
          {STEPS.map(label => (
            <Step key={label}>
              <StepLabel>{label}</StepLabel>
            </Step>
          ))}
        </Stepper>

        <Card sx={{ backgroundColor: styles.cardBg, border: styles.cardBorder, borderRadius: '20px' }}>
          <CardContent sx={{ p: 3 }}>
            {activeStep === 0 && (
              <Box>
                <Typography variant="h6" sx={{ color: styles.text, mb: 2 }}>
                  Ваш акаунт
                </Typography>
                <Typography variant="body2" sx={{ color: styles.textSecondary, mb: 1 }}>
                  Логін: <strong style={{ color: styles.text }}>{account?.login}</strong>
                </Typography>
                <Typography variant="body2" sx={{ color: styles.textSecondary, mb: 3 }}>
                  Email: <strong style={{ color: styles.text }}>{account?.email}</strong>
                </Typography>
                <Typography variant="body2" sx={{ color: styles.textSecondary, mb: 2 }}>
                  Вкажіть причину видалення (необов&apos;язково):
                </Typography>
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  value={reason}
                  onChange={e => setReason(e.target.value)}
                  placeholder="Наприклад: дитина більше не навчається на платформі"
                  sx={{
                    '& .MuiOutlinedInput-root': { color: styles.text, borderRadius: '10px' },
                    '& .MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(255,255,255,0.2)' },
                  }}
                />
              </Box>
            )}

            {activeStep === 1 && (
              <Box>
                <Typography variant="h6" sx={{ color: styles.text, mb: 2 }}>
                  Що буде видалено:
                </Typography>
                {[
                  'Обліковий запис та профіль',
                  'Весь прогрес навчання та результати уроків',
                  "Пов'язані записи батьків та дітей",
                  'Збережені налаштування та уподобання',
                  'Журнали активності (де дозволено законом)',
                ].map(item => (
                  <Box key={item} sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <Box sx={{ width: 6, height: 6, borderRadius: '50%', backgroundColor: '#f44336', flexShrink: 0 }} />
                    <Typography variant="body2" sx={{ color: styles.text }}>
                      {item}
                    </Typography>
                  </Box>
                ))}
                <Alert severity="info" sx={{ mt: 2, borderRadius: '8px' }}>
                  Деякі дані можуть зберігатись до 30 днів для виконання юридичних зобов&apos;язань.
                </Alert>
              </Box>
            )}

            {activeStep === 2 && (
              <Box>
                <Typography variant="h6" sx={{ color: styles.text, mb: 2 }}>
                  Підтвердіть email для завершення
                </Typography>
                <Typography variant="body2" sx={{ color: styles.textSecondary, mb: 2 }}>
                  Введіть email вашого акаунту (<strong>{account?.email}</strong>) для підтвердження:
                </Typography>
                <TextField
                  fullWidth
                  type="email"
                  value={confirmEmail}
                  onChange={e => setConfirmEmail(e.target.value)}
                  error={!!error}
                  helperText={error}
                  placeholder="your@email.com"
                  sx={{
                    '& .MuiOutlinedInput-root': { color: styles.text, borderRadius: '10px' },
                    '& .MuiOutlinedInput-notchedOutline': { borderColor: error ? '#f44336' : 'rgba(255,255,255,0.2)' },
                    '& .MuiFormHelperText-root': { color: '#f44336' },
                  }}
                />
              </Box>
            )}

            <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 3 }}>
              <Button disabled={activeStep === 0} onClick={handleBack} sx={{ color: styles.textSecondary, borderRadius: '10px' }}>
                Назад
              </Button>

              {activeStep < 2 ? (
                <Button
                  variant="contained"
                  onClick={handleNext}
                  sx={{ backgroundColor: styles.primary, color: '#000', borderRadius: '10px', fontWeight: 'bold' }}
                >
                  Далі
                </Button>
              ) : (
                <Button
                  variant="contained"
                  onClick={handleSubmit}
                  disabled={loading || !confirmEmail}
                  sx={{
                    backgroundColor: '#f44336',
                    color: '#fff',
                    borderRadius: '10px',
                    fontWeight: 'bold',
                    minWidth: 140,
                  }}
                >
                  {loading ? <CircularProgress size={20} sx={{ color: '#fff' }} /> : 'Видалити дані'}
                </Button>
              )}
            </Box>
          </CardContent>
        </Card>
      </Box>
    </Box>
  );
};

export default DataDeletionPage;
