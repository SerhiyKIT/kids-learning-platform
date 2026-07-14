import React from 'react';
import { Box, Button, Container, Typography } from '@mui/material';
import { Row, Col } from 'reactstrap';
import { translate } from 'react-jhipster';
import SportsEsportsIcon from '@mui/icons-material/SportsEsports';
import { ThemeStyles } from '../home.styles';

interface HeroProps {
  styles: ThemeStyles;
  isAuthenticated: boolean;
  onNavigate: (path: string) => void;
}

export const HeroSection = ({ styles, isAuthenticated, onNavigate }: HeroProps) => {
  return (
    <Container maxWidth="lg" sx={{ pt: 8, pb: 8 }}>
      <Row className="align-items-center">
        <Col xs="12" md="6">
          <Typography
            variant="h2"
            sx={{
              fontWeight: 800,
              mb: 2,
              background: `linear-gradient(45deg, ${styles.primary}, ${styles.secondary})`,
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              textShadow: styles.isDark ? '0 0 30px rgba(0,229,255,0.3)' : 'none',
            }}
          >
            {translate('home.hero.title')}
          </Typography>
          <Typography variant="h4" sx={{ mb: 3, fontWeight: 'medium' }}>
            {translate('home.hero.subtitle')}
          </Typography>
          <Typography variant="h6" sx={{ mb: 4, opacity: 0.8, fontWeight: 300 }}>
            {translate('home.hero.description')}
          </Typography>

          <Button
            variant="contained"
            size="large"
            onClick={() => onNavigate(isAuthenticated ? '/lesson' : '/account/register')}
            sx={{
              px: 5,
              py: 2,
              fontSize: '1.2rem',
              borderRadius: '50px',
              background: `linear-gradient(90deg, ${styles.primary}, ${styles.secondary})`,
              boxShadow: styles.glow,
              fontWeight: 'bold',
            }}
          >
            {isAuthenticated ? translate('home.hero.ctaContinue') : translate('home.hero.ctaStart')}
          </Button>
        </Col>

        <Col xs="12" md="6" className="d-flex justify-content-center mt-4 mt-md-0">
          <Box
            sx={{
              width: '300px',
              height: '300px',
              borderRadius: '50%',
              background: `radial-gradient(circle, ${styles.secondary} 0%, transparent 70%)`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              position: 'relative',
              animation: 'float 6s ease-in-out infinite',
            }}
          >
            <SportsEsportsIcon sx={{ fontSize: 200, color: styles.text, zIndex: 2 }} />
            <Box
              sx={{
                position: 'absolute',
                width: '100%',
                height: '100%',
                borderRadius: '50%',
                border: `2px dashed ${styles.primary}`,
                animation: 'spin 20s linear infinite',
              }}
            />
          </Box>
        </Col>
      </Row>
    </Container>
  );
};
