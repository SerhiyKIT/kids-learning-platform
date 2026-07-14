import React, { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { translate } from 'react-jhipster';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { setLocale } from 'app/shared/reducers/locale';
import { languages, locales } from 'app/config/translation';

import useLocalStorage from 'app/shared/hooks/useLocalStorage';

import { Row, Col } from 'reactstrap';
import { Box, Button, Container, IconButton, AppBar, Toolbar, Chip, Stack, Typography, Select, MenuItem } from '@mui/material';

import RocketLaunchIcon from '@mui/icons-material/RocketLaunch';
import DarkModeIcon from '@mui/icons-material/DarkMode';
import LightModeIcon from '@mui/icons-material/LightMode';
import PersonIcon from '@mui/icons-material/Person';
import SecurityIcon from '@mui/icons-material/Security';
import TranslateIcon from '@mui/icons-material/Translate';

import { getThemeStyles } from './home.styles';
import { HeroSection } from './components/hero-section';
import { FeaturesSection } from './components/features-section';
import { PortalsSection } from './components/portals-section';

export const Home = () => {
  const account = useAppSelector(state => state.authentication.account);
  const currentLocale = useAppSelector(state => state.locale.currentLocale);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  const [isDark, setIsDark] = useLocalStorage('app-theme-dark', true);
  const [userRole] = useLocalStorage('app-user-role', '');

  const styles = getThemeStyles(isDark);
  const toggleTheme = () => setIsDark(!isDark);

  const handleLocaleChange = (event: any) => {
    dispatch(setLocale(event.target.value));
  };

  // Auto-redirect logged-in users to their last dashboard
  useEffect(() => {
    if (account?.login && userRole) {
      navigate(`/${userRole}-dashboard`);
    }
  }, [account?.login, userRole]);

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
                {translate('platform.appName')}
              </Typography>
            </Box>

            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              {/* Locale switcher */}
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                <TranslateIcon sx={{ color: styles.textSecondary, fontSize: 18 }} />
                <Select
                  value={currentLocale || 'ua'}
                  onChange={handleLocaleChange}
                  variant="standard"
                  disableUnderline
                  sx={{
                    color: styles.text,
                    fontSize: '0.9rem',
                    '& .MuiSelect-icon': { color: styles.text },
                    '& .MuiSelect-select': { py: 0 },
                  }}
                >
                  {locales.map(locale => (
                    <MenuItem key={locale} value={locale}>
                      {languages[locale].name}
                    </MenuItem>
                  ))}
                </Select>
              </Box>

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
                    {translate('platform.nav.login')}
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
                    {translate('platform.nav.register')}
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
                  {translate('platform.nav.about')}
                </Link>
                <Link to="/pricing" style={{ color: styles.text, textDecoration: 'none' }}>
                  {translate('platform.nav.pricing')}
                </Link>
                <Link to="/contact" style={{ color: styles.text, textDecoration: 'none' }}>
                  {translate('platform.nav.contact')}
                </Link>
              </Stack>
            </Col>
            <Col xs="12" md="6" className="text-md-end text-start mt-3 mt-md-0">
              <Box
                sx={{ display: 'flex', alignItems: 'center', justifyContent: { xs: 'flex-start', md: 'flex-end' }, gap: 1, opacity: 0.7 }}
              >
                <SecurityIcon fontSize="small" sx={{ color: styles.text }} />
                <Typography variant="body2" sx={{ color: styles.text }}>
                  {translate('platform.footer.coppa')}
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
