import React from 'react';
import { Box, Container, Paper, Typography } from '@mui/material';
import { Row, Col } from 'reactstrap';
import SportsEsportsIcon from '@mui/icons-material/SportsEsports';
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents';
import PaletteIcon from '@mui/icons-material/Palette';
import { ThemeStyles } from '../home.styles';

const FeatureCard = ({ icon, title, desc, styles }: { icon: any; title: string; desc: string; styles: ThemeStyles }) => (
  <Col xs="12" md="4" className="mb-4">
    <Paper
      elevation={0}
      sx={{
        p: 4,
        height: '100%',
        backgroundColor: styles.cardBg,
        border: styles.cardBorder,
        borderRadius: '20px',
        textAlign: 'center',
        transition: '0.3s',
        '&:hover': { transform: 'translateY(-10px)', boxShadow: styles.glow },
      }}
    >
      <Box sx={{ mb: 2 }}>{icon}</Box>
      {/* Явно задаємо колір тексту */}
      <Typography variant="h5" sx={{ mb: 2, fontWeight: 'bold', color: styles.text }}>
        {title}
      </Typography>
      <Typography variant="body1" sx={{ opacity: 0.8, color: styles.textSecondary }}>
        {desc}
      </Typography>
    </Paper>
  </Col>
);

export const FeaturesSection = ({ styles }: { styles: ThemeStyles }) => {
  return (
    <Container maxWidth="lg" sx={{ py: 8 }}>
      <Row>
        <FeatureCard
          icon={<SportsEsportsIcon fontSize="large" sx={{ color: styles.primary }} />}
          title="Уроки-Міні-Ігри"
          desc="Квести та головоломки з Математики та Логіки."
          styles={styles}
        />
        <FeatureCard
          icon={<EmojiEventsIcon fontSize="large" sx={{ color: styles.secondary }} />}
          title="Система Ачівок"
          desc="Отримуй нагороди та підвищуй Рівень за проходження ігор."
          styles={styles}
        />
        <FeatureCard
          icon={<PaletteIcon fontSize="large" sx={{ color: styles.primary }} />}
          title="Персональні Стилі"
          desc="Обирай вигляд інтерфейсу: від Кіберпанку до Галактики."
          styles={styles}
        />
      </Row>
    </Container>
  );
};
