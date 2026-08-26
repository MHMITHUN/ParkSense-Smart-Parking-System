package com.parksense.store;

import com.parksense.members.Member;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** In-memory home of member records. */
public final class MemberStore {

    private final Map<String, Member> byId = new ConcurrentHashMap<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    /** Persistence hook: called after every mutation. */
    public void onChange(Runnable listener) {
        changeListeners.add(listener);
    }

    private void fireChange() {
        changeListeners.forEach(Runnable::run);
    }

    public void save(Member member) {
        byId.put(member.id(), member);
        fireChange();
    }

    public Optional<Member> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<Member> all() {
        List<Member> members = new ArrayList<>(byId.values());
        members.sort((a, b) -> a.id().compareTo(b.id()));
        return members;
    }

    public Optional<Member> findByPlate(String plateNo) {
        String plate = plateNo == null ? "" : plateNo.trim().toUpperCase();
        return byId.values().stream().filter(m -> m.coversPlate(plate)).findFirst();
    }
}
