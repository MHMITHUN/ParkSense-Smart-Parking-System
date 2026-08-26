package com.parksense.store;

import com.parksense.auth.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory home of staff accounts. The store is deliberately dumb — a
 * thread-safe map with lookup helpers — so all rules (hashing, roles)
 * stay in the auth package.
 */
public final class UserStore {

    private final Map<String, User> byUsername = new ConcurrentHashMap<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    /** Persistence hook: called after every mutation. */
    public void onChange(Runnable listener) {
        changeListeners.add(listener);
    }

    private void fireChange() {
        changeListeners.forEach(Runnable::run);
    }

    public void save(User user) {
        byUsername.put(user.username(), user);
        fireChange();
    }

    public Optional<User> find(String username) {
        return Optional.ofNullable(byUsername.get(username == null ? "" : username.trim().toLowerCase()));
    }

    public List<User> all() {
        return new ArrayList<>(byUsername.values());
    }
}
