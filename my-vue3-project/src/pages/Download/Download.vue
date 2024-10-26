<template>
  <div class="download-container">
    <h1>下载应用</h1>
    
    <!-- 文本提示 -->
    <p>{{ downloadMessage }}</p>

    <!-- 下载链接 -->
    <a :href="downloadLink" download>{{ downloadLink }}</a>
    
    <!-- 下载按钮 -->
    <button @click="triggerDownload">下载应用</button>

    <!-- 生成二维码 -->
    <qrcode-vue
      :value="qrcodeLink"
      :size="size"
      :level="level"
      :render-as="renderAs"
      :background="background"
      :foreground="foreground"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed } from 'vue';
import QrcodeVue from 'qrcode.vue';

export default defineComponent({
  components: {
    QrcodeVue,
  },
  setup() {
    // 动态获取根地址
    const baseUrl = window.location.origin;
    const apkPath = '/static/apk/app-debug.apk'; // APK 文件路径
    const downloadLink = ref(`${baseUrl}${apkPath}`); // 完整的下载链接
    
    // 二维码链接
    const qrcodeLink = computed(() => {
      return `${window.location.href}?download_from=scan_qrcode`;
    });

    // 设置二维码的样式参数
    const size = ref(300);
    const level = ref('H');
    const renderAs = ref('svg');
    const background = ref('#ffffff');
    const foreground = ref('#000000');

    // 下载提示信息
    const downloadMessage = '扫描二维码以开始下载应用。';

    // 触发下载
    const triggerDownload = () => {
      const anchor = document.createElement('a');
      anchor.href = downloadLink.value;
      anchor.download = ''; // 如果你希望使用默认文件名，可以留空
      anchor.click();
    };

    return {
      downloadLink,
      qrcodeLink,
      size,
      level,
      renderAs,
      background,
      foreground,
      downloadMessage,
      triggerDownload,
    };
  },
});
</script>

<style scoped>
.download-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 20px;
}
.download-container h1 {
  margin-bottom: 20px;
}
.download-container p {
  margin-bottom: 10px;
}
.download-container button {
  margin: 10px 0;
}
</style>
