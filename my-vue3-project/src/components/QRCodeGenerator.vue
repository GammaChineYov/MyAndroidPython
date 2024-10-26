<template>
  <div class="qrcode-container">
    <qrcode-vue
      :value="downloadLink"
      :size="size"
      :level="level"
      :render-as="renderAs"
      :background="background"
      :foreground="foreground"
      :image-settings="imageSettings"
    />
    <p>扫描二维码下载应用</p>
  </div>
</template>

<script lang="ts" setup>
import { ref, defineProps } from 'vue';
import QrcodeVue from 'qrcode.vue';
import type { Level, RenderAs, ImageSettings } from 'qrcode.vue';

// 定义组件属性
const props = defineProps<{
  downloadLink: string; // 外部传入的下载链接
  size?: number; // QR 码的大小
  level?: Level; // 纠错级别
  renderAs?: RenderAs; // 渲染方式
  background?: string; // 背景色
  foreground?: string; // 前景色
  imageSettings?: ImageSettings; // 图像设置
}>();

// 设置默认值
const size = ref(props.size || 300);
const level = ref(props.level || 'H');
const renderAs = ref(props.renderAs || 'svg');
const background = ref(props.background || '#ffffff');
const foreground = ref(props.foreground || '#000000');
const imageSettings = ref(props.imageSettings || {});
</script>

<style scoped>
.qrcode-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 20px;
}
</style>
