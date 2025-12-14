import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAppSelector } from 'app/config/store';

// Імпортуємо наш новий хук
import useLocalStorage from 'app/shared/hooks/useLocalStorage';

import { Row, Col } from 'reactstrap';
import { Box, Button, Container, IconButton, AppBar, Toolbar, Chip, Stack, Typography } from '@mui/material';

import RocketLaunchIcon from '@mui/icons-material/RocketLaunch';
import DarkModeIcon from '@mui/icons-material/DarkMode';
import LightModeIcon from '@mui/icons-material/LightMode';
import PersonIcon from '@mui/icons-material/Person';
import SecurityIcon from '@mui/icons-material/Security';

import { getThemeStyles } from './home.styles';
import { HeroSection } from './components/hero-section';
import { FeaturesSection } from './components/features-section';
import { PortalsSection } from './components/portals-section';

export const Home = () => {
  const account = useAppSelector(state => state.authentication.account);
  const navigate = useNavigate();

  // ВИКОРИСТОВУЄМО LOCAL STORAGE для теми
  // 'app-theme-dark' - це ключ, під яким збережеться значення
  const [isDark, setIsDark] = useLocalStorage('app-theme-dark', true);

  const styles = getThemeStyles(isDark);
  const toggleTheme = () => setIsDark(!isDark);

  return (
    <Box
      sx={{
        backgroundColor: styles.bg,
        minHeight: '100vh',
        color: styles.text,
        transition: 'all 0.3s ease',
        overflowX: 'hidden',
      }}
    >
      {/* --- HEADER --- */}
      <AppBar position="static" color="transparent" elevation={0} sx={{ pt: 2 }}>
        <Container maxWidth="lg">
          <Toolbar disableGutters sx={{ justifyContent: 'space-between' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <RocketLaunchIcon sx={{ color: styles.primary, fontSize: 32 }} />
              <Typography variant="h5" sx={{ fontWeight: 'bold', letterSpacing: 1, color: styles.text }}>
                НАСТУПНИЙ <span style={{ color: styles.primary }}>РІВЕНЬ</span>
              </Typography>
            </Box>

            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <IconButton onClick={toggleTheme} sx={{ color: styles.text }}>
                {isDark ? <LightModeIcon /> : <DarkModeIcon />}
              </IconButton>

              {account?.login ? (
                <Chip
                  icon={<PersonIcon sx={{ color: styles.text }} />}
                  label={account.login}
                  variant="outlined"
                  sx={{ borderColor: styles.primary, color: styles.text }}
                />
              ) : (
                <>
                  <Button component={Link} to="/login" sx={{ color: styles.text }}>
                    Вхід
                  </Button>
                  <Button
                    component={Link}
                    to="/account/register"
                    variant="contained"
                    sx={{
                      backgroundColor: styles.primary,
                      color: isDark ? '#000' : '#fff',
                      fontWeight: 'bold',
                    }}
                  >
                    Реєстрація
                  </Button>
                </>
              )}
            </Box>
          </Toolbar>
        </Container>
      </AppBar>

      {/* --- ОСНОВНІ БЛОКИ --- */}
      <HeroSection styles={styles} isAuthenticated={!!account?.login} onNavigate={navigate} />
      <FeaturesSection styles={styles} />
      <PortalsSection styles={styles} />

      {/* --- FOOTER --- */}
      <Box sx={{ py: 4, borderTop: styles.cardBorder, mt: 4 }}>
        <Container maxWidth="lg">
          <Row className="align-items-center">
            <Col xs="12" md="6">
              <Stack direction="row" spacing={3}>
                <Link to="/about" style={{ color: styles.text, textDecoration: 'none' }}>
                  Про Нас
                </Link>
                <Link to="/pricing" style={{ color: styles.text, textDecoration: 'none' }}>
                  Ціни
                </Link>
                <Link to="/contact" style={{ color: styles.text, textDecoration: 'none' }}>
                  Зв&apos;язок
                </Link>
              </Stack>
            </Col>
            <Col xs="12" md="6" className="text-md-end text-start mt-3 mt-md-0">
              <Box
                sx={{ display: 'flex', alignItems: 'center', justifyContent: { xs: 'flex-start', md: 'flex-end' }, gap: 1, opacity: 0.7 }}
              >
                <SecurityIcon fontSize="small" sx={{ color: styles.text }} />
                <Typography variant="body2" sx={{ color: styles.text }}>
                  COPPA-compliant. Дані захищені.
                </Typography>
              </Box>
            </Col>
          </Row>
        </Container>
      </Box>

      <style>{`
        @keyframes float { 0% { transform: translateY(0px); } 50% { transform: translateY(-20px); } 100% { transform: translateY(0px); } }
        @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
      `}</style>
    </Box>
  );
};

export default Home;
