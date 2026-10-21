/**
 * Server-enforced child-mode play session (docs/UX_гайд_дитячого_режиму.md §6): the parent's
 * session limit, the "rest" that follows it, and the parent's early wake-up. The server is the
 * source of truth — reloading the page never gets around the limit, and the limit is only checked
 * when a lesson completes, never mid-lesson.
 */
package ua.kidlearn.kidplay;
