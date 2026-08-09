/**
 * Development-only convenience endpoint that seeds a demo admin/teacher, a published safety
 * lesson, and a group — plus, for the calling parent, a consented child with that lesson
 * assigned — so the scene engine has something to play without manual admin/teacher setup
 * first. Every bean here is active only under the "dev" Spring profile; this package must never
 * ship to production.
 */
package ua.kidlearn.devseed;
