<template>
  <view class="content">
    <image class="logo" src="/static/logo.png" />
    <view class="text-area">
      <text class="title">{{ title }}</text>
    </view>
    <button @click="goToDownload">前往下载 APK</button> <!-- 添加按钮 -->
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';

// 定义标题
const title = ref('Hello');

// 导航到下载页面的方法
const goToDownload = () => {
  uni.navigateTo({
    url: '/pages/Download/Download', // 路由到下载页面
  });
};
</script>

<style>
.content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.logo {
  height: 200rpx;
  width: 200rpx;
  margin-top: 200rpx;
  margin-left: auto;
  margin-right: auto;
  margin-bottom: 50rpx;
}

.text-area {
  display: flex;
  justify-content: center;
}

.title {
  font-size: 36rpx;
  color: #8f8f94;
}
</style>
