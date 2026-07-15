package ua.kidlearn.groups;

/** Thrown when a join code identifies a real but archived (isActive=false) group. */
public class GroupInactiveException extends RuntimeException {
}
