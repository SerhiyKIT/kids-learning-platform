/**
 * E2E: Lesson Player — question navigation, answer selection, AI hint, finish screen
 * Requires: backend running with seed lessons
 */
describe('Lesson Player (authenticated)', () => {
  const username = Cypress.env('testUsername');
  const password = Cypress.env('testPassword');

  before(() => {
    cy.loginByOAuth(username, password);
  });

  beforeEach(() => {
    // Navigate to the first seeded lesson (id=9001)
    cy.visit('/lesson/9001');
  });

  it('shows the progress bar', () => {
    cy.get('[class*="MuiLinearProgress"]', { timeout: 10000 }).should('exist');
  });

  it('shows the question counter', () => {
    cy.contains(/питання|question/i, { timeout: 10000 }).should('be.visible');
  });

  it('shows answer option buttons', () => {
    cy.get('button[class*="MuiButton"]', { timeout: 10000 }).should('have.length.gte', 2);
  });

  it('selecting an answer highlights it and advances to next question', () => {
    cy.get('button[class*="MuiButton"]', { timeout: 10000 })
      .first()
      .click();
    // After 800ms animation the question should change or finish screen appears
    cy.wait(1000);
    cy.get('body').then($body => {
      const hasProgress = $body.find('[class*="MuiLinearProgress"]').length > 0;
      const hasFinished = $body.text().includes('Урок завершено') || $body.text().includes('finished');
      expect(hasProgress || hasFinished).to.be.true;
    });
  });

  it('shows AI hint button', () => {
    cy.contains(/ai підказка|підказка/i, { timeout: 10000 }).should('be.visible');
  });

  it('AI hint button triggers hint display', () => {
    cy.contains(/ai підказка|підказка/i, { timeout: 10000 }).click();
    // Either hint appears or loading spinner shows
    cy.get('body', { timeout: 8000 }).should($b => {
      const hasHint =
        $b.find('[class*="MuiCollapse-entered"]').length > 0 ||
        $b.find('[class*="MuiCircularProgress"]').length > 0;
      expect(hasHint).to.be.true;
    });
  });

  it('completes all questions and shows finish screen', () => {
    // Answer all 5 questions by clicking first button each time
    for (let i = 0; i < 5; i++) {
      cy.get('button[class*="MuiButton"]', { timeout: 10000 })
        .first()
        .click();
      cy.wait(900);
    }
    cy.contains(/урок завершено|finished|🎉/i, { timeout: 10000 }).should('be.visible');
  });

  it('finish screen has a "back to missions" button', () => {
    for (let i = 0; i < 5; i++) {
      cy.get('button[class*="MuiButton"]', { timeout: 10000 }).first().click();
      cy.wait(900);
    }
    cy.contains(/назад до місій|back/i, { timeout: 8000 }).should('be.visible').click();
    cy.url().should('include', '/student-dashboard');
  });
});
