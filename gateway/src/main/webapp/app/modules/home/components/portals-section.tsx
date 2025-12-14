import React from 'react';
import { Box, Button, Card, CardContent, Container, Typography } from '@mui/material';
import { Row, Col } from 'reactstrap';
import { Link } from 'react-router-dom';
import SportsEsportsIcon from '@mui/icons-material/SportsEsports';
import InsightsIcon from '@mui/icons-material/Insights';
import SchoolIcon from '@mui/icons-material/School';
import { ThemeStyles } from '../home.styles';

const PortalCard = ({ title, role, desc, icon, color, styles, link }) => (
  <Col xs="12" md="4" className="mb-4">
    <Card
      sx={{
        backgroundColor: styles.cardBg,
        border: `2px solid ${color}`,
        borderRadius: '20px',
        height: '100%',
        position: 'relative',
        overflow: 'visible',
        transition: '0.3s',
        '&:hover': { boxShadow: `0 0 30px ${color}80` },
      }}
    >
      <Box
        sx={{
          position: 'absolute',
          top: -30,
          left: '50%',
          transform: 'translateX(-50%)',
          backgroundColor: styles.bg,
          borderRadius: '50%',
          p: 1,
          border: `2px solid ${color}`,
        }}
      >
        <Box sx={{ color }}>{icon}</Box>
      </Box>

      <CardContent sx={{ pt: 6, textAlign: 'center' }}>
        <Typography variant="overline" sx={{ color, letterSpacing: 2, fontWeight: 'bold' }}>
          {role}
        </Typography>
        <Typography variant="h4" sx={{ my: 1, fontWeight: 'bold', color: styles.text }}>
          {title}
        </Typography>
        <Typography variant="body2" sx={{ mb: 3, opacity: 0.8, color: styles.textSecondary }}>
          {desc}
        </Typography>
        <Button
          component={Link}
          to={link}
          variant="outlined"
          sx={{
            borderColor: color,
            color,
            borderRadius: '20px',
            '&:hover': { backgroundColor: color, color: '#fff' },
          }}
        >
          Увійти
        </Button>
      </CardContent>
    </Card>
  </Col>
);

export const PortalsSection = ({ styles }: { styles: ThemeStyles }) => {
  return (
    <Box sx={{ py: 8, background: styles.isDark ? 'rgba(255,255,255,0.03)' : '#e3f2fd' }}>
      <Container maxWidth="lg">
        <Typography variant="h3" align="center" sx={{ mb: 6, fontWeight: 'bold', color: styles.text }}>
          Обери Свій Портал
        </Typography>
        <Row className="justify-content-center">
          <PortalCard
            title="Кабінет Учня"
            role="THE PLAYER"
            desc="Твій ігровий хаб. Прогрес та місії."
            icon={<SportsEsportsIcon sx={{ fontSize: 60 }} />}
            color={styles.primary}
            styles={styles}
            link="/student-dashboard" // ОНОВЛЕНО
          />
          <PortalCard
            title="Кабінет Батьків"
            role="THE NAVIGATOR"
            desc="Моніторинг та звіти."
            icon={<InsightsIcon sx={{ fontSize: 60 }} />}
            color={styles.secondary}
            styles={styles}
            link="/parent-dashboard" // ОНОВЛЕНО
          />
          <PortalCard
            title="Кабінет Вчителя"
            role="THE ARCHITECT"
            desc="Аналітика класу та редактор."
            icon={<SchoolIcon sx={{ fontSize: 60 }} />}
            color="#00c853"
            styles={styles}
            link="/teacher-dashboard" // ОНОВЛЕНО
          />
        </Row>
      </Container>
    </Box>
  );
};
