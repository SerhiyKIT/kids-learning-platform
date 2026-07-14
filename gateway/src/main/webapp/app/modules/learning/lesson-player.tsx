import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { translate } from 'react-jhipster';
import { useAppSelector, useAppDispatch } from 'app/config/store';
import { saveProgress } from 'app/entities/progress/progress.reducer';
import { getEntity as getLesson } from 'app/entities/lesson/lesson.reducer';
import { generateHint, clearHint } from 'app/entities/ai/ai.reducer';
import {
  Card,
  CardContent,
  Button,
  Typography,
  Box,
  CircularProgress,
  Alert,
  LinearProgress,
  Collapse,
  IconButton,
  Tooltip,
} from '@mui/material';
import useLocalStorage from 'app/shared/hooks/useLocalStorage';
import { getThemeStyles } from '../home/home.styles';
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh';
import CloseIcon from '@mui/icons-material/Close';

interface Question {
  id: number;
  text: string;
  options: string[];
  correctAnswer: string;
}

interface LessonContent {
  theme: string;
  questions: Question[];
}

export const LessonPlayer = () => {
  const { id } = useParams<'id'>();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const [isDark] = useLocalStorage('app-theme-dark', true);
  const styles = getThemeStyles(isDark);

  const account = useAppSelector(state => state.authentication.account);
  const lessonEntity = useAppSelector(state => state.lesson.entity);
  const lessonLoading = useAppSelector(state => state.lesson.loading);
  const aiHint = useAppSelector(state => state.ai.hint);
  const aiLoading = useAppSelector(state => state.ai.loading);

  const [lessonContent, setLessonContent] = useState<LessonContent | null>(null);
  const [parseError, setParseError] = useState(false);
  const [currentQuestion, setCurrentQuestion] = useState(0);
  const [score, setScore] = useState(0);
  const [finished, setFinished] = useState(false);
  const [selectedAnswer, setSelectedAnswer] = useState<string | null>(null);
  const [answered, setAnswered] = useState(false);
  const [showHint, setShowHint] = useState(false);

  useEffect(() => {
    if (id) {
      dispatch(getLesson(id));
    }
    return () => {
      dispatch(clearHint());
    };
  }, [id]);

  // Reset hint when question changes
  useEffect(() => {
    dispatch(clearHint());
    setShowHint(false);
  }, [currentQuestion]);

  useEffect(() => {
    if (lessonEntity?.content) {
      try {
        setLessonContent(JSON.parse(lessonEntity.content));
        setParseError(false);
      } catch {
        setParseError(true);
      }
    }
  }, [lessonEntity]);

  const handleAnswer = (answer: string) => {
    if (answered || !lessonContent) return;
    setSelectedAnswer(answer);
    setAnswered(true);

    const isCorrect = answer === lessonContent.questions[currentQuestion].correctAnswer;
    const newScore = score + (isCorrect ? 1 : 0);

    setTimeout(() => {
      if (currentQuestion + 1 < lessonContent.questions.length) {
        setCurrentQuestion(currentQuestion + 1);
        setSelectedAnswer(null);
        setAnswered(false);
        if (isCorrect) setScore(newScore);
      } else {
        const finalScore = isCorrect ? newScore : score;
        setScore(finalScore);
        finishLesson(finalScore);
      }
    }, 800);
  };

  const finishLesson = (finalScore: number) => {
    setFinished(true);
    const studentId: number = account?.id ?? 0;
    if (studentId) {
      dispatch(
        saveProgress({
          studentId,
          score: finalScore,
          status: 'COMPLETED',
          completedAt: new Date().toISOString(),
          lesson: { id: Number(id) },
        }),
      );
    }
  };

  const handleAskHint = () => {
    if (!lessonContent) return;
    const question = lessonContent.questions[currentQuestion];
    dispatch(
      generateHint({
        question: question.text,
        subject: lessonEntity?.subject?.title,
      }),
    );
    setShowHint(true);
  };

  if (lessonLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}>
        <CircularProgress sx={{ color: styles.primary }} />
      </Box>
    );
  }

  if (parseError || (!lessonLoading && !lessonContent)) {
    return (
      <Box sx={{ maxWidth: 600, mx: 'auto', mt: 6, px: 2 }}>
        <Alert severity="error" sx={{ borderRadius: '12px' }}>
          Урок порожній або формат контенту неправильний. Зверніться до вчителя.
        </Alert>
        <Button variant="outlined" sx={{ mt: 2 }} onClick={() => navigate('/student-dashboard')}>
          ← Назад до місій
        </Button>
      </Box>
    );
  }

  if (finished && lessonContent) {
    const total = lessonContent.questions.length;
    const percent = Math.round((score / total) * 100);
    return (
      <Box
        sx={{
          textAlign: 'center',
          mt: 5,
          p: 4,
          maxWidth: 500,
          mx: 'auto',
          backgroundColor: styles.cardBg,
          borderRadius: '24px',
          border: styles.cardBorder,
        }}
      >
        <Typography variant="h2" sx={{ mb: 2 }}>
          🎉
        </Typography>
        <Typography variant="h4" sx={{ fontWeight: 'bold', color: styles.text, mb: 1 }}>
          {translate('lesson.finished.title')}
        </Typography>
        <Typography variant="h5" sx={{ color: styles.primary, mb: 3 }}>
          {score} / {total} ({percent}%)
        </Typography>
        <LinearProgress
          variant="determinate"
          value={percent}
          sx={{ height: 12, borderRadius: 6, mb: 4, '& .MuiLinearProgress-bar': { backgroundColor: styles.primary } }}
        />
        <Button
          variant="contained"
          size="large"
          sx={{ borderRadius: '20px', px: 5, backgroundColor: styles.primary, color: '#000', fontWeight: 'bold' }}
          onClick={() => navigate('/student-dashboard')}
        >
          {translate('lesson.finished.back')}
        </Button>
      </Box>
    );
  }

  if (!lessonContent) return null;

  const question = lessonContent.questions[currentQuestion];
  const total = lessonContent.questions.length;

  return (
    <Box
      sx={{
        minHeight: '100vh',
        backgroundColor: styles.bg,
        color: styles.text,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        py: 6,
        px: 2,
      }}
    >
      {/* Progress bar */}
      <Box sx={{ width: '100%', maxWidth: 600, mb: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
          <Typography variant="body2" sx={{ color: styles.textSecondary }}>
            {translate('lesson.question')} {currentQuestion + 1} / {total}
          </Typography>
          <Typography variant="body2" sx={{ color: styles.primary }}>
            {lessonContent.theme}
          </Typography>
        </Box>
        <LinearProgress
          variant="determinate"
          value={((currentQuestion + 1) / total) * 100}
          sx={{
            height: 8,
            borderRadius: 4,
            backgroundColor: 'rgba(255,255,255,0.1)',
            '& .MuiLinearProgress-bar': { backgroundColor: styles.primary },
          }}
        />
      </Box>

      {/* Question card */}
      <Card
        sx={{
          maxWidth: 600,
          width: '100%',
          borderRadius: '24px',
          boxShadow: 5,
          backgroundColor: styles.cardBg,
          border: styles.cardBorder,
        }}
      >
        <CardContent sx={{ p: 4 }}>
          <Typography variant="h4" sx={{ textAlign: 'center', fontWeight: 'bold', color: styles.text, mb: 4, lineHeight: 1.3 }}>
            {question.text}
          </Typography>

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {question.options.map(opt => {
              const isSelected = selectedAnswer === opt;
              const isCorrect = opt === question.correctAnswer;
              let bgColor = styles.primary;
              if (answered) {
                bgColor = isCorrect ? '#00c853' : isSelected ? '#f44336' : 'rgba(255,255,255,0.08)';
              }

              return (
                <Button
                  key={opt}
                  variant="contained"
                  size="large"
                  disabled={answered}
                  onClick={() => handleAnswer(opt)}
                  sx={{
                    fontSize: '1.2rem',
                    borderRadius: '14px',
                    py: 1.5,
                    backgroundColor: answered ? bgColor : styles.primary,
                    color: '#fff',
                    fontWeight: 'bold',
                    transition: 'background-color 0.3s',
                    '&:disabled': { backgroundColor: bgColor, color: '#fff', opacity: 1 },
                  }}
                >
                  {opt}
                </Button>
              );
            })}
          </Box>

          {/* AI hint section */}
          {!answered && (
            <Box sx={{ mt: 3, textAlign: 'center' }}>
              <Tooltip title="Попросити підказку від AI">
                <Button
                  variant="outlined"
                  size="small"
                  startIcon={aiLoading ? <CircularProgress size={14} /> : <AutoFixHighIcon />}
                  disabled={aiLoading}
                  onClick={handleAskHint}
                  sx={{
                    borderColor: styles.secondary,
                    color: styles.secondary,
                    borderRadius: '20px',
                    fontSize: '0.85rem',
                    '&:hover': { backgroundColor: `${styles.secondary}20` },
                  }}
                >
                  {aiLoading ? 'Думаю...' : '✨ AI підказка'}
                </Button>
              </Tooltip>

              <Collapse in={showHint && !!aiHint}>
                <Box
                  sx={{
                    mt: 2,
                    p: 2,
                    borderRadius: '14px',
                    backgroundColor: `${styles.secondary}15`,
                    border: `1px solid ${styles.secondary}40`,
                    position: 'relative',
                    textAlign: 'left',
                  }}
                >
                  <IconButton
                    size="small"
                    onClick={() => setShowHint(false)}
                    sx={{ position: 'absolute', top: 4, right: 4, color: styles.textSecondary }}
                  >
                    <CloseIcon fontSize="small" />
                  </IconButton>
                  <Typography variant="body2" sx={{ color: styles.text, pr: 3 }}>
                    {aiHint}
                  </Typography>
                </Box>
              </Collapse>
            </Box>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default LessonPlayer;
