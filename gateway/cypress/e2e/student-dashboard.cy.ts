/**
 * E2E: Student Dashboard — authenticated user sees lessons, can navigate to lesson
 * Requires: backend running + Keycloak at localhost:9080
 */
describe('Student Dashboard (authenticated)', () => {
  const username = Cypress.env('testUsername');
  const password = Cypress.env('testPassword');

  before(() => {
    cy.loginByOAuth(username, password);
  });

  beforeEach(() => {
    // Set role so home page auto-redirects here
    cy.window().then(win => win.localStorage.setItem('app-user-role', 'student'));
    cy.visit('/student-dashboard');
  });

  it('renders the student dashboard heading', () => {
    cy.contains(/вітаємо|привіт|welcome/i).should('be.visible');
  });

  it('shows the XP level progress bar', () => {
    cy.get('[class*="MuiLinearProgress"]').should('exist');
  });

  it('shows the missions section title', () => {
    cy.contains(/місії|завдання|missions/i).should('be.visible');
  });

  it('shows a loading spinner while fetching lessons, then renders content', () => {
    // After mount: spinner OR lesson cards should appear
    cy.get('body').then($body => {
      if ($body.find('[class*="MuiCircularProgress"]').length) {
        cy.get('[class*="MuiCircularProgress"]').should('exist');
      }
    });
    // Eventually cards or empty state message appears
    cy.get('[class*="MuiCard"]', { timeout: 10000 }).should('have.length.gte', 1);
  });

  it('lesson card has a button linking to /lesson/:id', () => {
    cy.get('[class*="MuiCard"]', { timeout: 10000 })
      .first()
      .find('a[href*="/lesson/"]')
      .should('exist');
  });

  it('navigates to lesson player on card button click', () => {
    cy.get('[class*="MuiCard"]', { timeout: 10000 })
      .first()
      .find('a[href*="/lesson/"]')
      .click();
    cy.url().should('match', /\/lesson\/\d+/);
    cy.get('[class*="MuiLinearProgress"]', { timeout: 10000 }).should('exist');
  });
});
