import React, { useEffect, useState } from 'react';
import { Container, Row, Col } from 'reactstrap';
import {
  Box,
  Typography,
  Button,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Chip,
} from '@mui/material';
import { translate } from 'react-jhipster';
import useLocalStorage from 'app/shared/hooks/useLocalStorage';
import { getThemeStyles } from '../home/home.styles';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getEntities as getLessons, createEntity as createLesson } from 'app/entities/lesson/lesson.reducer';
import { getEntities as getSubjects } from 'app/entities/subject/subject.reducer';

import AddIcon from '@mui/icons-material/Add';
import GroupIcon from '@mui/icons-material/Group';
import BookIcon from '@mui/icons-material/Book';

const EMPTY_LESSON = {
  title: '',
  difficultyLevel: 1,
  subjectId: '',
  content: '',
};

const DIFFICULTY_LABELS: Record<number, string> = {
  1: 'Легкий',
  2: 'Нижче середнього',
  3: 'Середній',
  4: 'Вище середнього',
  5: 'Складний',
};

const CONTENT_PLACEHOLDER = JSON.stringify(
  {
    theme: 'Назва теми',
    questions: [{ id: 1, text: 'Питання?', options: ['А', 'Б', 'В', 'Г'], correctAnswer: 'А' }],
  },
  null,
  2,
);

