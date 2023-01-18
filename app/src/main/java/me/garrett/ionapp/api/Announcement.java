package me.garrett.ionapp.api;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Announcement {

    private final int id;
    private final @NonNull String title, content, author;
    private final int user;
    private final @NonNull Instant added, updated;
    private final @NonNull Set<Integer> groups;
    private final boolean pinned;

    public Announcement(int id, @NonNull String title, @NonNull String content, @NonNull String author, int user, @NonNull Instant added, @NonNull Instant updated, @NonNull Set<Integer> groups, boolean pinned) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.user = user;
        this.added = added;
        this.updated = updated;
        this.groups = groups;
        this.pinned = pinned;
    }

    public static @NonNull
    Announcement fromJson(@NonNull JSONObject json) throws JSONException {
        int id = json.getInt("id");
        String title = json.getString("title");
        String content = json.getString("content");
        String author = json.getString("author");
        int user = json.getInt("user");

        Instant added = Instant.parse(json.getString("added"));
        Instant updated = Instant.parse(json.getString("updated"));

        JSONArray groupArray = json.getJSONArray("groups");
        Set<Integer> groups = new HashSet<>();
        for (int i = 0; i < groupArray.length(); i++)
            groups.add(groupArray.getInt(i));

        boolean pinned = json.getBoolean("pinned");

        return new Announcement(id, title, content, author, user, added, updated, groups, pinned);
    }

    public static @NonNull
    List<Announcement> listFromRawJson(@NonNull String json) throws JSONException {
        return listFromJson(new JSONObject(json).getJSONArray("results"));
    }

    public static @NonNull
    List<Announcement> listFromJson(@NonNull JSONArray jsonArray) throws JSONException {
        List<Announcement> list = new ArrayList<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++)
            list.add(fromJson(jsonArray.getJSONObject(i)));
        return list;
    }

    public int getId() {
        return id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getContent() {
        return content;
    }

    @NonNull
    public String getAuthor() {
        return author;
    }

    public int getUser() {
        return user;
    }

    @NonNull
    public Instant getAdded() {
        return added;
    }

    @NonNull
    public Instant getUpdated() {
        return updated;
    }

    @NonNull
    public Set<Integer> getGroups() {
        return Collections.unmodifiableSet(groups);
    }

    public boolean isPinned() {
        return pinned;
    }

}
