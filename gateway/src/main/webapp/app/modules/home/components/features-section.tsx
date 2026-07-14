import React from 'react';
import { Box, Container, Paper, Typography } from '@mui/material';
import { Row, Col } from 'reactstrap';
import { translate } from 'react-jhipster';
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
          title={translate('home.features.miniGames.title')}
          desc={translate('home.features.miniGames.desc')}
          styles={styles}
        />
        <FeatureCard
          icon={<EmojiEventsIcon fontSize="large" sx={{ color: styles.secondary }} />}
          title={translate('home.features.achievements.title')}
          desc={translate('home.features.achievements.desc')}
          styles={styles}
        />
        <FeatureCard
          icon={<PaletteIcon fontSize="large" sx={{ color: styles.primary }} />}
          title={translate('home.features.styles.title')}
          desc={translate('home.features.styles.desc')}
          styles={styles}
        />
      </Row>
    </Container>
  );
};
