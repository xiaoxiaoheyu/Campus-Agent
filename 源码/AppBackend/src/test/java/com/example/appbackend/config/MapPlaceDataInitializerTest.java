package com.example.appbackend.config;

import com.example.appbackend.entity.MapPlace;
import com.example.appbackend.repository.MapPlaceRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MapPlaceDataInitializerTest {

    @Test
    void replacesOnlyLegacyCampusSeedsAndKeepsExistingChengduPlaces() throws Exception {
        MapPlaceRepository repository = mock(MapPlaceRepository.class);
        MapPlace legacy = place("学一食堂", "朝阳校区", "114.8997410", "40.7555380");
        MapPlace existingChengdu = place("芙蓉食堂", "管理员维护的成都校内地点", "104.1403674", "30.6745194");
        MapPlace unrelated = place("校外服务点", "管理员自建地点", "104.2000000", "30.7000000");
        when(repository.findAll()).thenReturn(List.of(legacy, existingChengdu, unrelated));

        new MapPlaceDataInitializer(repository).run(new DefaultApplicationArguments(new String[0]));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MapPlace>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<MapPlace> saved = captor.getValue();

        assertEquals("DISABLED", legacy.getStatus());
        assertFalse(legacy.getMapVisible());
        assertEquals("ENABLED", unrelated.getStatus());
        assertTrue(unrelated.getMapVisible());
        assertEquals(0, saved.stream().filter(item -> "芙蓉食堂".equals(item.getName())).count());
        assertTrue(saved.stream().anyMatch(item -> "新区图书馆".equals(item.getName())));
        assertTrue(saved.stream()
                .filter(item -> item != legacy)
                .allMatch(item -> item.getLocationDesc().startsWith("成都理工大学成都校区")));
    }

    private MapPlace place(String name, String location, String longitude, String latitude) {
        MapPlace place = new MapPlace();
        place.setName(name);
        place.setLocationDesc(location);
        place.setLongitude(new BigDecimal(longitude));
        place.setLatitude(new BigDecimal(latitude));
        place.setStatus("ENABLED");
        place.setMapVisible(true);
        return place;
    }
}
