// Global Cypress support file — runs before every spec

// Suppress uncaught exceptions from the app (e.g. React dev warnings)
Cypress.on('uncaught:exception', (err) => {
  // Ignore known non-critical errors
  if (err.message.includes('ResizeObserver') || err.message.includes('ChunkLoadError')) {
    return false;
  }
  return true;
});

// Custom command: login via Keycloak OAuth2 (session-based, bypasses UI)
Cypress.Commands.add('loginByOAuth', (username: string, password: string) => {
  cy.session(
    [username, password],
    () => {
      cy.visit('/');
      cy.get('body').then($body => {
        // If already authenticated, skip login
        if ($body.find('[data-cy="navbar-login"]').length === 0) return;
        cy.get('[data-cy="navbar-login"]').click();
        cy.origin('http://localhost:9080', { args: { username, password } }, ({ username: u, password: p }) => {
          cy.get('#username').type(u);
          cy.get('#password').type(p);
          cy.get('#kc-login').click();
        });
      });
    },
    { cacheAcrossSpecs: true },
  );
});

declare global {
  namespace Cypress {
    interface Chainable {
      loginByOAuth(username: string, password: string): Chainable<void>;
    }
  }
}
