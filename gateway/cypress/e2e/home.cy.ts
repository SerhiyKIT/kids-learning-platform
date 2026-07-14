/**
 * E2E: Home page — loads correctly, shows portals, locale switcher, theme toggle
 */
describe('Home page', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  it('loads the home page and shows the app name', () => {
    cy.contains('НАСТУПНИЙ РІВЕНЬ').should('be.visible');
  });

  it('shows three portal cards', () => {
    cy.get('[class*="MuiCard"]').should('have.length.gte', 3);
  });

  it('theme toggle switches appearance', () => {
    // The page starts in dark mode (localStorage default)
    cy.get('body').then($body => {
      const bgBefore = $body.css('background-color');
      cy.get('button[aria-label]').contains('svg').parent().click({ force: true });
      // After toggle, background should change
      cy.get('body').should($b => {
        expect($b.css('background-color')).not.to.eq(bgBefore);
      });
    });
  });

  it('COPPA consent banner appears on first visit', () => {
    // Clear storage so banner shows
    cy.clearLocalStorage('coppa-consent-given');
    cy.reload();
    cy.contains('Захист персональних даних дітей').should('be.visible');
  });

  it('COPPA consent is dismissed after agreeing', () => {
    cy.clearLocalStorage('coppa-consent-given');
    cy.reload();
    cy.contains('Захист персональних даних дітей').should('be.visible');
    // Check the checkbox and click agree
    cy.get('input[type="checkbox"]').check({ force: true });
    cy.contains('button', 'Погоджуюсь').click();
    cy.contains('Захист персональних даних дітей').should('not.exist');
  });

  it('locale switcher is present', () => {
    // The locale Select should render
    cy.get('[class*="MuiSelect"]').should('exist');
  });

  it('portal Enter buttons link to dashboards', () => {
    cy.get('a[href="/student-dashboard"]').should('exist');
    cy.get('a[href="/parent-dashboard"]').should('exist');
    cy.get('a[href="/teacher-dashboard"]').should('exist');
  });
});
