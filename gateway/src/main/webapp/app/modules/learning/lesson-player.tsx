import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { Card, CardContent, Button, Typography, Box, CircularProgress, Alert } from '@mui/material';
import { useAppSelector } from 'app/config/store';

// Типи для нашого JSON-тесту
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
  const [lessonData, setLessonData] = useState<LessonContent | null>(null);
  const [currentQuestion, setCurrentQuestion] = useState(0);
  const [score, setScore] = useState(0);
  const [loading, setLoading] = useState(true);
  const [finished, setFinished] = useState(false);

  // Отримуємо ID студента (для спрощення беремо 1 або ID поточного юзера, якщо реалізовано)
  const account = useAppSelector(state => state.authentication.account);

  useEffect(() => {
    // 1. Завантажуємо урок з бекенду (Learning Service)
    const fetchLesson = async () => {
      try {
        const result = await axios.get(`/services/learningservice/api/lessons/${id}`);
        // Парсимо JSON з поля content
        const parsedContent = JSON.parse(result.data.content);
        setLessonData(parsedContent);
      } catch (error) {
        console.error('Error loading lesson', error);
      } finally {
        setLoading(false);
      }
    };
    fetchLesson();
  }, [id]);

  const handleAnswer = (answer: string) => {
    if (!lessonData) return;

    // Перевірка відповіді
    if (answer === lessonData.questions[currentQuestion].correctAnswer) {
      setScore(score + 1);
    }

    // Перехід далі або кінець
    if (currentQuestion + 1 < lessonData.questions.length) {
      setCurrentQuestion(currentQuestion + 1);
    } else {
      finishLesson(score + (answer === lessonData.questions[currentQuestion].correctAnswer ? 1 : 0));
    }
  };

  const finishLesson = async (finalScore: number) => {
    setFinished(true);
    // 2. Зберігаємо результат на бекенд
    try {
      await axios.post('/services/learningservice/api/progresses', {
        studentId: 1001, // Тимчасовий ID, пізніше зв'яжемо з реальним Student
        score: finalScore,
        status: 'COMPLETED',
        completedAt: new Date().toISOString(),
        lesson: { id: Number(id) }, // Зв'язок з уроком
      });
    } catch (e) {
      console.error('Не вдалося зберегти прогрес', e);
    }
  };

  if (loading) return <CircularProgress />;
  if (!lessonData) return <Alert severity="error">Урок порожній або формат неправильний</Alert>;

  if (finished) {
    return (
      <Box sx={{ textAlign: 'center', mt: 5, p: 3, backgroundColor: '#e3f2fd', borderRadius: 4 }}>
        <Typography variant="h2">🎉 Урок завершено!</Typography>
        <Typography variant="h4" sx={{ mt: 2 }}>
          Твій результат: {score} з {lessonData.questions.length}
        </Typography>
        <Button variant="contained" color="success" size="large" sx={{ mt: 4 }} onClick={() => navigate('/')}>
          Повернутися до карти
        </Button>
      </Box>
    );
  }

  const question = lessonData.questions[currentQuestion];

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', mt: 5 }}>
      <Card sx={{ maxWidth: 600, width: '100%', borderRadius: 4, boxShadow: 5, backgroundColor: '#fff8e1' }}>
        <CardContent sx={{ textAlign: 'center', p: 4 }}>
          <Typography variant="h6" color="textSecondary">
            Питання {currentQuestion + 1} з {lessonData.questions.length}
          </Typography>

          <Typography variant="h4" sx={{ my: 4, fontWeight: 'bold', color: '#333' }}>
            {question.text}
          </Typography>

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {question.options.map(opt => (
              <Button
                key={opt}
                variant="contained"
                size="large"
                sx={{ fontSize: '1.5rem', borderRadius: 3, backgroundColor: '#ff7043' }}
                onClick={() => handleAnswer(opt)}
              >
                {opt}
              </Button>
            ))}
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
};

export default LessonPlayer;
