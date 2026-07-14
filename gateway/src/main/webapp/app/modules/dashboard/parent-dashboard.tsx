import React, { useEffect, useState } from 'react';
import { Container, Row, Col } from 'reactstrap';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Divider,
  TextField,
  Button,
  Alert,
  CircularProgress,
  LinearProgress,
  Chip,
} from '@mui/material';
import { translate } from 'react-jhipster';
import useLocalStorage from 'app/shared/hooks/useLocalStorage';
import { getThemeStyles } from '../home/home.styles';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getProgressForStudent } from 'app/entities/progress/progress.reducer';

import AccessTimeIcon from '@mui/icons-material/AccessTime';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import LinkIcon from '@mui/icons-material/Link';

const StatCard = ({ icon, title, value, color, styles }: any) => (
  <Card sx={{ backgroundColor: styles.cardBg, border: styles.cardBorder, borderRadius: '15px', height: '100%' }}>
    <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
      <Box sx={{ p: 1.5, borderRadius: '50%', backgroundColor: `${color}20` }}>
        {React.cloneElement(icon, { sx: { color, fontSize: 30 } })}
      </Box>
      <Box>
        <Typography variant="body2" sx={{ color: styles.textSecondary }}>
          {title}
        </Typography>
        <Typography variant="h5" sx={{ fontWeight: 'bold', color: styles.text }}>
          {value}
        </Typography>
      </Box>
    </CardContent>
  </Card>
);

