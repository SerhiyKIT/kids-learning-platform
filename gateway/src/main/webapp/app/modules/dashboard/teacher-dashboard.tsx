import React from 'react';
import { Container, Row, Col } from 'reactstrap';
import { Box, Typography, Button, Card, CardContent, Table, TableBody, TableCell, TableHead, TableRow } from '@mui/material';
import useLocalStorage from 'app/shared/hooks/useLocalStorage';
import { getThemeStyles } from '../home/home.styles';

import AddIcon from '@mui/icons-material/Add';
import GroupIcon from '@mui/icons-material/Group';
import EditIcon from '@mui/icons-material/Edit';

export const TeacherDashboard = () => {
  const [isDark] = useLocalStorage('app-theme-dark', true);
  const styles = getThemeStyles(isDark);

  const students = [
    { id: 1, name: 'Іван Петров', progress: '85%', lastActive: '2 хв тому' },
    { id: 2, name: 'Марія Сидоренко', progress: '92%', lastActive: '1 год тому' },
    { id: 3, name: 'Олексій Коваль', progress: '78%', lastActive: '5 год тому' },
  ];

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: styles.bg, color: styles.text, py: 4 }}>
      <Container>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
          <Typography variant="h3" sx={{ fontWeight: 'bold', color: styles.text }}>
            Панель Управління
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            sx={{
              backgroundColor: '#00c853',
              color: '#fff',
              borderRadius: '10px',
              px: 3,
              py: 1,
              '&:hover': { backgroundColor: '#009624' },
            }}
          >
            Створити Урок
          </Button>
        </Box>

        <Row className="mb-4">
          <Col md="12">
            <Card sx={{ backgroundColor: styles.cardBg, border: styles.cardBorder, borderRadius: '20px' }}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
                  <GroupIcon sx={{ color: styles.primary }} />
                  <Typography variant="h5" sx={{ color: styles.text }}>
                    Успішність Класу: 5-А
                  </Typography>
                </Box>

                <Table sx={{ '& td, & th': { color: styles.text, borderColor: 'rgba(255,255,255,0.1)' } }}>
                  <TableHead>
                    <TableRow>
                      <TableCell>Ім&apos;я Учня</TableCell>
                      <TableCell>Загальний Прогрес</TableCell>
                      <TableCell>Остання Активність</TableCell>
                      <TableCell align="right">Дії</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {students.map(student => (
                      <TableRow key={student.id}>
                        <TableCell>{student.name}</TableCell>
                        <TableCell>
                          <span style={{ color: styles.primary, fontWeight: 'bold' }}>{student.progress}</span>
                        </TableCell>
                        <TableCell sx={{ color: styles.textSecondary }}>{student.lastActive}</TableCell>
                        <TableCell align="right">
                          <Button size="small" startIcon={<EditIcon />} sx={{ color: styles.secondary }}>
                            Деталі
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </Col>
        </Row>
      </Container>
    </Box>
  );
};

export default TeacherDashboard;
