import React from 'react';
import { Container, Row, Col } from 'reactstrap';
import { Box, Typography, Card, CardContent, Button, LinearProgress, Chip, Avatar } from '@mui/material';
import { Link } from 'react-router-dom';
import useLocalStorage from 'app/shared/hooks/useLocalStorage';
import { getThemeStyles } from '../home/home.styles'; // Імпорт стилів з Home

// Іконки
import PlayCircleOutlineIcon from '@mui/icons-material/PlayCircleOutline';
import StarIcon from '@mui/icons-material/Star';
import LockIcon from '@mui/icons-material/Lock';
import BoltIcon from '@mui/icons-material/Bolt';

export const StudentDashboard = () => {
  const [isDark] = useLocalStorage('app-theme-dark', true);
  const styles = getThemeStyles(isDark);

  // Мок-дані (пізніше замінимо на реальні з API)
  const stats = { level: 5, xp: 2450, nextLevelXp: 3000, streak: 12 };
  const missions = [
    { id: 1, title: 'Таємниці Гравітації', subject: 'Фізика', status: 'active', xp: 500 },
    { id: 2, title: 'Дроби та Піца', subject: 'Математика', status: 'locked', xp: 300 },
    { id: 3, title: 'Світ Рослин', subject: 'Біологія', status: 'completed', xp: 250 },
  ];

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: styles.bg, color: styles.text, py: 4, transition: '0.3s' }}>
      <Container>
        {/* HERO HEADER */}
        <Row className="mb-5 align-items-center">
          <Col md="8">
            <Typography variant="h3" sx={{ fontWeight: 'bold', mb: 1 }}>
              Привіт, Космічний Рейнджере! 🚀
            </Typography>
            <Typography variant="body1" sx={{ color: styles.textSecondary }}>
              Готовий до нових відкриттів?
            </Typography>
          </Col>
          <Col md="4" className="text-end">
            <Chip
              icon={<BoltIcon sx={{ color: '#ffeb3b !important' }} />}
              label={`${stats.streak} днів стрік`}
              sx={{ backgroundColor: 'rgba(255, 235, 59, 0.2)', color: styles.text, border: '1px solid #ffeb3b', fontSize: '1.1rem', p: 1 }}
            />
          </Col>
        </Row>

        {/* STATS BAR */}
        <Row className="mb-5">
          <Col md="12">
            <Card sx={{ backgroundColor: styles.cardBg, border: styles.cardBorder, borderRadius: '20px' }}>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="h6" sx={{ color: styles.primary, fontWeight: 'bold' }}>
                    РІВЕНЬ {stats.level}
                  </Typography>
                  <Typography variant="body2" sx={{ color: styles.textSecondary }}>
                    {stats.xp} / {stats.nextLevelXp} XP
                  </Typography>
                </Box>
                <LinearProgress
                  variant="determinate"
                  value={(stats.xp / stats.nextLevelXp) * 100}
                  sx={{
                    height: 10,
                    borderRadius: 5,
                    backgroundColor: 'rgba(255,255,255,0.1)',
                    '& .MuiLinearProgress-bar': { backgroundColor: styles.primary },
                  }}
                />
              </CardContent>
            </Card>
          </Col>
        </Row>

        {/* MISSIONS GRID */}
        <Typography variant="h4" sx={{ mb: 3, fontWeight: 'bold' }}>
          Твої Місії
        </Typography>
        <Row>
          {missions.map(mission => (
            <Col md="4" key={mission.id} className="mb-4">
              <Card
                sx={{
                  backgroundColor: styles.cardBg,
                  border: styles.cardBorder,
                  borderRadius: '20px',
                  height: '100%',
                  opacity: mission.status === 'locked' ? 0.6 : 1,
                  transition: '0.3s',
                  '&:hover': { transform: mission.status !== 'locked' ? 'scale(1.03)' : 'none', boxShadow: styles.glow },
                }}
              >
                <CardContent sx={{ textAlign: 'center', py: 4 }}>
                  <Box sx={{ mb: 2 }}>
                    {mission.status === 'locked' ? (
                      <LockIcon sx={{ fontSize: 50, color: styles.textSecondary }} />
                    ) : (
                      <PlayCircleOutlineIcon sx={{ fontSize: 50, color: styles.primary }} />
                    )}
                  </Box>
                  <Typography variant="h5" sx={{ fontWeight: 'bold', mb: 1, color: styles.text }}>
                    {mission.title}
                  </Typography>
                  <Chip
                    label={mission.subject}
                    size="small"
                    sx={{ mb: 2, backgroundColor: 'rgba(255,255,255,0.1)', color: styles.textSecondary }}
                  />

                  <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 1, mb: 3 }}>
                    <StarIcon sx={{ color: '#ffeb3b', fontSize: 20 }} />
                    <Typography variant="body2" sx={{ color: styles.text }}>
                      +{mission.xp} XP
                    </Typography>
                  </Box>

                  <Button
                    variant={mission.status === 'active' ? 'contained' : 'outlined'}
                    disabled={mission.status === 'locked'}
                    component={Link}
                    to={`/lesson/${mission.id}`}
                    sx={{
                      borderRadius: '20px',
                      px: 4,
                      backgroundColor: mission.status === 'active' ? styles.primary : 'transparent',
                      color: mission.status === 'active' ? '#000' : styles.textSecondary,
                      borderColor: styles.textSecondary,
                    }}
                  >
                    {mission.status === 'active' ? 'ПОЧАТИ' : mission.status === 'completed' ? 'ПОВТОРИТИ' : 'ЗАБЛОКОВАНО'}
                  </Button>
                </CardContent>
              </Card>
            </Col>
          ))}
        </Row>
      </Container>
    </Box>
  );
};

export default StudentDashboard;
