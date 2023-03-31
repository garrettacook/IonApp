package me.garrett.ionapp.api;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Schedule {

    public static class Block {

        private final @NonNull
        String name;
        private final @NonNull
        LocalTime start, end;

        protected Block(@NonNull String name, @NonNull LocalTime start, @NonNull LocalTime end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }

        public static @NonNull
        Block fromJson(@NonNull JSONObject json) throws JSONException {
            String name = json.getString("name");
            LocalTime start = LocalTime.parse(json.getString("start"), IonApi.TIME_FORMAT);
            LocalTime end = LocalTime.parse(json.getString("end"), IonApi.TIME_FORMAT);
            return new Block(name, start, end);
        }

        @NonNull
        public String getName() {
            return name;
        }

        @NonNull
        public LocalTime getStart() {
            return start;
        }

        @NonNull
        public LocalTime getEnd() {
            return end;
        }

    }

    private final @NonNull
    LocalDate date;
    private final @NonNull
    String name;
    private final boolean special;
    private final @NonNull
    List<Block> blocks;

    protected Schedule(@NonNull LocalDate date, @NonNull String name, boolean special, @NonNull List<Block> blocks) {
        this.date = date;
        this.name = name;
        this.special = special;
        this.blocks = blocks;
    }

    public static @NonNull
    Schedule fromRawJson(@NonNull String json) throws JSONException {
        return fromJson(new JSONObject(json));
    }

    public static @NonNull
    Schedule fromJson(@NonNull JSONObject json) throws JSONException {
        LocalDate date = LocalDate.parse(json.getString("date"));
        JSONObject dayType = json.getJSONObject("day_type");

        String name = dayType.getString("name");
        boolean special = dayType.getBoolean("special");

        JSONArray blockArray = dayType.getJSONArray("blocks");
        List<Block> blocks = new ArrayList<>(blockArray.length());
        for (int i = 0; i < blockArray.length(); i++)
            blocks.add(Block.fromJson(blockArray.getJSONObject(i)));

        return new Schedule(date, name, special, blocks);
    }

    @NonNull
    public LocalDate getDate() {
        return date;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public boolean isSpecial() {
        return special;
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    @NonNull
    public List<Block> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    @NonNull
    public Optional<Block> getLunchBlock() {
        return blocks.stream().filter(b -> b.getName().equalsIgnoreCase("lunch"))
                .findFirst();
    }

    @NonNull
    public Map<Character, Block> getEighthBlocks() {
        return blocks.stream().filter(b -> b.getName().charAt(0) == '8')
                .collect(Collectors.toMap(b -> b.getName().charAt(1), b -> b));
    }

    @NonNull
    public Map<Character, Instant> getEighthTransitionTimes() {
        Map<Character, Instant> map = new HashMap<>();

        LocalTime lastEndTime = null;
        for (Block block : blocks) {
            if (block.getName().equals("Break")) // skip "breaks" since they mean nothing
                continue;

            if (block.getName().charAt(0) == '8') {
                if (lastEndTime == null)
                    lastEndTime = block.getStart().minusMinutes(10);
                map.put(block.getName().charAt(1), ZonedDateTime.of(date, lastEndTime, IonApi.ION_TIME_ZONE).toInstant());
            }
            lastEndTime = block.getEnd();
        }

        return map;
    }

    public @NonNull
    Instant getEnd() {
        return ZonedDateTime.of(date, blocks.get(blocks.size() - 1).end, IonApi.ION_TIME_ZONE).toInstant();
    }

}
