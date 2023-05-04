package me.garrett.ionapp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IonUtilsTest {

    @Test
    public void testBusSpaceToCoordinates() {
        assertArrayEquals(new int[]{0, -7}, IonUtils.getBusCoordinates("_9"));
        assertArrayEquals(new int[]{0, -6}, IonUtils.getBusCoordinates("_8"));
        assertArrayEquals(new int[]{0, -5}, IonUtils.getBusCoordinates("_7"));
        assertArrayEquals(new int[]{0, -4}, IonUtils.getBusCoordinates("_6"));
        assertArrayEquals(new int[]{0, -3}, IonUtils.getBusCoordinates("_5"));
        assertArrayEquals(new int[]{0, -2}, IonUtils.getBusCoordinates("_4"));
        assertArrayEquals(new int[]{0, -1}, IonUtils.getBusCoordinates("_3"));
        assertArrayEquals(new int[]{0, 0}, IonUtils.getBusCoordinates("_2"));
        assertArrayEquals(new int[]{0, 1}, IonUtils.getBusCoordinates("_1"));
        assertArrayEquals(new int[]{0, 2}, IonUtils.getBusCoordinates("_41"));
        assertArrayEquals(new int[]{1, -7}, IonUtils.getBusCoordinates("_31"));
        assertArrayEquals(new int[]{1, -6}, IonUtils.getBusCoordinates("_32"));
        assertArrayEquals(new int[]{1, -5}, IonUtils.getBusCoordinates("_33"));
        assertArrayEquals(new int[]{1, -4}, IonUtils.getBusCoordinates("_34"));
        assertArrayEquals(new int[]{1, -3}, IonUtils.getBusCoordinates("_35"));
        assertArrayEquals(new int[]{1, -2}, IonUtils.getBusCoordinates("_36"));
        assertArrayEquals(new int[]{1, -1}, IonUtils.getBusCoordinates("_37"));
        assertArrayEquals(new int[]{1, 0}, IonUtils.getBusCoordinates("_38"));
        assertArrayEquals(new int[]{1, 1}, IonUtils.getBusCoordinates("_39"));
        assertArrayEquals(new int[]{1, 2}, IonUtils.getBusCoordinates("_40"));
        assertArrayEquals(new int[]{2, -9}, IonUtils.getBusCoordinates("_22"));
        assertArrayEquals(new int[]{2, -8}, IonUtils.getBusCoordinates("_21"));
        assertArrayEquals(new int[]{2, -7}, IonUtils.getBusCoordinates("_20"));
        assertArrayEquals(new int[]{2, -6}, IonUtils.getBusCoordinates("_19"));
        assertArrayEquals(new int[]{2, -5}, IonUtils.getBusCoordinates("_18"));
        assertArrayEquals(new int[]{2, -4}, IonUtils.getBusCoordinates("_17"));
        assertArrayEquals(new int[]{2, -3}, IonUtils.getBusCoordinates("_16"));
        assertArrayEquals(new int[]{2, -2}, IonUtils.getBusCoordinates("_15"));
        assertArrayEquals(new int[]{2, -1}, IonUtils.getBusCoordinates("_14"));
        assertArrayEquals(new int[]{2, 0}, IonUtils.getBusCoordinates("_13"));
        assertArrayEquals(new int[]{2, 1}, IonUtils.getBusCoordinates("_12"));
        assertArrayEquals(new int[]{2, 2}, IonUtils.getBusCoordinates("_11"));
        assertArrayEquals(new int[]{2, 3}, IonUtils.getBusCoordinates("_10"));
        assertArrayEquals(new int[]{3, -8}, IonUtils.getBusCoordinates("_45"));
        assertArrayEquals(new int[]{3, -7}, IonUtils.getBusCoordinates("_44"));
        assertArrayEquals(new int[]{3, -6}, IonUtils.getBusCoordinates("_43"));
        assertArrayEquals(new int[]{3, -5}, IonUtils.getBusCoordinates("_42"));
        assertArrayEquals(new int[]{3, -4}, IonUtils.getBusCoordinates("_30"));
        assertArrayEquals(new int[]{3, -3}, IonUtils.getBusCoordinates("_29"));
        assertArrayEquals(new int[]{3, -2}, IonUtils.getBusCoordinates("_28"));
        assertArrayEquals(new int[]{3, -1}, IonUtils.getBusCoordinates("_27"));
        assertArrayEquals(new int[]{3, 0}, IonUtils.getBusCoordinates("_26"));
        assertArrayEquals(new int[]{3, 1}, IonUtils.getBusCoordinates("_25"));
        assertArrayEquals(new int[]{3, 2}, IonUtils.getBusCoordinates("_24"));
        assertArrayEquals(new int[]{3, 3}, IonUtils.getBusCoordinates("_23"));
    }

    @Test
    public void testBusCoordinatesToSpace() {
        assertEquals("_9", IonUtils.getBusSpace(0, -7));
        assertEquals("_8", IonUtils.getBusSpace(0, -6));
        assertEquals("_7", IonUtils.getBusSpace(0, -5));
        assertEquals("_6", IonUtils.getBusSpace(0, -4));
        assertEquals("_5", IonUtils.getBusSpace(0, -3));
        assertEquals("_4", IonUtils.getBusSpace(0, -2));
        assertEquals("_3", IonUtils.getBusSpace(0, -1));
        assertEquals("_2", IonUtils.getBusSpace(0, 0));
        assertEquals("_1", IonUtils.getBusSpace(0, 1));
        assertEquals("_41", IonUtils.getBusSpace(0, 2));
        assertEquals("_31", IonUtils.getBusSpace(1, -7));
        assertEquals("_32", IonUtils.getBusSpace(1, -6));
        assertEquals("_33", IonUtils.getBusSpace(1, -5));
        assertEquals("_34", IonUtils.getBusSpace(1, -4));
        assertEquals("_35", IonUtils.getBusSpace(1, -3));
        assertEquals("_36", IonUtils.getBusSpace(1, -2));
        assertEquals("_37", IonUtils.getBusSpace(1, -1));
        assertEquals("_38", IonUtils.getBusSpace(1, 0));
        assertEquals("_39", IonUtils.getBusSpace(1, 1));
        assertEquals("_40", IonUtils.getBusSpace(1, 2));
        assertEquals("_22", IonUtils.getBusSpace(2, -9));
        assertEquals("_21", IonUtils.getBusSpace(2, -8));
        assertEquals("_20", IonUtils.getBusSpace(2, -7));
        assertEquals("_19", IonUtils.getBusSpace(2, -6));
        assertEquals("_18", IonUtils.getBusSpace(2, -5));
        assertEquals("_17", IonUtils.getBusSpace(2, -4));
        assertEquals("_16", IonUtils.getBusSpace(2, -3));
        assertEquals("_15", IonUtils.getBusSpace(2, -2));
        assertEquals("_14", IonUtils.getBusSpace(2, -1));
        assertEquals("_13", IonUtils.getBusSpace(2, 0));
        assertEquals("_12", IonUtils.getBusSpace(2, 1));
        assertEquals("_11", IonUtils.getBusSpace(2, 2));
        assertEquals("_10", IonUtils.getBusSpace(2, 3));
        assertEquals("_45", IonUtils.getBusSpace(3, -8));
        assertEquals("_44", IonUtils.getBusSpace(3, -7));
        assertEquals("_43", IonUtils.getBusSpace(3, -6));
        assertEquals("_42", IonUtils.getBusSpace(3, -5));
        assertEquals("_30", IonUtils.getBusSpace(3, -4));
        assertEquals("_29", IonUtils.getBusSpace(3, -3));
        assertEquals("_28", IonUtils.getBusSpace(3, -2));
        assertEquals("_27", IonUtils.getBusSpace(3, -1));
        assertEquals("_26", IonUtils.getBusSpace(3, 0));
        assertEquals("_25", IonUtils.getBusSpace(3, 1));
        assertEquals("_24", IonUtils.getBusSpace(3, 2));
        assertEquals("_23", IonUtils.getBusSpace(3, 3));
    }

    @Test
    public void testBusRowEnds() {
        assertArrayEquals(new int[]{-7, 2}, IonUtils.getBusRowEnds(0));
        assertArrayEquals(new int[]{-7, 2}, IonUtils.getBusRowEnds(1));
        assertArrayEquals(new int[]{-9, 3}, IonUtils.getBusRowEnds(2));
        assertArrayEquals(new int[]{-8, 3}, IonUtils.getBusRowEnds(3));
    }

}