export const ParentDashboard = () => {
  const dispatch = useAppDispatch();
  const [isDark] = useLocalStorage('app-theme-dark', true);
  const styles = getThemeStyles(isDark);

  const [linkedChildId, setLinkedChildId] = useLocalStorage<number | null>('parent-linked-child-id', null);
  const [inputId, setInputId] = useState('');

  const progressList = useAppSelector(state => state.progress.entities);
  const progressLoading = useAppSelector(state => state.progress.loading);

  useEffect(() => {
    if (linkedChildId) {
      dispatch(getProgressForStudent(linkedChildId));
    }
  }, [linkedChildId]);

  const handleLinkChild = () => {
    const parsed = parseInt(inputId, 10);
    if (!isNaN(parsed) && parsed > 0) {
      setLinkedChildId(parsed);
      setInputId('');
    }
  };

  const completedProgress = progressList.filter(p => p.status === 'COMPLETED');
  const avgScore =
    completedProgress.length > 0 ? Math.round(completedProgress.reduce((sum, p) => sum + (p.score ?? 0), 0) / completedProgress.length) : 0;
  const completedCount = completedProgress.length;
  // approximate: each lesson ~15 min
  const totalMinutes = completedCount * 15;
  const timeSpent = totalMinutes >= 60 ? `${Math.floor(totalMinutes / 60)} год ${totalMinutes % 60} хв` : `${totalMinutes} хв`;

  const recentActivity = [...progressList]
    .filter(p => p.completedAt != null)
    .sort((a, b) => new Date(b.completedAt ?? 0).getTime() - new Date(a.completedAt ?? 0).getTime())
    .slice(0, 5);

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: styles.bg, color: styles.text, py: 4 }}>
      <Container>
        <Typography variant="h3" sx={{ mb: 4, fontWeight: 'bold', color: styles.text }}>
          {translate('dashboard.parent.title')}
        </Typography>

        {/* Link child section */}
        {!linkedChildId ? (
          <Card sx={{ backgroundColor: styles.cardBg, border: styles.cardBorder, borderRadius: '20px', mb: 4 }}>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                <LinkIcon sx={{ color: styles.primary }} />
                <Typography variant="h6" sx={{ color: styles.text }}>
                  Прив&apos;яжіть обліковий запис дитини
                </Typography>
              </Box>
              <Typography variant="body2" sx={{ color: styles.textSecondary, mb: 2 }}>
                Введіть ID облікового запису вашої дитини, щоб бачити її прогрес.
              </Typography>
              <Box sx={{ display: 'flex', gap: 2 }}>
                <TextField
                  size="small"
                  label="ID дитини"
                  value={inputId}
                  onChange={e => setInputId(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && handleLinkChild()}
                  sx={{
                    '& .MuiOutlinedInput-root': { borderRadius: '10px', color: styles.text },
                    '& .MuiInputLabel-root': { color: styles.textSecondary },
                    '& .MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(255,255,255,0.2)' },
                  }}
                />
                <Button
                  variant="contained"
                  onClick={handleLinkChild}
                  sx={{ borderRadius: '10px', backgroundColor: styles.primary, color: '#000', fontWeight: 'bold' }}
                >
                  Прив&apos;язати
                </Button>
              </Box>
            </CardContent>
          </Card>
        ) : (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
            <Chip
              icon={<LinkIcon />}
              label={`Дитина ID: ${linkedChildId}`}
              sx={{ backgroundColor: `${styles.primary}20`, color: styles.primary, border: `1px solid ${styles.primary}` }}
            />
            <Button
              size="small"
              variant="outlined"
              sx={{ color: styles.textSecondary, borderColor: 'rgba(255,255,255,0.2)' }}
              onClick={() => setLinkedChildId(null)}
            >
              Змінити
            </Button>
          </Box>
        )}

        {linkedChildId && progressLoading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
            <CircularProgress sx={{ color: styles.primary }} />
          </Box>
        )}

        {linkedChildId && !progressLoading && progressList.length === 0 && (
          <Alert severity="info" sx={{ borderRadius: '12px', mb: 4 }}>
            Ще немає записів про навчання для цієї дитини.
          </Alert>
        )}

        {linkedChildId && !progressLoading && progressList.length > 0 && (
          <>
            {/* TOP STATS */}
            <Row className="mb-4">
              <Col md="4" className="mb-3">
                <StatCard
                  icon={<TrendingUpIcon />}
                  title={translate('dashboard.parent.avgScore')}
                  value={`${avgScore}%`}
                  color={styles.primary}
                  styles={styles}
                />
              </Col>
              <Col md="4" className="mb-3">
                <StatCard
                  icon={<AccessTimeIcon />}
                  title={translate('dashboard.parent.studyTime')}
                  value={timeSpent}
                  color={styles.secondary}
                  styles={styles}
                />
              </Col>
              <Col md="4" className="mb-3">
                <StatCard
                  icon={<CheckCircleIcon />}
                  title={translate('dashboard.parent.completedLessons')}
                  value={completedCount}
                  color="#00c853"
                  styles={styles}
                />
              </Col>
            </Row>

            <Row>
              <Col md="8">
                <Card sx={{ backgroundColor: styles.cardBg, border: styles.cardBorder, borderRadius: '20px', p: 2 }}>
                  <CardContent>
                    <Typography variant="h5" sx={{ mb: 3, color: styles.text }}>
                      {translate('dashboard.parent.lastActivity')}
                    </Typography>
                    {recentActivity.map((p, i) => {
                      const date = p.completedAt
                        ? new Date(p.completedAt).toLocaleDateString('uk-UA', {
                            day: '2-digit',
                            month: 'short',
                            hour: '2-digit',
                            minute: '2-digit',
                          })
                        : '';
                      return (
                        <Box key={p.id ?? i} sx={{ mb: 2 }}>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                            <Typography sx={{ color: styles.text }}>
                              {translate('dashboard.parent.lesson')}: {p.lesson?.title ?? `Урок #${p.lesson?.id}`}
                            </Typography>
                            <Typography sx={{ color: '#00c853', fontWeight: 'bold' }}>{p.score ?? 0}/100</Typography>
                          </Box>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <LinearProgress
                              variant="determinate"
                              value={p.score ?? 0}
                              sx={{
                                flex: 1,
                                height: 4,
                                borderRadius: 2,
                                backgroundColor: 'rgba(255,255,255,0.1)',
                                '& .MuiLinearProgress-bar': { backgroundColor: styles.primary },
                              }}
                            />
                            <Typography variant="caption" sx={{ color: styles.textSecondary, whiteSpace: 'nowrap' }}>
                              {date}
                            </Typography>
                          </Box>
                          {i !== recentActivity.length - 1 && <Divider sx={{ my: 2, borderColor: 'rgba(255,255,255,0.1)' }} />}
                        </Box>
                      );
                    })}
                  </CardContent>
                </Card>
              </Col>

              <Col md="4">
                <Card sx={{ backgroundColor: styles.cardBg, border: styles.cardBorder, borderRadius: '20px', p: 2, height: '100%' }}>
                  <CardContent>
                    <Typography variant="h6" sx={{ color: styles.text, mb: 2 }}>
                      {translate('dashboard.parent.aiRecommendations')}
                    </Typography>
                    <Typography variant="body2" sx={{ color: styles.textSecondary, fontStyle: 'italic' }}>
                      {completedCount < 3
                        ? 'Дитина тільки починає — заохочуйте проходити більше уроків щодня!'
                        : avgScore >= 80
                          ? 'Чудові результати! Спробуйте перейти до складніших рівнів.'
                          : 'Варто повторити пройдені теми — середній бал можна покращити.'}
                    </Typography>
                  </CardContent>
                </Card>
              </Col>
            </Row>
          </>
        )}
      </Container>
    </Box>
  );
};

export default ParentDashboard;
