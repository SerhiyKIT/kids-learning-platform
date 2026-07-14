import React, { useEffect } from 'react';
import { Container, Row, Col } from 'reactstrap';
import { Box, Typography, Card, CardContent, Button, LinearProgress, Chip, CircularProgress, Alert } from '@mui/material';
import { Link } from 'react-router-dom';
import { translate } from 'react-jhipster';
import useLocalStorage from 'app/shared/hooks/useLocalStorage';
import { getThemeStyles } from '../home/home.styles';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getEntities as getLessons } from 'app/entities/lesson/lesson.reducer';
import { getProgressForStudent } from 'app/entities/progress/progress.reducer';

import PlayCircleOutlineIcon from '@mui/icons-material/PlayCircleOutline';
import StarIcon from '@mui/icons-material/Star';
import BoltIcon from '@mui/icons-material/Bolt';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';

// Статичний XP за рівень складності
const XP_BY_DIFFICULTY: Record<number, number> = { 1: 100, 2: 200, 3: 300, 4: 400, 5: 500 };

const getMissionStatus = (lessonId: number, completedIds: Set<number>) => {
  if (completedIds.has(lessonId)) return 'completed';
  return 'active';
};

export const StudentDashboard = () => {
  const dispatch = useAppDispatch();
  const [isDark] = useLocalStorage('app-theme-dark', true);
  const styles = getThemeStyles(isDark);

  const account = useAppSelector(state => state.authentication.account);
  // Використовуємо numeric ID юзера як studentId до впровадження окремої моделі Student↔User
  const studentId: number = account?.id ?? 0;

  const lessons = useAppSelector(state => state.lesson.entities);
  const lessonsLoading = useAppSelector(state => state.lesson.loading);
  const progressList = useAppSelector(state => state.progress.entities);

  useEffect(() => {
    dispatch(getLessons({ page: 0, size: 20, sort: 'difficultyLevel,asc' }));
    if (studentId) {
      dispatch(getProgressForStudent(studentId));
    }
  }, [studentId]);

  const completedLessonIds = new Set<number>(
    progressList
      .filter(p => p.status === 'COMPLETED')
      .map(p => p.lesson?.id)
      .filter((lessonId): lessonId is number => lessonId !== undefined),
  );

  const totalXp = progressList.filter(p => p.status === 'COMPLETED').reduce((sum, p) => sum + (p.score ?? 0) * 10, 0);

  const level = Math.floor(totalXp / 500) + 1;
  const xpInLevel = totalXp % 500;
  const streak = progressList.length; // спрощений стрік — кількість завершень

  const getMissionButtonLabel = (status: string) => {
    if (status === 'active') return translate('dashboard.student.status.start');
    if (status === 'completed') return translate('dashboard.student.status.repeat');
    return translate('dashboard.student.status.locked');
  };

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: styles.bg, color: styles.text, py: 4, transition: '0.3s' }}>
      <Container>
        {/* HERO HEADER */}
        <Row className="mb-5 align-items-center">
          <Col md="8">
            <Typography variant="h3" sx={{ fontWeight: 'bold', mb: 1 }}>
              {translate('dashboard.student.greeting')}
            </Typography>
            <Typography variant="body1" sx={{ color: styles.textSecondary }}>
              {translate('dashboard.student.ready')}
            </Typography>
          </Col>
          <Col md="4" className="text-end">
            {streak > 0 && (
              <Chip
                icon={<BoltIcon sx={{ color: '#ffeb3b !important' }} />}
                label={translate('dashboard.student.streak', { count: streak })}
                sx={{
                  backgroundColor: 'rgba(255, 235, 59, 0.2)',
                  color: styles.text,
                  border: '1px solid #ffeb3b',
                  fontSize: '1.1rem',
                  p: 1,
                }}
              />
            )}
          </Col>
        </Row>

        {/* STATS BAR */}
        <Row className="mb-5">
          <Col md="12">
            <Card sx={{ backgroundColor: styles.cardBg, border: styles.cardBorder, borderRadius: '20px' }}>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="h6" sx={{ color: styles.primary, fontWeight: 'bold' }}>
                    {translate('dashboard.student.level')} {level}
                  </Typography>
                  <Typography variant="body2" sx={{ color: styles.textSecondary }}>
                    {xpInLevel} / 500 XP
                  </Typography>
                </Box>
                <LinearProgress
                  variant="determinate"
                  value={(xpInLevel / 500) * 100}
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
          {translate('dashboard.student.missionsTitle')}
        </Typography>

        {lessonsLoading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
            <CircularProgress sx={{ color: styles.primary }} />
          </Box>
        )}

        {!lessonsLoading && lessons.length === 0 && (
          <Alert severity="info" sx={{ borderRadius: '12px' }}>
            Уроків поки немає. Вчитель незабаром додасть нові місії! 🚀
          </Alert>
        )}

        <Row>
          {lessons.map(lesson => {
            if (!lesson.id) return null;
            const status = getMissionStatus(lesson.id, completedLessonIds);
            const xp = XP_BY_DIFFICULTY[lesson.difficultyLevel ?? 1] ?? 100;

            return (
              <Col md="4" key={lesson.id} className="mb-4">
                <Card
                  sx={{
                    backgroundColor: styles.cardBg,
                    border: status === 'completed' ? `1px solid ${styles.primary}` : styles.cardBorder,
                    borderRadius: '20px',
                    height: '100%',
                    transition: '0.3s',
                    '&:hover': { transform: 'scale(1.03)', boxShadow: styles.glow },
                  }}
                >
                  <CardContent sx={{ textAlign: 'center', py: 4 }}>
                    <Box sx={{ mb: 2 }}>
                      {status === 'completed' ? (
                        <CheckCircleIcon sx={{ fontSize: 50, color: styles.primary }} />
                      ) : (
                        <PlayCircleOutlineIcon sx={{ fontSize: 50, color: styles.primary }} />
                      )}
                    </Box>

                    <Typography variant="h5" sx={{ fontWeight: 'bold', mb: 1, color: styles.text }}>
                      {lesson.title}
                    </Typography>

                    {lesson.subject?.title && (
                      <Chip
                        label={lesson.subject.title}
                        size="small"
                        sx={{ mb: 2, backgroundColor: 'rgba(255,255,255,0.1)', color: styles.textSecondary }}
                      />
                    )}

                    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 1, mb: 3 }}>
                      <StarIcon sx={{ color: '#ffeb3b', fontSize: 20 }} />
                      <Typography variant="body2" sx={{ color: styles.text }}>
                        +{xp} XP
                      </Typography>
                    </Box>

                    <Button
                      variant={status === 'completed' ? 'outlined' : 'contained'}
                      component={Link}
                      to={`/lesson/${lesson.id}`}
                      sx={{
                        borderRadius: '20px',
                        px: 4,
                        backgroundColor: status === 'completed' ? 'transparent' : styles.primary,
                        color: status === 'completed' ? styles.primary : '#000',
                        borderColor: styles.primary,
                      }}
                    >
                      {getMissionButtonLabel(status)}
                    </Button>
                  </CardContent>
                </Card>
              </Col>
            );
          })}
        </Row>
      </Container>
    </Box>
  );
};

export default StudentDashboard;
