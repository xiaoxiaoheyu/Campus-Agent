package com.example.appbackend.config;

import com.example.appbackend.entity.MapPlace;
import com.example.appbackend.repository.MapPlaceRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class MapPlaceDataInitializer implements ApplicationRunner {

    private static final String CAMPUS_LOCATION = "成都理工大学成都校区";
    private static final BigDecimal CHENGDU_MIN_LONGITUDE = new BigDecimal("104.12");
    private static final BigDecimal CHENGDU_MAX_LONGITUDE = new BigDecimal("104.17");
    private static final BigDecimal CHENGDU_MIN_LATITUDE = new BigDecimal("30.65");
    private static final BigDecimal CHENGDU_MAX_LATITUDE = new BigDecimal("30.70");

    private final MapPlaceRepository mapPlaceRepository;

    public MapPlaceDataInitializer(MapPlaceRepository mapPlaceRepository) {
        this.mapPlaceRepository = mapPlaceRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<MapPlace> existingPlaces = mapPlaceRepository.findAll();
        List<MapPlace> changedPlaces = new ArrayList<>();

        // 只停用本初始化器创建过的河北建筑工程学院朝阳校区旧点位，避免影响管理员自建数据。
        existingPlaces.stream()
                .filter(this::isLegacyChaoyangSeed)
                .filter(place -> !"DISABLED".equals(place.getStatus()) || Boolean.TRUE.equals(place.getMapVisible()))
                .forEach(place -> {
                    place.setStatus("DISABLED");
                    place.setMapVisible(false);
                    changedPlaces.add(place);
                });

        List<MapPlace> seeds = List.of(
                // 食堂
                place("CANTEEN", "CANTEEN", "芙蓉食堂", "成都理工大学校内餐饮服务点。", "芙蓉园片区", "104.1403674", "30.6745194", 1),
                place("CANTEEN", "CANTEEN", "银杏餐厅", "成都理工大学校内餐饮服务点。", "银杏园片区", "104.1435442", "30.6701312", 2),
                place("CANTEEN", "CANTEEN", "珙桐园食堂", "成都理工大学校内餐饮服务点。", "珙桐园片区", "104.1494160", "30.6727072", 3),
                place("CANTEEN", "CANTEEN", "香樟食堂", "成都理工大学校内餐饮服务点。", "香樟园片区", "104.1548536", "30.6768845", 4),
                // 体育场馆
                place("SPORTS", "SPORTS_GROUND", "体育馆", "成都理工大学体育场馆。", "校园西区", "104.1400596", "30.6727341", 10),
                place("SPORTS", "SPORTS_GROUND", "香樟操场", "成都理工大学室外运动场地。", "香樟园片区", "104.1534713", "30.6758057", 11),
                place("SPORTS", "SPORTS_GROUND", "银杏篮球场", "成都理工大学室外篮球场。", "银杏园片区", "104.1427432", "30.6694773", 12),
                // 教学与图书馆
                place("TEACHING", "TEACHING_BUILDING", "第一教学楼", "成都理工大学教学楼。", "校园西区", "104.1426518", "30.6755090", 20),
                place("TEACHING", "TEACHING_BUILDING", "第二教学楼", "成都理工大学教学楼。", "校园西区", "104.1444130", "30.6760736", 21),
                place("TEACHING", "TEACHING_BUILDING", "第六教学楼", "成都理工大学教学楼。", "校园西区", "104.1450571", "30.6750289", 22),
                place("TEACHING", "TEACHING_BUILDING", "第八教学楼", "成都理工大学教学楼。", "校园中部", "104.1459870", "30.6737942", 23),
                place("TEACHING", "TEACHING_BUILDING", "第九教学楼", "成都理工大学教学楼。", "校园西南区", "104.1417409", "30.6718614", 24),
                place("TEACHING", "TEACHING_BUILDING", "新区图书馆", "成都理工大学图书馆。", "校园东区", "104.1514968", "30.6761361", 25),
                // 校园服务与景观
                place("OTHER", "HOSPITAL", "校医院", "成都理工大学校园医疗服务点。", "校园西北区", "104.1427438", "30.6769351", 30),
                place("OTHER", "LANDSCAPE", "砚湖", "成都理工大学校园景观。", "校园西区", "104.1428618", "30.6741345", 31),
                place("OTHER", "ADMIN_BUILDING", "保卫处", "成都理工大学校园安全服务点。", "校园西北区", "104.1429773", "30.6763609", 32)
        );

        seeds.stream()
                .filter(seed -> existingPlaces.stream().noneMatch(existing -> isSameCampusPlace(existing, seed)))
                .forEach(changedPlaces::add);

        if (!changedPlaces.isEmpty()) {
            mapPlaceRepository.saveAll(changedPlaces);
        }
    }

    private boolean isLegacyChaoyangSeed(MapPlace place) {
        String location = place.getLocationDesc();
        return place.getParentId() == null
                && location != null
                && location.contains("朝阳校区")
                && place.getLongitude() != null
                && place.getLatitude() != null
                && place.getLongitude().compareTo(new BigDecimal("114.89")) >= 0
                && place.getLongitude().compareTo(new BigDecimal("114.91")) <= 0
                && place.getLatitude().compareTo(new BigDecimal("40.74")) >= 0
                && place.getLatitude().compareTo(new BigDecimal("40.77")) <= 0;
    }

    private boolean isSameCampusPlace(MapPlace existing, MapPlace seed) {
        if (!seed.getName().equals(existing.getName())) return false;
        String location = existing.getLocationDesc();
        if (location != null && location.startsWith(CAMPUS_LOCATION)) return true;
        return existing.getLongitude() != null
                && existing.getLatitude() != null
                && existing.getLongitude().compareTo(CHENGDU_MIN_LONGITUDE) >= 0
                && existing.getLongitude().compareTo(CHENGDU_MAX_LONGITUDE) <= 0
                && existing.getLatitude().compareTo(CHENGDU_MIN_LATITUDE) >= 0
                && existing.getLatitude().compareTo(CHENGDU_MAX_LATITUDE) <= 0;
    }

    private MapPlace place(
            String sceneType,
            String placeType,
            String name,
            String description,
            String locationDesc,
            String longitude,
            String latitude,
            int sortOrder
    ) {
        MapPlace place = new MapPlace();
        place.setSceneType(sceneType);
        place.setPlaceType(placeType);
        place.setName(name);
        place.setDescription(description);
        place.setLocationDesc(CAMPUS_LOCATION + " · " + locationDesc);
        place.setLongitude(new BigDecimal(longitude));
        place.setLatitude(new BigDecimal(latitude));
        place.setStatus("ENABLED");
        place.setMapVisible(true);
        place.setSortOrder(sortOrder);
        return place;
    }
}