export const TeacherDashboard = () => {
  const dispatch = useAppDispatch();
  const [isDark] = useLocalStorage('app-theme-dark', true);
  const styles = getThemeStyles(isDark);

  const lessons = useAppSelector(state => state.lesson.entities);
  const lessonsLoading = useAppSelector(state => state.lesson.loading);
  const subjects = useAppSelector(state => state.subject.entities);
  const creating = useAppSelector(state => state.lesson.updating);
  const createSuccess = useAppSelector(state => state.lesson.updateSuccess);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_LESSON);
  const [contentError, setContentError] = useState('');

  useEffect(() => {
    dispatch(getLessons({ page: 0, size: 50, sort: 'id,asc' }));
    dispatch(getSubjects());
  }, []);

  useEffect(() => {
    if (createSuccess && dialogOpen) {
      setDialogOpen(false);
      setForm(EMPTY_LESSON);
      setContentError('');
    }
  }, [createSuccess]);

  const handleFieldChange = (field: string) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setForm(prev => ({ ...prev, [field]: e.target.value }));
  };

  const handleSubmit = () => {
    if (!form.title.trim()) return;
    try {
      if (form.content) JSON.parse(form.content);
      setContentError('');
    } catch {
      setContentError('JSON формат невірний. Перевірте структуру контенту.');
      return;
    }

    const subjectId = form.subjectId ? Number(form.subjectId) : undefined;
    dispatch(
      createLesson({
        title: form.title,
        difficultyLevel: Number(form.difficultyLevel),
        content: form.content || undefined,
        subject: subjectId ? { id: subjectId } : undefined,
      }),
    );
  };

  const inputSx = {
    '& .MuiOutlinedInput-root': { color: styles.text, borderRadius: '10px' },
    '& .MuiInputLabel-root': { color: styles.textSecondary },
    '& .MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(255,255,255,0.2)' },
    '& .MuiSelect-icon': { color: styles.text },
  };

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: styles.bg, color: styles.text, py: 4 }}>
      <Container>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
          <Typography variant="h3" sx={{ fontWeight: 'bold', color: styles.text }}>
            {translate('dashboard.teacher.title')}
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setDialogOpen(true)}
            sx={{
              backgroundColor: '#00c853',
              color: '#fff',
              borderRadius: '10px',
              px: 3,
              py: 1,
              '&:hover': { backgroundColor: '#009624' },
            }}
          >
            {translate('dashboard.teacher.createLesson')}
          </Button>
        </Box>

        {/* STATS ROW */}
        <Row className="mb-4">
          <Col md="4" className="mb-3">
            <Card sx={{ backgroundColor: styles.cardBg, border: styles.cardBorder, borderRadius: '15px' }}>
              <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Box sx={{ p: 1.5, borderRadius: '50%', backgroundColor: `${styles.primary}20` }}>
                  <BookIcon sx={{ color: styles.primary, fontSize: 30 }} />
                </Box>
                <Box>
                  <Typography variant="body2" sx={{ color: styles.textSecondary }}>
                    Всього уроків
                  </Typography>
                  <Typography variant="h5" sx={{ fontWeight: 'bold', color: styles.text }}>
                    {lessons.length}
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          </Col>
          <Col md="4" className="mb-3">
            <Card sx={{ backgroundColor: styles.cardBg, border: styles.cardBorder, borderRadius: '15px' }}>
              <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Box sx={{ p: 1.5, borderRadius: '50%', backgroundColor: '#00c85320' }}>
                  <GroupIcon sx={{ color: '#00c853', fontSize: 30 }} />
                </Box>
                <Box>
                  <Typography variant="body2" sx={{ color: styles.textSecondary }}>
                    Предметів
                  </Typography>
                  <Typography variant="h5" sx={{ fontWeight: 'bold', color: styles.text }}>
                    {subjects.length}
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          </Col>
        </Row>

        {/* LESSONS TABLE */}
        <Row className="mb-4">
          <Col md="12">
            <Card sx={{ backgroundColor: styles.cardBg, border: styles.cardBorder, borderRadius: '20px' }}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
                  <GroupIcon sx={{ color: styles.primary }} />
                  <Typography variant="h5" sx={{ color: styles.text }}>
                    {translate('dashboard.teacher.classPerformance')}
                  </Typography>
                </Box>

                {lessonsLoading && (
                  <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                    <CircularProgress sx={{ color: styles.primary }} />
                  </Box>
                )}

                {!lessonsLoading && lessons.length === 0 && (
                  <Alert severity="info" sx={{ borderRadius: '12px' }}>
                    Уроків ще немає. Створіть перший урок!
                  </Alert>
                )}

                {!lessonsLoading && lessons.length > 0 && (
                  <Table sx={{ '& td, & th': { color: styles.text, borderColor: 'rgba(255,255,255,0.1)' } }}>
                    <TableHead>
                      <TableRow>
                        <TableCell>ID</TableCell>
                        <TableCell>{translate('dashboard.teacher.table.studentName')}</TableCell>
                        <TableCell>Предмет</TableCell>
                        <TableCell>{translate('dashboard.teacher.table.progress')}</TableCell>
                        <TableCell align="right">{translate('dashboard.teacher.table.actions')}</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {lessons.map(lesson => (
                        <TableRow key={lesson.id}>
                          <TableCell sx={{ color: styles.textSecondary }}>{lesson.id}</TableCell>
                          <TableCell sx={{ fontWeight: 'bold' }}>{lesson.title}</TableCell>
                          <TableCell>
                            {lesson.subject?.title ? (
                              <Chip
                                label={lesson.subject.title}
                                size="small"
                                sx={{ backgroundColor: `${styles.primary}20`, color: styles.primary }}
                              />
                            ) : (
                              <Typography variant="caption" sx={{ color: styles.textSecondary }}>
                                —
                              </Typography>
                            )}
                          </TableCell>
                          <TableCell>
                            <Box sx={{ display: 'flex', gap: 0.5 }}>
                              {[1, 2, 3, 4, 5].map(d => (
                                <Box
                                  key={d}
                                  sx={{
                                    width: 10,
                                    height: 10,
                                    borderRadius: '2px',
                                    backgroundColor: d <= (lesson.difficultyLevel ?? 1) ? styles.primary : 'rgba(255,255,255,0.15)',
                                  }}
                                />
                              ))}
                            </Box>
                          </TableCell>
                          <TableCell align="right">
                            <Button size="small" component="a" href={`/lesson/${lesson.id}`} sx={{ color: styles.secondary }}>
                              {translate('dashboard.teacher.details')}
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </CardContent>
            </Card>
          </Col>
        </Row>
      </Container>

      {/* CREATE LESSON DIALOG */}
      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        maxWidth="md"
        fullWidth
        PaperProps={{ sx: { backgroundColor: styles.cardBg, color: styles.text, borderRadius: '20px' } }}
      >
        <DialogTitle sx={{ fontWeight: 'bold', color: styles.text }}>{translate('dashboard.teacher.createLesson')}</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important' }}>
          <TextField label="Назва уроку" value={form.title} onChange={handleFieldChange('title')} fullWidth required sx={inputSx} />

          <Box sx={{ display: 'flex', gap: 2 }}>
            <FormControl fullWidth sx={inputSx}>
              <InputLabel sx={{ color: styles.textSecondary }}>Предмет</InputLabel>
              <Select
                value={form.subjectId}
                label="Предмет"
                onChange={e => setForm(prev => ({ ...prev, subjectId: String(e.target.value) }))}
                sx={{ color: styles.text }}
              >
                <MenuItem value="">— без предмету —</MenuItem>
                {subjects.map(s => (
                  <MenuItem key={s.id} value={String(s.id)}>
                    {s.title}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControl fullWidth sx={inputSx}>
              <InputLabel sx={{ color: styles.textSecondary }}>Складність</InputLabel>
              <Select
                value={form.difficultyLevel}
                label="Складність"
                onChange={e => setForm(prev => ({ ...prev, difficultyLevel: Number(e.target.value) }))}
                sx={{ color: styles.text }}
              >
                {[1, 2, 3, 4, 5].map(d => (
                  <MenuItem key={d} value={d}>
                    {d} — {DIFFICULTY_LABELS[d]}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>

          <TextField
            label="Контент (JSON)"
            value={form.content}
            onChange={handleFieldChange('content')}
            multiline
            rows={8}
            fullWidth
            placeholder={CONTENT_PLACEHOLDER}
            error={!!contentError}
            helperText={contentError || 'Формат: { "theme": "...", "questions": [...] }'}
            sx={{ ...inputSx, '& .MuiFormHelperText-root': { color: contentError ? '#f44336' : styles.textSecondary } }}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2, gap: 1 }}>
          <Button onClick={() => setDialogOpen(false)} sx={{ color: styles.textSecondary, borderRadius: '10px' }}>
            Скасувати
          </Button>
          <Button
            onClick={handleSubmit}
            variant="contained"
            disabled={creating || !form.title.trim()}
            sx={{ backgroundColor: '#00c853', color: '#fff', borderRadius: '10px', fontWeight: 'bold', minWidth: 120 }}
          >
            {creating ? <CircularProgress size={20} sx={{ color: '#fff' }} /> : 'Створити урок'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default TeacherDashboard;
