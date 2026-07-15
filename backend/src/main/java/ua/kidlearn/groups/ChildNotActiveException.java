package ua.kidlearn.groups;

/** Thrown when a parent tries to join a child that isn't active (e.g. still pending_consent). */
public class ChildNotActiveException extends RuntimeException {
}
