<template>
  <view class="page flex-col">
    <view class="group_1 flex-row justify-between">
      <view class="image-text_1 flex-col" @click="onTab('index')">
        <image
          class="label_1"
          referrerpolicy="no-referrer"
          src="/static/tabbar/watercolor/home.png"
        />
        <text class="text-group_1" :class="{ active: current === 'index' }">首页</text>
      </view>
      <view class="group_2 flex-row">
        <view class="image-text_2 flex-col" @click="onTab('map')">
          <image
            class="image_1"
            referrerpolicy="no-referrer"
            src="/static/tabbar/watercolor/map.png"
          />
          <text class="text-group_2" :class="{ active: current === 'map' }">校园地图</text>
        </view>
      </view>
      <view class="image-text_3 flex-col" @click="onTab('mine')">
        <image
          class="label_2"
          referrerpolicy="no-referrer"
          src="/static/tabbar/watercolor/mine.png"
        />
        <text class="text-group_3" :class="{ active: current === 'mine' }">我的</text>
      </view>
    </view>
  </view>
</template>
<script>
export default {
  name: 'AppMainTabBar',
  props: {
    current: {
      type: String,
      default: 'index'
    }
  },
  data() {
    return {};
  },
  methods: {
    onTab(type) {
      const routes = {
        'index': '/pages/index/index',
        'map': '/pages/map/map',
        'mine': '/pages/mine/mine'
      };
      const url = routes[type];
      if (!url) return;
      const pages = getCurrentPages();
      const cur = pages[pages.length - 1];
      const curRoute = cur ? ('/' + cur.route) : '';
      if (curRoute === url) return;
      uni.redirectTo({
        url,
        animationType: 'none',
        animationDuration: 0,
        fail: () => {
          uni.reLaunch({ url });
        }
      });
    }
  }
};
</script>
<style lang='css'>
.page view {
  box-sizing: border-box;
  flex-shrink: 0;
}

.page {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  width: 750rpx;
	height: calc(246rpx + env(safe-area-inset-bottom));
  background: transparent;
  z-index: 999;
  padding-bottom: env(safe-area-inset-bottom);
}

.page::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
	top: 104rpx;
  bottom: 0;
	background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), #fbfefc);
	box-shadow: 0 -16rpx 38rpx rgba(26, 85, 67, 0.14);
  z-index: 0;
}

.group_1 {
  position: relative;
  z-index: 1;
  width: 750rpx;
	background: transparent;
	padding: 6rpx 86rpx 20rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.image-text_1 {
	margin-top: 68rpx;
}

.label_1 {
	width: 88rpx;
	height: 88rpx;
  align-self: center;
}

.text-group_1 { 
  overflow-wrap: break-word;
  color: rgba(102, 102, 102, 1);
  font-size: 24rpx;
  text-align: center;
  white-space: nowrap;
  line-height: 32rpx;
	margin-top: -8rpx;
}

.text-group_1.active {
	color: #0783f5;
	font-weight: 800;
}

.group_2 {
	padding: 0;
}

.image-text_2 {
	align-items: center;
	margin-top: -18rpx;
}

.image_1 {
	width: 168rpx;
	height: 168rpx;
	filter: drop-shadow(0 12rpx 20rpx rgba(197, 157, 33, 0.22));
}

.text-group_2 {
  overflow-wrap: break-word;
	color: #16856b;
  font-size: 24rpx;
  font-family: PingFangSC-Medium;
  font-weight: 500;
  text-align: center;
  white-space: nowrap;
	line-height: 30rpx;
  align-self: center;
	margin-top: -25rpx;
}

.text-group_2.active {
	color: #0783f5;
}

.image-text_3 {
	margin-top: 68rpx;
}

.label_2 {
	width: 88rpx;
	height: 88rpx;
	margin-left: 0;
}

.text-group_3 {
  overflow-wrap: break-word;
  color: rgba(102, 102, 102, 1);
  font-size: 24rpx;
  font-family: PingFangSC-Medium;
  font-weight: 500;
  text-align: center;
  white-space: nowrap;
  line-height: 32rpx;
	margin-top: -8rpx;
}

.text-group_3.active {
	color: #0783f5;
	font-weight: 800;
}

.image_2 {
  width: 750rpx;
  height: 68rpx;
}

.flex-col {
  display: flex;
  flex-direction: column;
}

.flex-row {
  display: flex;
  flex-direction: row;
}

.justify-between {
  display: flex;
  justify-content: space-between;
}
</style>
