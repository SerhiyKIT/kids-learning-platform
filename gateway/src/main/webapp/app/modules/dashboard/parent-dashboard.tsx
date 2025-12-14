import React from 'react';
import { Container, Row, Col } from 'reactstrap';
import { Box, Typography, Card, CardContent, Grid, Divider } from '@mui/material';
import useLocalStorage from 'app/shared/hooks/useLocalStorage';
import { getThemeStyles } from '../home/home.styles';

import AccessTimeIcon from '@mui/icons-material/AccessTime';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';

export const ParentDashboard = () => {
  const [isDark] = useLocalStorage('app-theme-dark', true);
  const styles = getThemeStyles(isDark);

  const childProgress = {
    name: 'Олексій',
    avgScore: 92,
    timeSpent: '4 год 15 хв',
    completedLessons: 12,
  };

  const StatCard = ({ icon, title, value, color }: any) => (
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

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: styles.bg, color: styles.text, py: 4 }}>
      <Container>
        <Typography variant="h3" sx={{ mb: 4, fontWeight: 'bold', color: styles.text }}>
          Моніторинг Успішності
        </Typography>

        {/* TOP STATS */}
        <Row className="mb-4">
          <Col md="4" className="mb-3">
            <StatCard icon={<TrendingUpIcon />} title="Середній бал" value={`${childProgress.avgScore}%`} color={styles.primary} />
          </Col>
          <Col md="4" className="mb-3">
            <StatCard icon={<AccessTimeIcon />} title="Час навчання (тиждень)" value={childProgress.timeSpent} color={styles.secondary} />
          </Col>
          <Col md="4" className="mb-3">
            <StatCard icon={<CheckCircleIcon />} title="Пройдено уроків" value={childProgress.completedLessons} color="#00c853" />
          </Col>
        </Row>

        {/* DETAILED REPORT */}
        <Row>
          <Col md="8">
            <Card sx={{ backgroundColor: styles.cardBg, border: styles.cardBorder, borderRadius: '20px', p: 2 }}>
              <CardContent>
                <Typography variant="h5" sx={{ mb: 3, color: styles.text }}>
                  Остання Активність
                </Typography>
                {[1, 2, 3].map(i => (
                  <Box key={i} sx={{ mb: 2 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Typography sx={{ color: styles.text }}>Урок: Основи програмування</Typography>
                      <Typography sx={{ color: '#00c853', fontWeight: 'bold' }}>100/100 балів</Typography>
                    </Box>
                    <Typography variant="body2" sx={{ color: styles.textSecondary }}>
                      Вчора о 14:30
                    </Typography>
                    {i !== 3 && <Divider sx={{ my: 2, borderColor: 'rgba(255,255,255,0.1)' }} />}
                  </Box>
                ))}
              </CardContent>
            </Card>
          </Col>

          <Col md="4">
            <Card sx={{ backgroundColor: styles.cardBg, border: styles.cardBorder, borderRadius: '20px', p: 2, height: '100%' }}>
              <CardContent>
                <Typography variant="h6" sx={{ color: styles.text, mb: 2 }}>
                  Рекомендації ШІ
                </Typography>
                <Typography variant="body2" sx={{ color: styles.textSecondary, fontStyle: 'italic' }}>
                  &quot;Олексій чудово справляється з математикою, але варто приділити більше уваги читанню. Спробуйте інтерактивні
                  історії.&quot;
                </Typography>
              </CardContent>
            </Card>
          </Col>
        </Row>
      </Container>
    </Box>
  );
};

export default ParentDashboard;
